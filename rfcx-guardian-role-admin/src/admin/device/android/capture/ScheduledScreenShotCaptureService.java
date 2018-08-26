package admin.device.android.capture;

import admin.RfcxGuardian;
import android.app.IntentService;
import android.content.Intent;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.service.RfcxServiceHandler;

public class ScheduledScreenShotCaptureService extends IntentService {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ScheduledScreenShotCaptureService.class);
	
	private static final String SERVICE_NAME = "ScheduledScreenShotCapture";
	
	public static final long SCHEDULED_SCREENSHOT_CYCLE_DURATION = ( 30 * 60 * 1000 ); // every 30 minutes
		
	public ScheduledScreenShotCaptureService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();
		
		app.rfcxServiceHandler.triggerService("ScreenShotCapture", true);
	
	}
	
	
}
