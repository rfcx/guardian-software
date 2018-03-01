package admin;

import rfcx.utility.misc.ShellCommands;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.device.DeviceConnectivity;
import rfcx.utility.device.DeviceI2cUtils;
import rfcx.utility.device.control.DeviceAirplaneMode;
import rfcx.utility.rfcx.RfcxDeviceGuid;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxPrefs;
import rfcx.utility.rfcx.RfcxRole;
import rfcx.utility.service.RfcxServiceHandler;

import admin.device.android.capture.DeviceLogCatCaptureDb;
import admin.device.android.capture.DeviceScreenShotDb;
import admin.device.android.capture.DeviceScreenShotJobService;
import admin.device.android.control.AirplaneModeOffJobService;
import admin.device.android.control.AirplaneModeOnJobService;
import admin.device.android.control.DateTimeSntpSyncJobService;
import admin.device.android.control.RebootTriggerJobService;
import admin.device.sentinel.DeviceSentinelService;
import admin.device.sentinel.DeviceSentinelPowerDb;
import admin.device.sentinel.DeviceSentinelPowerUtils;
import admin.receiver.AirplaneModeReceiver;
import admin.receiver.ConnectivityReceiver;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class RfcxGuardian extends Application {
	
	public String version;
	
	public static final String APP_ROLE = "Admin";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

	public RfcxDeviceGuid rfcxDeviceGuid = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	public DeviceScreenShotDb deviceScreenShotDb = null;
	public DeviceLogCatCaptureDb deviceLogCatCaptureDb = null;
	public DeviceSentinelPowerDb deviceSentinelPowerDb = null;
	
	public DeviceSentinelPowerUtils deviceSentinelPowerUtils = null;
	
	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
	public DeviceAirplaneMode deviceAirplaneMode = new DeviceAirplaneMode(APP_ROLE);

	// Receivers
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	
	public String[] RfcxCoreServices = 
			new String[] { 
				"DeviceSentinel"
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
		
		(new ShellCommands(this, APP_ROLE)).triggerNeedForRootAccess();
		DeviceI2cUtils.resetI2cPermissions(this, APP_ROLE);
		DateTimeUtils.resetDateTimeReadWritePermissions(this, APP_ROLE);
		
		this.deviceSentinelPowerUtils = new DeviceSentinelPowerUtils(getApplicationContext());
		
		initializeRoleServices();
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
			
			String[] onLaunchServices = new String[RfcxCoreServices.length+1];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			onLaunchServices[RfcxCoreServices.length] = 
					"ServiceMonitor"
						+"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
						+"|"+ServiceMonitor.SERVICE_MONITOR_CYCLE_DURATION
						;
			
			this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
		}
	}
	
	private void setDbHandlers() {
		
		this.deviceSentinelPowerDb = new DeviceSentinelPowerDb(this, this.version);
		this.deviceScreenShotDb = new DeviceScreenShotDb(this, this.version);
		this.deviceLogCatCaptureDb = new DeviceLogCatCaptureDb(this, this.version);
	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
		this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerJobService.class);
		this.rfcxServiceHandler.addService("ScreenShotJob", DeviceScreenShotJobService.class);
		this.rfcxServiceHandler.addService("AirplaneModeOffJob", AirplaneModeOffJobService.class);
		this.rfcxServiceHandler.addService("AirplaneModeOnJob", AirplaneModeOnJobService.class);
		this.rfcxServiceHandler.addService("DateTimeSntpSyncJob", DateTimeSntpSyncJobService.class);
		this.rfcxServiceHandler.addService("DeviceSentinel", DeviceSentinelService.class);
	}
    
}
