package rfcx.utility.rfcx;

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
			excMsg.append("An exception is thrown, but the exception itself is null.");
		}
		
		Log.e( logTag, excMsg.toString());
		
	}
	
	public static void logThrowable(String logTag, Throwable thrw) {
		
		StringBuilder thrwMsg = new StringBuilder();
		if ( thrw != null ) {
			thrwMsg.append("Message: ").append(thrw.getMessage()).append(" ||| ")
				.append("Cause: ").append(thrw.getCause()).append(" ||| ")
				.append("StackTrace: ").append(TextUtils.join(" | ",thrw.getStackTrace()));
		} else {
			thrwMsg.append("The throwable itself is null.");
		}
		
		Log.e( logTag, thrwMsg.toString());
		
	}

}
