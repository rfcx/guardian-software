package org.rfcx.guardian.guardian.api.methods.clocksync;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ClockSyncUtils {

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ClockSyncUtils");
    private RfcxGuardian app;

    public ClockSyncUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
    }


}
