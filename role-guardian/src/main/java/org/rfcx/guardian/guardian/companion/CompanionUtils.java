package org.rfcx.guardian.guardian.companion;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class CompanionUtils {

	public CompanionUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionUtils");

	private RfcxGuardian app = null;





}
