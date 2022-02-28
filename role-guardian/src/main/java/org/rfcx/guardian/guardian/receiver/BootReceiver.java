package org.rfcx.guardian.guardian.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class BootReceiver extends BroadcastReceiver {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "BootReceiver");

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.v(logTag, "BootReceiver Launched...");

        // initializing rfcx application
        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

    }

}

