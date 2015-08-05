package org.rfcx.guardian.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class DateTimeUtils {
	
	private static final String TAG = "RfcxGuardianInstaller-"+DateTimeUtils.class.getSimpleName();
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	public String getDateTime() {
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public String getDateTime(Date date) {
		return dateFormat.format(date);
	}
	
	public Date getDateFromString(String dateString) {
		try {
			return dateFormat.parse(dateString);
		} catch (ParseException e) {
			Log.e(TAG, e.getMessage());
			return new Date();
		}
	}
	
	public Calendar nextOccurenceOf(int hour, int minute, int second) {
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
	
}
