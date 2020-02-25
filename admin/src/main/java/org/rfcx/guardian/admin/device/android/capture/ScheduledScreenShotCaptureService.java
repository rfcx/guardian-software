package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledScreenShotCaptureService extends IntentService {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ScheduledScreenShotCaptureService.class);
	
	private static final String SERVICE_NAME = "ScheduledScreenShotCapture";
		
	public ScheduledScreenShotCaptureService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxPrefs.getPrefAsBoolean("admin_enable_screenshot_capture")) {
			app.rfcxServiceHandler.triggerService("ScreenShotCapture", true);
		} else {
			Log.i(logTag, "Scheduled ScreenShot Capture is currently disabled in preferences.");
		}
		
	}
	
	
}
