package org.rfcx.guardian.guardian.api.methods.download;

import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ApiDownloadUtils {

	public ApiDownloadUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiDownloadUtils");

	private RfcxGuardian app;






}
