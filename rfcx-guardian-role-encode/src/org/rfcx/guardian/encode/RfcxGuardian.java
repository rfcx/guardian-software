package org.rfcx.guardian.encode;

import org.rfcx.guardian.encode.database.AudioEncodeDb;
import org.rfcx.guardian.encode.service.AudioEncodeService;
import org.rfcx.guardian.encode.service.AudioEncodeTrigger;
import org.rfcx.guardian.encode.service.CheckInTriggerIntentService;
import org.rfcx.guardian.encode.service.ServiceMonitorIntentService;
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
	
	public static final String APP_ROLE = "Encode";

	private static final String logTag = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public AudioEncodeDb audioEncodeDb = null;
	
	public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
	
	@Override
	public void onCreate() {
		
		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, logTag);
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
						"AudioEncodeTrigger",
						"ServiceMonitor"
							+"|"+DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits one minute before running
							+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
					}, 
				true);
		}
	}
	
	private void setDbHandlers() {
		this.audioEncodeDb = new AudioEncodeDb(this, this.version);
	}
	
	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("AudioEncode", AudioEncodeService.class);
		this.rfcxServiceHandler.addService("AudioEncodeTrigger", AudioEncodeTrigger.class);
		this.rfcxServiceHandler.addService("CheckInTrigger", CheckInTriggerIntentService.class);
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
	}
    
}
