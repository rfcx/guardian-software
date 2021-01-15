package org.rfcx.guardian.admin.device.android.capture;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class ScheduledScreenShotCaptureService extends IntentService {

	public static final String SERVICE_NAME = "ScheduledScreenShotCapture";
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledScreenShotCaptureService");
		
	public ScheduledScreenShotCaptureService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxSvc.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxSvc.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		if (app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ADMIN_ENABLE_SCREENSHOT_CAPTURE)) {
			app.rfcxSvc.triggerService( ScreenShotCaptureService.SERVICE_NAME, true);
		} else {
			Log.i(logTag, "Scheduled ScreenShot Capture is currently disabled in preferences.");
		}
		
	}
	
	
}
