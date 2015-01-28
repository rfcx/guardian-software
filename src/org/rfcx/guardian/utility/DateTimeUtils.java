package org.rfcx.guardian.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.util.Log;

public class DateTimeUtils {
	
	private static final String TAG = "RfcxGuardian-"+DateTimeUtils.class.getSimpleName();
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
	
}
