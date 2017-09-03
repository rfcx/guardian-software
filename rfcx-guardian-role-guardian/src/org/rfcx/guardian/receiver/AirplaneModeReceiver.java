package org.rfcx.guardian.receiver;

import java.util.Calendar;

import org.rfcx.guardian.RfcxGuardian;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AirplaneModeReceiver extends BroadcastReceiver {
	
	public AirplaneModeReceiver(String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+AirplaneModeReceiver.class.getSimpleName();
	}

	private String logTag = "Rfcx-Utils-"+AirplaneModeReceiver.class.getSimpleName();
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		Log.v(logTag, "AirplaneMode " + ( app.deviceAirplaneMode.isEnabled(context) ? "Enabled" : "Disabled" )
						+ " at "+(Calendar.getInstance()).getTime().toLocaleString());
		
	}

}
