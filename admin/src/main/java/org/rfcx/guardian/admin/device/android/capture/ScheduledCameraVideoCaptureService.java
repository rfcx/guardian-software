package org.rfcx.guardian.admin.device.android.capture;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

public class ScheduledCameraVideoCaptureService extends IntentService {

	private static final String SERVICE_NAME = "ScheduledCameraVideoCapture";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ScheduledCameraVideoCaptureService");

	public ScheduledCameraVideoCaptureService() {
		super(logTag);
	}
	
	@Override
	protected void onHandleIntent(Intent inputIntent) {
		Intent intent = new Intent(RfcxServiceHandler.intentServiceTags(false, RfcxGuardian.APP_ROLE, SERVICE_NAME));
		sendBroadcast(intent, RfcxServiceHandler.intentServiceTags(true, RfcxGuardian.APP_ROLE, SERVICE_NAME));;
		
		RfcxGuardian app = (RfcxGuardian) getApplication();

//		if (app.rfcxPrefs.getPrefAsBoolean("admin_enable_photo_capture")) {
//			app.rfcxServiceHandler.triggerService("CameraVideoCapture", true);
//		} else {
//			Log.i(logTag, "Scheduled Photo Capture is currently disabled in preferences.");
//		}
		
	}
	
	
}
