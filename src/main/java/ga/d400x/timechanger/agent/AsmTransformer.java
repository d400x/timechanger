package ga.d400x.timechanger.agent;

import static org.objectweb.asm.Opcodes.*;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Transfomer that intercepts {@link java.lang.System#currentTimeMillis()} and applies offset.
 * @author d400x
 */
public class AsmTransformer implements ClassFileTransformer {

	static final int ASM_VER = ASM9;

	/**
	 * transforms date/time method calls by using ASM
	 * @param className will be checked by {@link TimeChangerAgent#isSkip}
	 * @param classfileBuffer class loaded and modified by ASM
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {

		//System.out.println(">>> " + className);

		// NOTE: className separated by '/' NOT '.' (eg. java/lang/System )

		try {
			if(TimeChangerAgent.isSkip(className)) {
				// no need to transform
				//TimeMillisOffsetAgent.log("className skip! " + className);
				return null;
			}

			ClassReader classReader = new ClassReader(classfileBuffer);
			//ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
			ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);	// COMPUTE_FRAMES caused java.lang.ClassCircularityError

			TimeOffsetClassAdapter timeOffsetClassAdapter = new TimeOffsetClassAdapter(ASM_VER, classWriter, className);
			ClassVisitor cv = timeOffsetClassAdapter;

			//// for develop and debug
			//cv = new CheckClassAdapter(timeOffsetClassAdapter);

			classReader.accept(cv, ClassReader.EXPAND_FRAMES);

			return timeOffsetClassAdapter.hasTarget ? classWriter.toByteArray() : null;
		} catch(Throwable t) {
			System.err.println(t.toString());
			t.printStackTrace(System.err);
			throw t;
//		} finally {
//			System.err.println("transform end");
		}
	}

	private static final class TimeOffsetClassAdapter extends ClassVisitor {
		/**
		 * @param api {@link ClassVisitor#api}
		 * @param classVisitor {@link ClassVisitor#cv}
		 * @param className (for logging)
		 */
		protected TimeOffsetClassAdapter(int api, ClassVisitor classVisitor, String className) {
			super(api, classVisitor);
			this.hasTarget = false;
			this.className = className;
//			logger.finest("CustomClassVisitor constructor");
		}

		/** whether class modified or not */
		boolean hasTarget;

		/** target class name (for logging) */
		String className;

		/**
		 * apply custom methodVisitor that enables offset to time-related-methods
		 */
		@Override
		public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
			if(cv != null) {
				return new TimeOffsetMethodAdapter(ASM_VER, cv.visitMethod(access, name, descriptor, signature, exceptions), this, name);
			}
			return null;
		}
	}

	/**
	 * {@link MethodVisitor} which replaces:
	 * <ul>
	 * <li>{@link java.lang.System#currentTimeMillis()}</li>
	 * <li>{@link jdk.internal.misc.VM#getNanoTimeAdjustment(long)} (Java9+)</li>
	 * </ul>
	 */
	private static final class TimeOffsetMethodAdapter extends MethodVisitor {

		/**
		 * @param api {@link MethodVisitor#api}
		 * @param methodVisitor {@link MethodVisitor#mv}
		 * @param customClassVisitor
		 * @param methodName target method name (for logging)
		 */
		protected TimeOffsetMethodAdapter(int api, MethodVisitor methodVisitor, TimeOffsetClassAdapter customClassVisitor, String methodName) {
			super(api, methodVisitor);
			this.ccv = customClassVisitor;
			this.methodName = methodName;
		}

		TimeOffsetClassAdapter ccv;

		String methodName;

		/**
		 * create instructions to get offset-applied System.currentTimeMillis()<br>
		 * <code>Math.addExact(System.currentTimeMillis(), Long.valueOf(System.getProperty(TimeChangerAgent.PROP_OFFSET, "0")).longValue());</code>
		 */
		private void createOffsetTimeMillisCode() {
			// System.currentTimeMillis
			super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "currentTimeMillis", "()J", false);
			// System.getProperty(TimeChangerAgent.PROP_OFFSET, "0")
			visitLdcInsn(TimeChangerAgent.PROP_OFFSET);
			visitLdcInsn("0");
			super.visitMethodInsn(INVOKESTATIC, "java/lang/System", "getProperty", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", false);
			// Long::valueOf
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(Ljava/lang/String;)Ljava/lang/Long;", false);
			// Long::longValue
			super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
			// Math.addExact
			super.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "addExact", "(JJ)J", false);
		}

		/**
		 * For debug use: System.out.println(long_val_on_top_of_op-stack);
		 */
		@SuppressWarnings("unused")
		private void printLongValOnOpStackTop() {
			visitInsn(DUP2);
			visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			visitInsn(DUP_X2);
			visitInsn(POP);
			super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(J)V", false);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if(opcode == INVOKESTATIC
					&& !isInterface
					&& "java/lang/System".equals(owner)
					&& "currentTimeMillis".equals(name)
					&& "()J".equals(descriptor))
			{
				// replace System#currentTimeMillis()
				TimeChangerAgent.log("(" + ccv.className + "#" + methodName + ") target: " + owner + " " + name + " " + descriptor + " opcode=" + opcode + ", isInterface=" + isInterface);
				createOffsetTimeMillisCode();
				ccv.hasTarget = true;
			}
			else if(opcode == INVOKESTATIC
					&& !isInterface
					&& "jdk/internal/misc/VM".equals(owner)
					&& "getNanoTimeAdjustment".equals(name)
					&& "(J)J".equals(descriptor))
			{
				// replace VM#getNanoTimeAdjustment(long)
				TimeChangerAgent.log("(" + ccv.className + "#" + methodName + ") target: " + owner + " " + name + " " + descriptor + " opcode=" + opcode + ", isInterface=" + isInterface);

				//-- just call Utility method and return
				//super.visitMethodInsn(opcode, "ga/d400x/timechanger/util/TimeChangerUtil", "offsetGetNanoTimeAdjustment", descriptor, isInterface);
				//return;
				//--

				//-- DEBUG: output long value on top of op stack --//
				//printLongValOnOpStackTop();
				//-- DEBUG end --//

				// long offsetInMillis = arg:offsetInSeconds *= -1000L
				visitLdcInsn(Long.valueOf(-1000L));
				visitInsn(LMUL);

				// long offsetTimeMillis = Math.addExact(System.currentTimeMillis, Long.valueOf(System.getProperty("HOGE","0")).longValue())
				createOffsetTimeMillisCode();

				// long diffMillis = Math.addExact(offsetTimeMillis, offsetInMillis)
				super.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "addExact", "(JJ)J", false);
				visitInsn(DUP2);	// keep diffMillis operand for lcmp

				// long diffSec = diffMillis / 1000L
				visitLdcInsn(Long.valueOf(1000L));
				visitInsn(LDIV);	// long diffSec = diffMillis / 1000

				// long checkSec == (long)((int)diffSec)
				visitInsn(DUP2);	// keep diffMillis operand for l2i,i2l
				visitInsn(L2I);
				visitInsn(I2L);

				visitInsn(LCMP);

				Label label = new Label();
				Label end = new Label();
				visitJumpInsn(IFEQ, label);

				// if(work != 0)
				visitInsn(POP2);
				visitLdcInsn(Long.valueOf(-1L));
				visitJumpInsn(GOTO, end);

				// if(work == 0)
				visitLabel(label);
				visitLdcInsn(Long.valueOf(1000000L));
				visitInsn(LMUL);

				visitLabel(end);

				//-- DEBUG: output long value on top of op stack --//
				//printLongValOnOpStackTop();
				//-- DEBUG end --//

				ccv.hasTarget = true;
			} else {
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		}
	}
}
