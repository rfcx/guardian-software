package org.rfcx.guardian.telecom;

import org.rfcx.guardian.intentservice.AudioEncodeIntentService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class CarrierInteraction {

	private static final String TAG = CarrierInteraction.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private static final String HASH = Uri.encode("#");
	
	public void submitCode(Context context, String code) {
        try {
        	Intent callIntent = new Intent("android.intent.action.CALL",Uri.parse("tel:"+code.replaceAll("#", HASH)));
        	callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	callIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	context.startActivity(callIntent);
        } catch (Exception e) {
        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
        }	
	}
	

	
	
}
