package org.rfcx.guardian.updater.status;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class UpdaterStatus extends RfcxStatus {

    private static final String fetchTargetRole = "guardian";
    private final RfcxGuardian app;

    public UpdaterStatus(Context context) {
        super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
        this.app = (RfcxGuardian) context.getApplicationContext();
        setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
    }

    @Override
    protected boolean[] getStatusBasedOnRoleSpecificLogic(int group, boolean[] fallbackValues, boolean printFeedbackInLog) {
        boolean[] statusValues = fallbackValues;
        boolean reportUpdate = false;

        // we'd put some functionality here

        if (reportUpdate) {
            Log.w(logTag, "Refreshed local status cache for '" + statusGroups[group] + "'");
        }
        return statusValues;
    }


}
