package org.rfcx.guardian.classify.status;

import android.content.Context;

import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class ClassifyStatus extends RfcxStatus {

	private static final String fetchTargetRole = "guardian";

	public ClassifyStatus(Context context) {
		super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
		this.app = (RfcxGuardian) context.getApplicationContext();
		setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
	}

	private final RfcxGuardian app;

	@Override
	protected boolean getStatusBasedOnRoleSpecificLogic(int activityType, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {

		boolean statusValue = fallbackValue;

		// we'd put some functionality here

		return statusValue;
	}


}
