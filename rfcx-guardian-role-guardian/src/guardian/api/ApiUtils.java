package guardian.api;

import android.content.Context;
import guardian.RfcxGuardian;
import rfcx.utility.rfcx.RfcxLog;

public class ApiUtils {

	public ApiUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiUtils.class);

	private RfcxGuardian app;
	

}
