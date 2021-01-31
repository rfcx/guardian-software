package org.rfcx.guardian.guardian.status;

import android.content.Context;

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
	protected boolean getStatusBasedOnRoleSpecificLogic(int activityType, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {
		boolean statusValue = fallbackValue;

		if (activityType == getActivityType("audio_capture"))  {
			if (statusType == getStatusType("allowed")) {
				statusValue = app.audioCaptureUtils.isAudioCaptureAllowed( true, printFeedbackInLog);
			} else if (statusType == getStatusType("enabled")) {
				statusValue = !app.audioCaptureUtils.isAudioCaptureDisabled(printFeedbackInLog);
			}

		} else if (activityType == getActivityType("api_checkin")) {
			if (statusType == getStatusType("allowed")) {
				statusValue = app.apiCheckInHealthUtils.isApiCheckInAllowed(true, printFeedbackInLog);
			} else if (statusType == getStatusType("enabled")) {
				statusValue = !app.apiCheckInHealthUtils.isApiCheckInDisabled(printFeedbackInLog);
			}
		}

		return statusValue;
	}


}
