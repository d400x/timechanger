package ga.d400x.timechanger.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;


/**
 * Agent which intercepts System.currentTimeMillis() method calls and apply offset to their return value
 * @see AsmTransformer
 */
public class TimeChangerAgent {

	static boolean isDoneBootstrapSearchAdd = false;

	// commented out: logger.log initiates java.util.Date before transform
	//static final Logger logger = Logger.getLogger(TimeMillisOffsetAgent.class.getCanonicalName());

	/** System property key for offset millis */
	public static final String PROP_OFFSET = TimeChangerAgent.class.getSimpleName() + ".OFFSETMILLIS";

	/** System property key for timechanger javaagent applied or not */
	public static final String PROP_PREMAIN = TimeChangerAgent.class.getSimpleName() + ".PREMAIN";

	/** System property key for debug mode */
	private static final String PROP_DEBUGLOG = "TIMECHANGER_DEBUG";

	/** option string for exclude-option */
	private static final String EXCLUDE_PREFIX = "exclude=";

	/**
	 * @param agentArgs "exclude=classpath,pkgpath/,path_starts_with*,..."
	 * @param inst {@link Instrumentation}
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		try {
			// Args
			boolean isArgs = false;
			if(agentArgs != null && agentArgs.startsWith(EXCLUDE_PREFIX) && EXCLUDE_PREFIX.length() < agentArgs.length()) {
				for(String path: agentArgs.substring(EXCLUDE_PREFIX.length()).split(",")) {
					path = path.replace('.', '/');
					if(path.endsWith("/")) {
						addSkipClassStartWith(path);
						isArgs = true;
					}
					else if(path.endsWith("*")) {
						int idx = path.length();
						while(0 < idx && path.charAt(idx-1) == '*') {	// strip trailing '*'s without loading Regex
							idx--;
						}
						if(0 <= idx) {
							addSkipClassStartWith(path.substring(0, idx));
							isArgs = true;
						}
					} else {
						addSkipClass(path);
						isArgs = true;
					}
				}
				if(isArgs) {
					log("skip=" + SKIP_CLASS + ", skipClassStartsWith=" + SKIP_CLASS_STARTSWITH);
				}
			}

			inst.addTransformer(new AsmTransformer());
			TimeChangerAgent.log("added AsmTransformer");

			// mark premain processed
			System.setProperty(PROP_PREMAIN, "1");
			log("property set: " + PROP_PREMAIN + ", classLoader=" + TimeChangerAgent.class.getClassLoader());
		} catch(Exception e) {
			e.printStackTrace(System.err);
			throw e;
		}
	}

	// utility methods

	private static boolean isDebugLog() {
		return System.getProperty(PROP_DEBUGLOG) != null;
	}

	/**
	 * log to STDERR if {@link #PROP_DEBUGLOG} set
	 * @param msg logging message
	 */
	public static void log(String msg) {
		if(isDebugLog()) {
			System.err.println(msg);
		}
	}

//	/**
//	 * check class existence
//	 * @param className
//	 * @return class existence
//	 */
//	private static boolean isExistClass(String className) {
//		try {
//			Class.forName(className);
//			return true;
//		} catch(ClassNotFoundException e) {
//			System.err.println(e.toString());
//		}
//		return false;
//	}

	private static final Set<String> SKIP_CLASS_STARTSWITH = new HashSet<>();
	static {
		String path = TimeChangerAgent.class.getPackage().getName().replace('.', '/') + '/';
		SKIP_CLASS_STARTSWITH.add(path);
		SKIP_CLASS_STARTSWITH.add(path.replace("/agent/", "/util/"));
	}

	/**
	 * Add exclude class pattern (start with)
	 * @param startWith start-with-path separated by '/' (eg. "java/lang")
	 * @return {@link Set#add(Object)}
	 */
	public static synchronized boolean addSkipClassStartWith(String startWith) {
		return SKIP_CLASS_STARTSWITH.add(startWith);
	}

	private static final Set<String> SKIP_CLASS = new HashSet<>();

	/**
	 * Add exclude class
	 * @param className class path separated by '/' (eg. "java/util/Date")
	 * @return {@link Set#add(Object)}
	 */
	public static synchronized boolean addSkipClass(String className) {
		return SKIP_CLASS.add(className);
	}

	/**
	 * check className is transform skip target or not
	 * @param className separated by '/' (eg. "java/util/Date")
	 * @return true if className would not transform
	 */
	public static boolean isSkip(String className) {
		return className != null
				&& (SKIP_CLASS.contains(className) || SKIP_CLASS_STARTSWITH.stream().filter(className::startsWith).findFirst().isPresent());
	}

	/**
	 * dumps skip target classes (to STDOUT)
	 */
	public static void dumpSkip() {
		System.out.println("skipClass=" + SKIP_CLASS);
		System.out.println("skipClassStartsWith=" + SKIP_CLASS_STARTSWITH);
	}
}
