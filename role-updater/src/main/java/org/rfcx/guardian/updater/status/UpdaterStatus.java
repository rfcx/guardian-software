package org.rfcx.guardian.updater.status;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxStatus;

public class UpdaterStatus extends RfcxStatus {

	private static final String fetchTargetRole = "guardian";

	public UpdaterStatus(Context context) {
		super(RfcxGuardian.APP_ROLE, fetchTargetRole, ((RfcxGuardian) context.getApplicationContext()).rfcxGuardianIdentity, context.getContentResolver());
		this.app = (RfcxGuardian) context.getApplicationContext();
		setOrResetCacheExpirations(this.app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));
	}

	private final RfcxGuardian app;

	@Override
	protected boolean getStatusBasedOnRoleSpecificLogic(int group, int statusType, boolean fallbackValue, boolean printFeedbackInLog) {
		boolean statusValue = fallbackValue;
		boolean reportUpdate = false;

		// we'd put some functionality here

		if (reportUpdate) { Log.w(logTag, "Refreshed local status cache for '"+ groups[group]+"', 'is_"+statusTypes[statusType]+"'"); }
		return statusValue;
	}


}
