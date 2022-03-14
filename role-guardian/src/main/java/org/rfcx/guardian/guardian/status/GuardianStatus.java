package org.rfcx.guardian.guardian.status;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class GuardianStatus extends RfcxStatus {

    private static final String fetchTargetRole = "admin";
    private final RfcxGuardian app;

    public GuardianStatus(Context context) {
        super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
        this.app = (RfcxGuardian) context.getApplicationContext();
        setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
    }

    @Override
    protected boolean[] getStatusBasedOnRoleSpecificLogic(int group, boolean[] fallbackValues, boolean printFeedbackInLog) {
        boolean[] statusValues = fallbackValues;
        boolean reportUpdate = false;
        for (int statusType = 0; statusType < statusTypes.length; statusType++) {

            if (isGroup(Group.AUDIO_CAPTURE, group)) {

                if (isStatusType(Type.ALLOWED, statusType)) {
                    statusValues[statusType] = app.audioCaptureUtils.isAudioCaptureAllowed(true, printFeedbackInLog);

                } else if (isStatusType(Type.ENABLED, statusType)) {
                    statusValues[statusType] = !app.audioCaptureUtils.isAudioCaptureDisabled(printFeedbackInLog);
                }

            } else if (isGroup(Group.API_CHECKIN, group)) {

                if (isStatusType(Type.ALLOWED, statusType)) {
                    statusValues[statusType] = app.apiCheckInHealthUtils.isApiCheckInAllowed(true, printFeedbackInLog);

                } else if (isStatusType(Type.ENABLED, statusType)) {
                    statusValues[statusType] = !app.apiCheckInHealthUtils.isApiCheckInDisabled(printFeedbackInLog);
                }

            } else if (isGroup(Group.SBD_COMMUNICATION, group)) {

                if (isStatusType(Type.ALLOWED, statusType)) {
                    statusValues[statusType] = fallbackValues[statusType];

                } else if (isStatusType(Type.ENABLED, statusType)) {
                    statusValues[statusType] = fallbackValues[statusType];
                }
            }

        }
        if (reportUpdate) {
            Log.w(logTag, "Refreshed local status cache for '" + statusGroups[group] + "'");
        }
        return statusValues;
    }


}
