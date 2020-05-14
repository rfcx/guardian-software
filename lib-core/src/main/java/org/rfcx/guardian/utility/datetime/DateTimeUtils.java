package org.rfcx.guardian.utility.datetime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DateTimeUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "DateTimeUtils");
	
	private static final Locale DEFAULT_LOCALE = Locale.getDefault();
	private static final SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", DEFAULT_LOCALE);
	private static final SimpleDateFormat TIMEZONE_FORMAT = new SimpleDateFormat("Z", DEFAULT_LOCALE);

	public static String getTimeZoneOffset() {
		return TIMEZONE_FORMAT.format(Calendar.getInstance(TimeZone.getTimeZone("GMT"), DEFAULT_LOCALE).getTime());
	}
	
	public static String getDateTime() { return DATETIME_FORMAT.format(new Date()); }
	public static String getDateTime(Date date) { return DATETIME_FORMAT.format(date); }
	public static String getDateTime(long date) {
		return DATETIME_FORMAT.format(new Date(date));
	}
	
	public static Date getDateFromString(String dateString) {
		try {
			return DATETIME_FORMAT.parse(dateString);
		} catch (ParseException e) {
			RfcxLog.logExc(logTag, e);
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
	
	public static Calendar nowPlusThisLong(int hours, int minutes, int seconds) {
		long rightNowPlus = (new Date()).getTime() + (hours * 60 * 60 * 1000) + (minutes * 60 * 1000) + (seconds * 1000);
		Calendar nowPlusCalendar = Calendar.getInstance();
		try { nowPlusCalendar.setTimeInMillis(rightNowPlus); } catch (Exception e) { RfcxLog.logExc(logTag, e); }
		return nowPlusCalendar;
	}

	public static Calendar nowPlusThisLong(String HH_MM_SS) {
		String[] timePieces = HH_MM_SS.split(":");
		int hours = (int) Integer.parseInt(timePieces[0]);
		int minutes = (int) Integer.parseInt(timePieces[1]);
		int seconds = 0;
		if (timePieces.length == 3) { seconds = (int) Integer.parseInt(timePieces[2]); }
		return nowPlusThisLong(hours,minutes,seconds);
	}

	public static String timeStampDifferenceFromNowAsReadableString(Date date) {
		return milliSecondDurationAsReadableString(Math.abs(timeStampDifferenceFromNowInMilliSeconds(date)), true);
	}
	
	public static String timeStampDifferenceFromNowAsReadableString(long date) {
		return timeStampDifferenceFromNowAsReadableString(new Date(date));
	}
	
	public static long timeStampDifferenceFromNowInMilliSeconds(Date date) {
		long rightNow = (new Date()).getTime();
		long timeStamp = date.getTime();
		return timeStamp - rightNow;
	}
	
	public static long timeStampDifferenceFromNowInMilliSeconds(long date) {
		return timeStampDifferenceFromNowInMilliSeconds(new Date(date));
	}
	
	public static String milliSecondDurationAsReadableString(long milliSeconds, boolean displayEvenIfZero) {
		StringBuilder rtrnStr = new StringBuilder();
		
		int hours = (int) Math.floor( milliSeconds / 3600000 );
		if (hours > 0) { rtrnStr.append(hours).append(" hour").append((hours != 1) ? "s" : ""); }
		
		int minutes = (int) Math.floor( (milliSeconds - (hours * 3600000)) / 60000 );
		if (minutes > 0) { rtrnStr.append((hours > 0) ? ", " : "").append(minutes).append(" minute").append((minutes != 1) ? "s" : ""); }
		
		int seconds = Math.round((milliSeconds - (hours * 3600000) - (minutes * 60000)) / 1000);
		if (((hours+minutes) == 0) || (seconds > 0) || displayEvenIfZero) {
			rtrnStr.append(((hours > 0) || (minutes > 0)) ? ", " : "").append(seconds).append(" second").append((seconds != 1) ? "s" : "");
		}
		
		return rtrnStr.toString();
	}
	
	public static String milliSecondDurationAsReadableString(long milliSeconds) {
		return milliSecondDurationAsReadableString(milliSeconds, false);
	}
	
	public static boolean isTimeStampWithinTimeRange(Date timeStamp, int startHour, int startMinute, int startSecond, int endHour, int endMinute, int endSecond) {
		
		Calendar startCalendar = Calendar.getInstance();
		startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
		startCalendar.set(Calendar.MINUTE, startMinute);
		startCalendar.set(Calendar.SECOND, startSecond);
		
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.set(Calendar.HOUR_OF_DAY, endHour);
		endCalendar.set(Calendar.MINUTE, endMinute);
		endCalendar.set(Calendar.SECOND, endSecond);
		
		return ((timeStamp.getTime() >= startCalendar.getTimeInMillis()) && (timeStamp.getTime() <= endCalendar.getTimeInMillis()));
	}
	
	public static boolean isTimeStampWithinTimeRange(Date timeStamp, String start_HH_MM_SS, String end_HH_MM_SS) {
		
		String[] startTimePieces = start_HH_MM_SS.split(":");
		int startHour = (int) Integer.parseInt(startTimePieces[0]);
		int startMinute = (int) Integer.parseInt(startTimePieces[1]);
		int startSecond = 0;
		if (startTimePieces.length == 3) { startSecond = (int) Integer.parseInt(startTimePieces[2]); }
		
		String[] endTimePieces = end_HH_MM_SS.split(":");
		int endHour = (int) Integer.parseInt(endTimePieces[0]);
		int endMinute = (int) Integer.parseInt(endTimePieces[1]);
		int endSecond = 0;
		if (endTimePieces.length == 3) { endSecond = (int) Integer.parseInt(endTimePieces[2]); }
		
		return isTimeStampWithinTimeRange(timeStamp, startHour, startMinute, startSecond, endHour, endMinute, endSecond);
	}

	public static void resetDateTimeReadWritePermissions(Context context) {
		Log.v(logTag, "Resetting Permissions on DateTime Alarm Handlers...");
		ShellCommands.executeCommandAsRootAndIgnoreOutput("chmod 666 /dev/alarm;", context);
	}

	public static void setSystemTimezone(String timezoneInTzFormat, Context context) {
		for (String validTz : TimeZone.getAvailableIDs()) {
			if (validTz.equalsIgnoreCase(timezoneInTzFormat)) {
				Log.v(logTag, "Setting System Timezone to '"+timezoneInTzFormat+"'");
				AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
				alarmManager.setTimeZone(validTz);
				break;
			}
		}
	}

	public static void listValidTimezones() {
		for (String validTz : TimeZone.getAvailableIDs()) {
			Log.v(logTag, validTz);
		}
	}

}
