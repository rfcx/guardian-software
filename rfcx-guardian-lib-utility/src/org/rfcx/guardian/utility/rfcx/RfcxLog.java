package org.rfcx.guardian.utility.rfcx;

import android.text.TextUtils;
import android.util.Log;

public class RfcxLog {
	
	public static String generateLogTag(String appRole, Class logClass) {
		
		return (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(logClass.getSimpleName()).toString();
		
	}
	
	public static void logExc(String logTag, Exception exc) {
		
		StringBuilder excMsg = new StringBuilder();
		if ( exc != null ) {
			excMsg.append("Message: ").append(exc.getMessage()).append(" ||| ")
				.append("Cause: ").append(exc.getCause()).append(" ||| ")
				.append("StackTrace: ").append(TextUtils.join(" | ",exc.getStackTrace()));
		} else {
			excMsg.append("An exception thrown, but the exception itself is null.");
		}
		
		Log.e( logTag, excMsg.toString());
		
	}

}
