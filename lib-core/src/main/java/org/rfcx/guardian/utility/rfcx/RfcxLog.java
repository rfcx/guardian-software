package org.rfcx.guardian.utility.rfcx;

import android.text.TextUtils;
import android.util.Log;

public class RfcxLog {

	public static String generateLogTag(String appRole, Class logClass) {
		return "Rfcx-" + appRole + "-" + logClass.getSimpleName();
	}

	public static String generateLogTag(String appRole, String logClassName) {
		return "Rfcx-" + appRole + "-" + logClassName;
	}
	
	public static String getExceptionContentAsString(Exception exc) {
		StringBuilder excMsg = new StringBuilder();
		if ( exc != null ) {
			excMsg.append("Message: ").append(exc.getMessage()).append(" ||| ")
				.append("Cause: ").append(exc.getCause()).append(" ||| ")
				.append("StackTrace: ").append(TextUtils.join(" | ",exc.getStackTrace()));
		} else {
			excMsg.append("An exception is thrown, but the exception itself is null.");
		}
		return excMsg.toString();
	}
	
	public static String getThrowableContentAsString(Throwable thrw) {
		StringBuilder thrwMsg = new StringBuilder();
		if ( thrw != null ) {
			thrwMsg.append("Message: ").append(thrw.getMessage()).append(" ||| ")
				.append("Cause: ").append(thrw.getCause()).append(" ||| ")
				.append("StackTrace: ").append(TextUtils.join(" | ",thrw.getStackTrace()));
		} else {
			thrwMsg.append("The throwable itself is null.");
		}
		return thrwMsg.toString();
	}
	
	public static void logExc(String logTag, Exception exc, String optionalExtraTag) {
		String extraTag = ( (optionalExtraTag == null) || (optionalExtraTag.length() == 0) ) ? "" : ("Tag: " + optionalExtraTag + " ||| ");
		Log.e( logTag, extraTag + getExceptionContentAsString(exc) );
	}

	public static void logExc(String logTag, Exception exc) {
		logExc(logTag, exc, null);
	}
	
	public static void logThrowable(String logTag, Throwable thrw) {
		Log.e( logTag, getThrowableContentAsString(thrw));
	}

}
