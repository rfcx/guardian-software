package org.rfcx.guardian.admin.receiver;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BootReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.v(logTag, "Rfcx-"+RfcxGuardian.APP_ROLE+" BootReceiver Launched...");
		
		// initializing rfcx application
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

		// record boot time in database
		app.rebootDb.dbRebootComplete.insert(System.currentTimeMillis());
	}

}

