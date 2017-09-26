package org.rfcx.guardian.admin;

import org.rfcx.guardian.admin.service.AirplaneModeOffJobService;
import org.rfcx.guardian.admin.service.RebootTriggerIntentService;
import org.rfcx.guardian.admin.service.ScreenShotJobService;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.device.control.DeviceAirplaneMode;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.device.control.DeviceScreenShot;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.Context;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Admin";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public DeviceAirplaneMode deviceAirplaneMode = new DeviceAirplaneMode(APP_ROLE);
	public DeviceBluetooth deviceBluetooth = new DeviceBluetooth(APP_ROLE);
	
	public DeviceScreenShot deviceScreenShot = null;
	
	public String[] RfcxCoreServices = 
			new String[] { 
			};
	
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
		
		this.deviceScreenShot = new DeviceScreenShot(getApplicationContext());
		
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
			
			String[] onLaunchServices = new String[RfcxCoreServices.length+1];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			onLaunchServices[RfcxCoreServices.length] = 
					"ServiceMonitor"
						+"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits three minutes before running
						+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
						;
			
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true);
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerIntentService.class);
		this.rfcxServiceHandler.addService("ScreenShotJob", ScreenShotJobService.class);
		this.rfcxServiceHandler.addService("AirplaneModeOff", AirplaneModeOffJobService.class);
	}
    
}
