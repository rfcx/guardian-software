package org.rfcx.guardian.admin.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class MmsReceiver extends BroadcastReceiver {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "MmsReceiver");
	
    @Override
    public void onReceive(Context context, Intent intent) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

    }
    
}
