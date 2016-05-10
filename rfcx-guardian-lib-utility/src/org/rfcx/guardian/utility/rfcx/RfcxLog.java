package org.rfcx.guardian.utility.rfcx;

import android.text.TextUtils;
import android.util.Log;

public class RfcxLog {
	
	private static final String NULL_EXC = "An exception thrown, but the exception itself is null.";
	
	public static void logExc(String TAG, Exception e) {
		Log.e(
			TAG,
			( e != null ) ? ( e.getMessage()+" ||| "+TextUtils.join(" | ",e.getStackTrace()) ) : NULL_EXC
			);
	}

}
