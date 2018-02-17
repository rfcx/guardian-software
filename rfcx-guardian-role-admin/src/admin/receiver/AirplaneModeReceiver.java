package admin.receiver;

import java.util.Calendar;

import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.control.DeviceAirplaneMode;
import rfcx.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import admin.RfcxGuardian;

public class AirplaneModeReceiver extends BroadcastReceiver {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AirplaneModeReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.v(logTag, 
				(new StringBuilder())
				.append("AirplaneMode ")
				.append(DeviceAirplaneMode.isEnabled(context) ? "Enabled" : "Disabled")
				.append(" at ")
				.append(DateTimeUtils.getDateTime(System.currentTimeMillis()))
				.toString()
				);
				
	
				
		
	}

}
