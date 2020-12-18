package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledLogcatCaptureService extends IntentService {

	private static final String SERVICE_NAME = "ScheduledLogcatCapture";
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledLogcatCaptureService");
		
	public ScheduledLogcatCaptureService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxPrefs.getPrefAsBoolean("admin_enable_log_capture")) {
			app.rfcxServiceHandler.triggerService("LogcatCapture", true);
		} else {
			Log.i(logTag, "Scheduled Logcat Capture is currently disabled in preferences.");
		}
	}
	
	
}
