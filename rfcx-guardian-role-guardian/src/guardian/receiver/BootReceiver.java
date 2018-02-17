package guardian.receiver;

import rfcx.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import guardian.RfcxGuardian;

public class BootReceiver extends BroadcastReceiver {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BootReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Log.v(logTag, "Running BootReceiver");
		
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		// record boot time in database
		app.rebootDb.dbRebootComplete.insert(System.currentTimeMillis());
	
	}

}

