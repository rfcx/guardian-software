package org.rfcx.guardian.admin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.device.android.control.ScheduledClockSyncService;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ConnectivityReceiver extends BroadcastReceiver {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ConnectivityReceiver");

    private boolean isFirstInstance = true;

    @Override
    public void onReceive(Context context, Intent intent) {

        RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();

        int disconnectedFor = app.deviceConnectivity.updateConnectivityStateAndReportDisconnectedFor(intent);

        if (this.isFirstInstance) {
            app.rfcxSvc.triggerService(
                    new String[]{
                            ScheduledClockSyncService.SERVICE_NAME,
                            "" + DateTimeUtils.nowPlusThisLong("00:00:10").getTimeInMillis(),
                            "norepeat"
                    }, false);
        }

        this.isFirstInstance = false;

    }

}
