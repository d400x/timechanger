package ga.d400x.timechanger.sample;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import ga.d400x.timechanger.util.TimeChangerUtil;

public class Sample {

	public static void main(String... args) throws Exception {

		System.out.println("classLoader: " + Sample.class.getClassLoader());

		if(TimeChangerUtil.isChangingTime()) {
			// offset by System property (-DTimeChangerAgent.OFFSETTIMEMILLIS)
			printTimes();
			// clear offset
			TimeChangerUtil.clearOffsetMillis();
		}

		// no offset
		printTimes();

		// +12 hours
		TimeChangerUtil.setOffsetMillis(43200_000L);
		printTimes();

		// -1 day
		TimeChangerUtil.setOffsetMillis(-86_400_000L);
		printTimes();

		// calc and set to specified date
		TimeChangerUtil.setTo(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-10-17 12:34:56"));
		printTimes();

		// time progress after offset set
		TimeUnit.SECONDS.sleep(5L);
		printTimes();
	}

	private static void printTimes() {
		System.out.println("==== offset:" + TimeChangerUtil.getOffsetMillis());

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
		long now = System.currentTimeMillis();

		System.out.println("System.currentTimeMillis(): " + now + " (" + df.format(now) + ")");
		System.out.println("new Date()                : " + df.format(new Date()));
		System.out.println("Calendar.getInstance()    : " + df.format(Calendar.getInstance().getTime()));


		ZonedDateTime zdt = ZonedDateTime.now(ZoneId.systemDefault());
		System.out.println("ZonedDateTime.now         : " + zdt + " (toInstant(): " + zdt.toInstant() + ")");

		LocalDateTime l = LocalDateTime.now();
		System.out.println("LocalDateTime.now():      : " + l + " (" + l.format(DateTimeFormatter.ISO_DATE_TIME) + ")");
	}

}
