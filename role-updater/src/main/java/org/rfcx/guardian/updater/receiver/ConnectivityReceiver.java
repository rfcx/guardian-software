package org.rfcx.guardian.updater.receiver;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ConnectivityReceiver");

    @Override
    public void onReceive(Context context, Intent intent) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        app.deviceConnectivity.updateConnectivityState(intent);

        app.apiUpdateRequestUtils.attemptToTriggerUpdateRequest(false, false);

    }


}
