package org.rfcx.guardian.guardian.api.msg;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ApiShortMsgUtils {

	public ApiShortMsgUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiShortMsgUtils");

	private RfcxGuardian app;







}
