package org.rfcx.guardian.guardian.status;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class GuardianStatus extends RfcxStatus {

	private static final String fetchTargetRole = "admin";

	public GuardianStatus(Context context) {
		super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
		this.app = (RfcxGuardian) context.getApplicationContext();
		setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
	}

	private final RfcxGuardian app;

	@Override
	protected boolean getStatusBasedOnRoleSpecificLogic(int group, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {
		boolean statusValue = fallbackValue;
		boolean reportUpdate = false;

		if (isGroup( Tag.AUDIO_CAPTURE, group)) {

			if (isStatusType( Type.ALLOWED, statusType)) {
				statusValue = app.audioCaptureUtils.isAudioCaptureAllowed( true, printFeedbackInLog);
				reportUpdate = true;

			} else if (isStatusType( Type.ENABLED, statusType)) {
				statusValue = !app.audioCaptureUtils.isAudioCaptureDisabled(printFeedbackInLog);
				reportUpdate = true;
			}

		} else if (isGroup( Tag.API_CHECKIN, group)) {

			if (isStatusType( Type.ALLOWED, statusType)) {
				statusValue = app.apiCheckInHealthUtils.isApiCheckInAllowed(true, printFeedbackInLog);
				reportUpdate = true;

			} else if (isStatusType( Type.ENABLED, statusType)) {
				statusValue = !app.apiCheckInHealthUtils.isApiCheckInDisabled(printFeedbackInLog);
				reportUpdate = true;
			}

		}/* else if (isGroup( Tag.SBD_COMMUNICATION, group)) {

			if (isStatusType( Type.ALLOWED, statusType)) {
				statusValue = fallbackValue;
				reportUpdate = true;

			} else if (isStatusType( Type.ENABLED, statusType)) {
				statusValue = fallbackValue;
				reportUpdate = true;
			}
		}*/

		if (reportUpdate) { Log.w(logTag, "Refreshed local status cache for '"+ groups[group]+"', 'is_"+statusTypes[statusType]+"'"); }
		return statusValue;
	}


}
