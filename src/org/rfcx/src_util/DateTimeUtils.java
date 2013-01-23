package org.rfcx.src_util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTimeUtils {
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
	
	public String getCurrDateTime() {
		Date dateTime = new Date();
		return dateFormat.format(dateTime);
	}
	
}
