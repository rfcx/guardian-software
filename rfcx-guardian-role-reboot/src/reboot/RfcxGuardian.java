package reboot;

import rfcx.utility.misc.ShellCommands;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceConnectivity;
import rfcx.utility.device.DeviceI2cUtils;
import rfcx.utility.device.control.DeviceAirplaneMode;
import rfcx.utility.device.hardware.DeviceHardwareUtils;
import rfcx.utility.device.hardware.DeviceHardware_Huawei_U8150;
import rfcx.utility.rfcx.RfcxDeviceGuid;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxPrefs;
import rfcx.utility.rfcx.RfcxRole;
import rfcx.utility.service.RfcxServiceHandler;

import admin.device.android.capture.DeviceLogCatDb;
import admin.device.android.capture.DeviceLogCatCaptureService;
import admin.device.android.capture.DeviceScreenShotDb;
import admin.device.android.capture.DeviceScreenShotCaptureService;
import admin.device.android.capture.ScheduledLogCatCaptureService;
import admin.device.android.capture.ScheduledScreenShotCaptureService;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import reboot.control.RebootTriggerJobService;
import reboot.control.ScheduledRebootService;
import reboot.device.android.control.AirplaneModeEnableService;
import reboot.device.android.control.AirplaneModeToggleService;
import reboot.device.android.control.DateTimeSntpSyncJobService;
import reboot.device.android.control.ForceRoleRelaunchService;
import reboot.device.sentinel.DeviceSentinelService;
import reboot.device.sentinel.SentinelPowerDb;
import reboot.device.sentinel.SentinelPowerUtils;
import reboot.receiver.AirplaneModeReceiver;
import reboot.receiver.ConnectivityReceiver;

public class RfcxGuardian extends Application {
	
	public String version;
	
	public static final String APP_ROLE = "Reboot";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

	public RfcxDeviceGuid rfcxDeviceGuid = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public String[] RfcxCoreServices = 
			new String[] { 
			};
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceGuid = new RfcxDeviceGuid(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(this, logTag);
		this.rfcxPrefs.writeVersionToFile(this.version);

		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
		
		setDbHandlers();
		setServiceHandlers();
		
		ShellCommands.triggerNeedForRootAccess(this);
		DeviceI2cUtils.resetI2cPermissions(this);
		DateTimeUtils.resetDateTimeReadWritePermissions(this);
		
		this.sentinelPowerUtils = new SentinelPowerUtils(this);
		
		initializeRoleServices();
		
		// fix GPS functionality for the Huawei phones
		if (DeviceHardware_Huawei_U8150.isDevice_Huawei_U8150()) {
			DeviceHardware_Huawei_U8150.checkOrResetGPSFunctionality(this);
		}
		
	}
	
	public void onTerminate() {
		super.onTerminate();

		this.unregisterReceiver(connectivityReceiver);
		this.unregisterReceiver(airplaneModeReceiver);
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices() {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			
			String[] runOnceOnlyOnLaunch = new String[] {
					"ServiceMonitor"
							+"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
							+"|"+ServiceMonitor.SERVICE_MONITOR_CYCLE_DURATION
							,
					"ScheduledReboot"
							+"|"+DateTimeUtils.nextOccurenceOf(this.rfcxPrefs.getPrefAsString("reboot_forced_daily_at")).getTimeInMillis()
							+"|"+( 24 * 60 * 60 * 1000 ) // repeats daily
							,
					"ScheduledScreenShotCapture"
							+"|"+DateTimeUtils.nowPlusThisLong("00:00:30").getTimeInMillis() // waits thirty seconds before running
							+"|"+( this.rfcxPrefs.getPrefAsLong("admin_screenshot_capture_cycle") * 60 * 1000 )
							,
					"ScheduledLogCatCapture"
							+"|"+DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
							+"|"+( this.rfcxPrefs.getPrefAsLong("admin_log_capture_cycle") * 60 * 1000 )
			};
			
			String[] onLaunchServices = new String[ RfcxCoreServices.length + runOnceOnlyOnLaunch.length ];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
		}
	}
	
	private void setDbHandlers() {
		
		this.sentinelPowerDb = new SentinelPowerDb(this, this.version);
		this.deviceScreenShotDb = new DeviceScreenShotDb(this, this.version);
		this.deviceLogCatDb = new DeviceLogCatDb(this, this.version);
	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerJobService.class);
		this.rfcxServiceHandler.addService("ScheduledReboot", ScheduledRebootService.class);
		this.rfcxServiceHandler.addService("AirplaneModeToggle", AirplaneModeToggleService.class);
		this.rfcxServiceHandler.addService("AirplaneModeEnable", AirplaneModeEnableService.class);
		this.rfcxServiceHandler.addService("DateTimeSntpSyncJob", DateTimeSntpSyncJobService.class);
		this.rfcxServiceHandler.addService("DeviceSentinel", DeviceSentinelService.class);
		this.rfcxServiceHandler.addService("ForceRoleRelaunch", ForceRoleRelaunchService.class);

		this.rfcxServiceHandler.addService("ScreenShotCapture", DeviceScreenShotCaptureService.class);
		this.rfcxServiceHandler.addService("ScheduledScreenShotCapture", ScheduledScreenShotCaptureService.class);

		this.rfcxServiceHandler.addService("LogCatCapture", DeviceLogCatCaptureService.class);
		this.rfcxServiceHandler.addService("ScheduledLogCatCapture", ScheduledLogCatCaptureService.class);
		
		
		
	}
    
}
