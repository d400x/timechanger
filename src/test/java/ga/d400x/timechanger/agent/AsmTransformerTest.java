package ga.d400x.timechanger.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ga.d400x.timechanger.util.TimeChangerUtil;

public class AsmTransformerTest {

	@DisplayName("no transform by pkgname")
	@ParameterizedTest
	@ValueSource(strings = { "ga/d400x/timechanger/agent/TimeChangerAgent", "ga/d400x/timechanger/util/TimeChangerUtil" })
	public void skipTransform(String className) throws Exception {
		AsmTransformer transformer = new AsmTransformer();
		assertNull(transformer.transform(null,className, null, null, null));
	}

	@DisplayName("no transform class has no System.currentTimeMillis() call")
	@Test
	public void skipTransform() throws Exception {
		AsmTransformer transformer = new AsmTransformer();
		String className = "ga.d400x.timechanger.test.Sample2";
		byte[] classfileBuffer = getClassByteArray(className);

		assertNull(transformer.transform(null, className, null, null, classfileBuffer));
	}

	@DisplayName("transformed")
	@Test
	public void transform() throws Exception {
		AsmTransformer transformer = new AsmTransformer();
		String className = "ga.d400x.timechanger.test.Sample1";
		byte[] classfileBuffer = getClassByteArray(className);
		byte[] transformed = transformer.transform(null, className, null, null, classfileBuffer);

		assertNotNull(transformed);
		assertFalse(Arrays.equals(classfileBuffer, transformed));

		Class<?> clazz = loadClass(className, transformed);
		System.out.println(clazz);
		Method m = clazz.getMethod("getTimeMillis");
		Object instance = clazz.getDeclaredConstructor().newInstance();

		Supplier<Long> offsetMillisSupplier = ()-> {
			try {
				return (Long)m.invoke(instance);
			} catch(Exception e) {
				throw new RuntimeException(e);
			}
		};

		System.setProperty(TimeChangerAgent.PROP_PREMAIN, "1");
		long acceptableDeltaMillis = 8L;

		// no timeoffset
		offsetCheck(0L, offsetMillisSupplier, acceptableDeltaMillis, 0L);

		// 1 day
		offsetCheck(86_400_000L, offsetMillisSupplier, acceptableDeltaMillis, 1L);

		// -1 week
		offsetCheck(-604_800_000L, offsetMillisSupplier, acceptableDeltaMillis, 3L);
	}

	private void offsetCheck(long offset, Supplier<Long> offsetMillisSupplier, long acceptableDeltaMillis, long waitSec) {
		TimeChangerUtil.setOffsetMillis(offset);
		long actualTimeMillis = System.currentTimeMillis();
		if(0 < waitSec) {
			try {
				TimeUnit.SECONDS.sleep(waitSec);
			} catch(Exception e) {}
		}
		long offsetedTimeMillis = offsetMillisSupplier.get();
		long offsetedTimeMillisWithoutWait = offsetedTimeMillis - (waitSec * 1000L);
		long actualOffset = offsetedTimeMillisWithoutWait - actualTimeMillis;
		System.out.println(String.format("actual=%d (%s), offseted=%d (%s), offset=%d, actualOffset=%d, diff=%d, acceptable=%d"
				, actualTimeMillis, new Timestamp(actualTimeMillis)
				, offsetedTimeMillis, new Timestamp(offsetedTimeMillis)
				, offset
				, actualOffset
				, (actualOffset - offset)
				, acceptableDeltaMillis
				));
		assertTrue(Math.abs(actualOffset - offset) <= acceptableDeltaMillis);
	}

	/**
	 * get raw bytecode from resource
	 * @param className
	 * @return .class bytecodes
	 */
	byte[] getClassByteArray(String className) {
		if(!className.endsWith(".class")) {
			className += ".class";
		}
		if(className.charAt(0) != '/' && className.charAt(0) != '.') {
			className = "/" + className;
		}
		className = className.replace('.', '/').replaceAll("/class$", ".class");
		//System.err.println(className);
		try(InputStream is = getClass().getResourceAsStream(className);
				ByteArrayOutputStream os = new ByteArrayOutputStream())
		{
			byte[] buf = new byte[4096];
			int len = 0;
			while(0 < (len = is.read(buf))) {
				os.write(buf, 0, len);
			}

			return os.toByteArray();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * create {@code Class<?>} from Java byte code
	 * @param className
	 * @param classfileBuffer
	 * @return {@code Class<?>} from classfileBuffer
	 */
	Class<?> loadClass(String className, byte[] classfileBuffer) {
		return new CustomClassLoader(getClass().getClassLoader()).defineClass(className, classfileBuffer);
	}

	/**
	 * custom {@link ClassLoader} which enables to create class by raw bytecode
	 */
	static class CustomClassLoader extends ClassLoader {
		public CustomClassLoader(ClassLoader parent) {
			super(parent);
		}
		public Class<?> defineClass(String name, byte[] buf) {
			return defineClass(name, buf, 0, buf.length);
		}
	}

}
