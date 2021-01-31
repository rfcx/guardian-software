package org.rfcx.guardian.admin.status;

import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class AdminStatus extends RfcxStatus {

	private static final String fetchTargetRole = "guardian";

	public AdminStatus(Context context) {
		super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
		this.app = (RfcxGuardian) context.getApplicationContext();
		setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
	}

	private final RfcxGuardian app;

	@Override
	protected boolean getStatusBasedOnRoleSpecificLogic(int activityType, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {

		boolean statusValue = fallbackValue;

		if (statusType == getStatusType("allowed")) {
			statusValue = !app.sentinelPowerUtils.isReducedCaptureModeActive_BasedOnSentinelPower(activityTypes[activityType]);

		}

		return statusValue;
	}


}
