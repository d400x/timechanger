package ga.d400x.timechanger.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class AsmTransformer implements ClassFileTransformer {

//	static final Logger logger = Logger.getLogger(AsmTransformer.class.getCanonicalName());

	static final int ASM_VER = Opcodes.ASM9;

	static final String REPLACE_OWNER = "ga/d400x/timechanger/util/TimeChangerUtil";

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
			ClassWriter classWriter = new ClassWriter(classReader, 0);

			CustomClassVisitor customClassVisitor = new CustomClassVisitor(ASM_VER, classWriter, className);

			classReader.accept(customClassVisitor, 0);

			return customClassVisitor.hasTarget ? classWriter.toByteArray() : null;
		} catch(Throwable t) {
			System.err.println(t.toString());
			t.printStackTrace(System.err);
			throw t;
//		} finally {
//			System.err.println("transform end");
		}
	}

	private static final class CustomClassVisitor extends ClassVisitor {
		/**
		 * @param api {@link ClassVisitor#api}
		 * @param classVisitor {@link ClassVisitor#cv}
		 * @param className (for logging)
		 */
		protected CustomClassVisitor(int api, ClassVisitor classVisitor, String className) {
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
				return new CustomMethodVisitor(ASM_VER, cv.visitMethod(access, name, descriptor, signature, exceptions), this, name);
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
	private static final class CustomMethodVisitor extends MethodVisitor {

		/**
		 * @param api {@link MethodVisitor#api}
		 * @param methodVisitor {@link MethodVisitor#mv}
		 * @param customClassVisitor
		 * @param methodName target method name (for logging)
		 */
		protected CustomMethodVisitor(int api, MethodVisitor methodVisitor, CustomClassVisitor customClassVisitor, String methodName) {
			super(api, methodVisitor);
			this.ccv = customClassVisitor;
			this.methodName = methodName;
		}

		CustomClassVisitor ccv;

		String methodName;

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
			if(opcode == Opcodes.INVOKESTATIC && !isInterface)
			{
				if("java/lang/System".equals(owner) && "currentTimeMillis".equals(name) && "()J".equals(descriptor)) {
					// replace System#currentTimeMillis()
					TimeChangerAgent.log("(" + ccv.className + "#" + methodName + ") target: " + owner + " " + name + " " + descriptor + " opcode=" + opcode + ", isInterface=" + isInterface);
					owner = REPLACE_OWNER;
					name = "offsetCurrentTimeMillis";
					ccv.hasTarget = true;
				}
//				else if("java/lang/System".equals(owner) && "nanoTime".equals(name) && "()J".equals(descriptor)) {
//					// replace System#nanoTime()
//					TimeChangerAgent.log("(" + ccv.className + "#" + methodName + ") target: " + owner + " " + name + " " + descriptor + " opcode=" + opcode + ", isInterface=" + isInterface);
//					owner = REPLACE_OWNER;
//					name = "offsetNanoTime";
//					ccv.hasTarget = true;
//				}
				else if("jdk/internal/misc/VM".equals(owner) && "getNanoTimeAdjustment".equals(name) && "(J)J".equals(descriptor)) {
					// replace VM#getNanoTimeAdjustment(long)
					TimeChangerAgent.log("(" + ccv.className + "#" + methodName + ") target: " + owner + " " + name + " " + descriptor + " opcode=" + opcode + ", isInterface=" + isInterface);
					owner = REPLACE_OWNER;
					name = "offsetGetNanoTimeAdjustment";
					ccv.hasTarget = true;
				}
			}
			super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
		}
	}

}
