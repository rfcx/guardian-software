package org.rfcx.guardian.api.receiver;

import java.util.Calendar;

import org.rfcx.guardian.api.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AirplaneModeReceiver.class.getSimpleName();
	
	private RfcxGuardian app = null;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		if (app == null) app = (RfcxGuardian) context.getApplicationContext();

		Log.v(TAG,
				"AirplaneMode " + ( app.airplaneMode.isEnabled(context) ? "Enabled" : "Disabled" )
				+ " at "+(Calendar.getInstance()).getTime().toLocaleString());
		
	}

}
