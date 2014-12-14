package org.rfcx.guardian.utility;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	public String getDateTime() {
		Date date = new Date();
		return dateFormat.format(date);
	}
	
	public String getDateTime(Date date) {
		return dateFormat.format(date);
	}
	
}
