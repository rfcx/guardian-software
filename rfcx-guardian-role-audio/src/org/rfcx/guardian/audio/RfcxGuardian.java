package org.rfcx.guardian.audio;

import org.rfcx.guardian.audio.capture.AudioCapture;
import org.rfcx.guardian.audio.service.AudioCaptureService;
import org.rfcx.guardian.audio.service.AudioEncodeTriggerIntentService;
import org.rfcx.guardian.audio.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.Context;

public class RfcxGuardian extends Application {

	public String version;
	Context context;
	
	public static final String APP_ROLE = "Audio";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public AudioCapture audioCapture = null;
	
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, TAG);
		this.rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices() {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			this.rfcxServiceHandler.triggerServiceSequence(
				"OnLaunchServiceSequence", 
					new String[] { 
						"AudioCapture",
						"ServiceMonitor"
							+"|"+DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits one minute before running
							+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
					}, 
				true);
		}
	}
	
	private void setDbHandlers() {

	}
	
	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("AudioCapture", AudioCaptureService.class);
		this.rfcxServiceHandler.addService("AudioEncodeTrigger", AudioEncodeTriggerIntentService.class);
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
	}
    
}
