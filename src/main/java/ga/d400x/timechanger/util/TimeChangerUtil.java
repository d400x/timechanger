package ga.d400x.timechanger.util;

import java.util.Date;

import ga.d400x.timechanger.agent.TimeChangerAgent;

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
	 * @param target
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
	 * returns Offseted timeMillis<br>
	 * Transformer replaces {@link System#currentTimeMillis()} call to this method
	 * @return Offseted timeMillis
	 * @see #getActualTimeMillis()
	 * @see ga.d400x.timechanger.agent.TimeChangerAgent#premain
	 * @see ga.d400x.timechanger.agent.AsmTransformer.CustomMethodVisitor#visitMethodInsn
	 */
	public static long offsetCurrentTimeMillis() {
		return System.currentTimeMillis() + getOffsetMillis();
	}

//	public static long offsetNaoTime() {
//		return System.nanoTime() + (getOffsetMillis() * 1_000_000L);
//	}

	/**
	 * Transformer replaces {@link jdk.internal.misc.VM#getNanoTimeAdjustment(long)} call to this method
	 * @param offsetInSeconds
	 * @return offseted {@link jdk.internal.misc.VM#getNanoTimeAdjustment(long)}
	 * @see ga.d400x.timechanger.agent.TimeChangerAgent#premain
	 * @see ga.d400x.timechanger.agent.AsmTransformer.CustomMethodVisitor#visitMethodInsn
	 */
	public static long offsetGetNanoTimeAdjustment(long offsetInSeconds) {
		long adj = (offsetCurrentTimeMillis() - (offsetInSeconds * 1000L));
		if((adj >>> 32) != 0L) {
			return -1L;
		}
		return adj * 1_000_000L;
	}

}
