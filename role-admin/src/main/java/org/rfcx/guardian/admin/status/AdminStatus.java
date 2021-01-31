package org.rfcx.guardian.admin.status;

import android.content.Context;
import android.util.Log;

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
	protected boolean getStatusBasedOnRoleSpecificLogic(int group, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {

		boolean statusValue = fallbackValue;
		boolean reportUpdate = false;

		if (isStatusType( Type.ALLOWED, statusType)) {
			statusValue = !app.sentinelPowerUtils.isReducedCaptureModeActive_BasedOnSentinelPower(statusGroups[group]);
			reportUpdate = true;
		}

		if (reportUpdate) { Log.w(logTag, "Refreshed local status cache for '"+ statusGroups[group]+"', 'is_"+statusTypes[statusType]+"'"); }

		return statusValue;
	}


}
