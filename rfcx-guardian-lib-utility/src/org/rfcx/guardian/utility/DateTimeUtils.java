package org.rfcx.guardian.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DateTimeUtils {
	
	private static final String TAG = "Rfcx-Utils-"+DateTimeUtils.class.getSimpleName();
	
	private static final Locale DEFAULT_LOCALE = Locale.getDefault();
	private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", DEFAULT_LOCALE);
	private static final SimpleDateFormat TIMEZONE_FORMAT = new SimpleDateFormat("Z", DEFAULT_LOCALE);

	public static String getTimeZoneOffset() {
		return TIMEZONE_FORMAT.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), DEFAULT_LOCALE).getTime());
	}
	
	public static String getDateTime() {
		Date date = new Date();
		return DATETIME_FORMAT.format(date);
	}
	
	public static String getDateTime(Date date) {
		return DATETIME_FORMAT.format(date);
	}
	
	public static String getDateTime(long date) {
		return DATETIME_FORMAT.format(new Date(date));
	}
	
	public static Date getDateFromString(String dateString) {
		try {
			return DATETIME_FORMAT.parse(dateString);
		} catch (ParseException e) {
			RfcxLog.logExc(TAG, e);
		}
		return null;
	}
	
	public static Calendar nextOccurenceOf(int hour, int minute, int second) {
		Calendar rightNow = Calendar.getInstance();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		if (rightNow.getTimeInMillis() >= calendar.getTimeInMillis()) {
			calendar.set(Calendar.DAY_OF_YEAR, rightNow.get(Calendar.DAY_OF_YEAR)+1);
		}
		return calendar;
	}
	
	public static Calendar nextOccurenceOf(String HH_MM_SS) {
		String[] timePieces = HH_MM_SS.split(":");
		int hour = (int) Integer.parseInt(timePieces[0]);
		int minute = (int) Integer.parseInt(timePieces[1]);
		int second = 0;
		if (timePieces.length == 3) { second = (int) Integer.parseInt(timePieces[2]); }
		return nextOccurenceOf(hour,minute,second);
	}
	
	public static String milliSecondsAsMinutes(long milliSeconds) {
		StringBuilder rtrnStr = new StringBuilder();
		
		int minutes = (int) Math.floor( milliSeconds / 60000 );
		rtrnStr.append(minutes).append(" minutes");
		
		int seconds = Math.round((milliSeconds - (minutes * 60000)) / 1000);
		if (seconds > 0) { rtrnStr.append(", ").append(seconds).append(" seconds"); }
		
		return rtrnStr.toString();
	}
	
}
