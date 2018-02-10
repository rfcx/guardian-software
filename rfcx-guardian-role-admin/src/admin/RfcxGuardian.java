package admin;

import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.device.control.DeviceAirplaneMode;
import org.rfcx.guardian.utility.device.control.DeviceBluetooth;
import org.rfcx.guardian.utility.device.control.DeviceScreenShot;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceGuid;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import admin.sentinel.SentinelPowerDb;
import admin.sentinel.SentinelPowerUtils;
import admin.device.android.capture.DeviceLogCatCaptureDb;
import admin.device.android.capture.DeviceScreenShotDb;
import admin.device.android.capture.DeviceScreenShotJobService;
import admin.sentinel.I2cUtils;
import admin.service.AirplaneModeOffJobService;
import admin.service.AirplaneModeOnJobService;
import admin.service.I2cResetPermissionsJobService;
import admin.service.RebootTriggerJobService;

import org.rfcx.guardian.utility.ShellCommands;

import android.app.Application;
import android.content.Context;

public class RfcxGuardian extends Application {
	
	public String version;
	
	public static final String APP_ROLE = "Admin";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

	public RfcxDeviceGuid rfcxDeviceGuid = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public SentinelPowerUtils sentinelPowerUtils = null;
	public SentinelPowerDb sentinelPowerDb = null;
	
	public DeviceScreenShotDb deviceScreenShotDb = null;
	public DeviceLogCatCaptureDb deviceLogCatCaptureDb = null;
	
	public DeviceAirplaneMode deviceAirplaneMode = new DeviceAirplaneMode(APP_ROLE);
	public DeviceBluetooth deviceBluetooth = new DeviceBluetooth(APP_ROLE);
	
	public String[] RfcxCoreServices = 
			new String[] { 
				"I2cResetPermissions"
			};
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceGuid = new RfcxDeviceGuid(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, logTag);
		this.rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		setServiceHandlers();
		
		this.sentinelPowerUtils = new SentinelPowerUtils(this);
		
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
		
		this.sentinelPowerDb = new SentinelPowerDb(this, this.version);
		this.deviceScreenShotDb = new DeviceScreenShotDb(this, this.version);
		this.deviceLogCatCaptureDb = new DeviceLogCatCaptureDb(this, this.version);
	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerJobService.class);
		this.rfcxServiceHandler.addService("ScreenShotJob", DeviceScreenShotJobService.class);
		this.rfcxServiceHandler.addService("AirplaneModeOff", AirplaneModeOffJobService.class);
		this.rfcxServiceHandler.addService("AirplaneModeOn", AirplaneModeOnJobService.class);
		this.rfcxServiceHandler.addService("I2cResetPermissions", I2cResetPermissionsJobService.class);
	}
    
}
