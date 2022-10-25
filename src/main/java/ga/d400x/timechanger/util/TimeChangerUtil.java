package ga.d400x.timechanger.util;

import java.util.Date;

import ga.d400x.timechanger.agent.TimeChangerAgent;

/**
 * Utility methods
 * @author d400x
 */
public class TimeChangerUtil {

	private TimeChangerUtil(){}

	/**
	 * @return true if TimeChangerAgent applied and transformer set
	 */
	private static boolean isPremain() {
		return System.getProperty(TimeChangerAgent.PROP_PREMAIN) != null;
	}

	/**
	 * @throws UnsupportedOperationException when {@link #isPremain()} false
	 */
	private static void checkPremain() {
		if(!isPremain()) {
			throw new UnsupportedOperationException("premain not executed");
		}
	}

	/**
	 * sets offset millis
	 * @param offsetMillis will be added to every {@link System#currentTimeMillis()} call
	 */
	public static void setOffsetMillis(long offsetMillis) {
		checkPremain();
		if(offsetMillis == 0L) {
			clearOffsetMillis();
		} else {
			System.setProperty(TimeChangerAgent.PROP_OFFSET, String.valueOf(offsetMillis));
		}
	}

	/**
	 * get current offset millis
	 * @return offsetMillis will be added to every {@link System#currentTimeMillis()} call
	 */
	public static long getOffsetMillis() {
		if(!isPremain()) return 0L;

		String val = System.getProperty(TimeChangerAgent.PROP_OFFSET);
		return val == null ? 0L : Long.parseLong(val);
	}

	/**
	 * clears offset millis
	 */
	public static void clearOffsetMillis() {
		System.clearProperty(TimeChangerAgent.PROP_OFFSET);
	}

	/**
	 * calculate and set offset so that changed time will be <i>target</i>
	 * @param target target date that offset will be (offset = target - current)
	 */
	public static void setTo(Date target) {
		checkPremain();
		setOffsetMillis(target.getTime() - System.currentTimeMillis());
	}

	/**
	 * returns actual System.currentTimeMillis call result
	 * @return {@link System#currentTimeMillis()}
	 */
	public static Long getActualTimeMillis() {
		return System.currentTimeMillis();
	}

	/**
	 * checks changing time or not
	 * @return true if changing time
	 */
	public static boolean isChangingTime() {
		return isPremain() && getOffsetMillis() != 0L;
	}

	/**
	 * Offseted {@link System#currentTimeMillis()}<br>
	 * <del>Transformer replaces {@link System#currentTimeMillis()} call to this method</del>
	 * @return Offseted timeMillis
	 * @see #getActualTimeMillis()
	 * @see ga.d400x.timechanger.agent.TimeChangerAgent#premain
	 * @see ga.d400x.timechanger.agent.AsmTransformer.TimeOffsetMethodAdapter#visitMethodInsn
	 */
	public static long offsetCurrentTimeMillis() {
		return System.currentTimeMillis() + getOffsetMillis();
	}

//	public static long offsetNaoTime() {
//		return System.nanoTime() + (getOffsetMillis() * 1_000_000L);
//	}

	/**
	 * Offseted {@code jdk.internal.misc.VM#getNanoTimeAdjustment(long)} (JDK11+)<br>
	 * <del>Transformer replaces {@code jdk.internal.misc.VM#getNanoTimeAdjustment(long)} call to this method</del>
	 * @param offsetInSeconds base value (unixtime seconds)
	 * @return offseted {@code jdk.internal.misc.VM#getNanoTimeAdjustment(long)} (or -1L if calculated value out of range) <br>
	 * see more detailed info for a javadoc for jdk.internal.misc.VM
	 * @see ga.d400x.timechanger.agent.TimeChangerAgent#premain
	 * @see ga.d400x.timechanger.agent.AsmTransformer.TimeOffsetMethodAdapter#visitMethodInsn
	 */
	public static long offsetGetNanoTimeAdjustment(long offsetInSeconds) {
		long adjSec = (offsetCurrentTimeMillis() / 1000L) - offsetInSeconds;
		if(adjSec != (long)((int)adjSec)) {
			return -1L;
		}
		return adjSec * 1_000_000_000L;
	}

}
