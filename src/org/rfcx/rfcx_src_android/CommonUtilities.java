package org.rfcx.rfcx_src_android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommonUtilities {
	
	public String currentDateTime() {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		Date dateTime = new Date();
		return dateFormat.format(dateTime);
	}
	
}
