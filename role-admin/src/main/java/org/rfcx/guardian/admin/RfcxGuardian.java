package org.rfcx.guardian.admin;

import org.rfcx.guardian.admin.asset.AssetUtils;
import org.rfcx.guardian.admin.asset.ScheduledAssetCleanupService;
import org.rfcx.guardian.admin.device.android.capture.CameraCaptureDb;
import org.rfcx.guardian.admin.device.android.capture.CameraCaptureService;
import org.rfcx.guardian.admin.device.android.capture.ScheduledCameraCaptureService;
import org.rfcx.guardian.admin.device.android.control.AirplaneModeSetService;
import org.rfcx.guardian.admin.device.android.control.ScheduledClockSyncService;
import org.rfcx.guardian.admin.device.android.control.SystemSettingsService;
import org.rfcx.guardian.admin.device.android.ssh.SSHServerControlService;
import org.rfcx.guardian.admin.device.sentinel.SentinelSensorDb;
import org.rfcx.guardian.admin.device.sentinel.SentinelAccelUtils;
import org.rfcx.guardian.admin.device.sentinel.SentinelUtils;
import org.rfcx.guardian.admin.satellite.sbd.SbdDispatchCycleService;
import org.rfcx.guardian.admin.satellite.sbd.SbdDispatchService;
import org.rfcx.guardian.admin.satellite.sbd.SbdDispatchTimeoutService;
import org.rfcx.guardian.admin.satellite.sbd.SbdMessageDb;
import org.rfcx.guardian.admin.satellite.sbd.SbdUtils;
import org.rfcx.guardian.admin.satellite.swm.SwmDispatchTimeoutService;
import org.rfcx.guardian.admin.sms.SmsDispatchCycleService;
import org.rfcx.guardian.admin.sms.SmsMessageDb;
import org.rfcx.guardian.admin.device.android.control.ADBStateSetService;
import org.rfcx.guardian.admin.sms.SmsDispatchService;
import org.rfcx.guardian.admin.device.android.control.WifiHotspotStateSetService;
import org.rfcx.guardian.admin.device.android.system.DeviceDataTransferDb;
import org.rfcx.guardian.admin.device.android.system.DeviceSpaceDb;
import org.rfcx.guardian.admin.device.android.system.DeviceRebootDb;
import org.rfcx.guardian.admin.device.android.system.DeviceSensorDb;
import org.rfcx.guardian.admin.device.android.system.DeviceSystemDb;
import org.rfcx.guardian.admin.device.android.system.DeviceSystemService;
import org.rfcx.guardian.admin.device.android.system.DeviceUtils;
import org.rfcx.guardian.admin.status.AdminStatus;
import org.rfcx.guardian.admin.status.StatusCacheService;
import org.rfcx.guardian.admin.satellite.swm.SwmMessageDb;
import org.rfcx.guardian.admin.satellite.swm.SwmUtils;
import org.rfcx.guardian.i2c.DeviceI2cUtils;
import org.rfcx.guardian.uart.DeviceUartUtils;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.device.capture.DeviceCPU;
import org.rfcx.guardian.utility.device.telephony.DeviceMobileNetwork;
import org.rfcx.guardian.utility.device.telephony.DeviceMobilePhone;
import org.rfcx.guardian.utility.device.telephony.DeviceNetworkStats;
import org.rfcx.guardian.utility.device.control.DeviceSystemProperties;
import org.rfcx.guardian.gpio.DeviceGpioUtils;
import org.rfcx.guardian.utility.device.control.DeviceSystemSettings;
import org.rfcx.guardian.utility.device.control.DeviceWallpaper;
import org.rfcx.guardian.utility.device.hardware.DeviceHardware_OrangePi_3G_IOT;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.control.DeviceAirplaneMode;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

import org.rfcx.guardian.admin.device.android.capture.LogcatDb;
import org.rfcx.guardian.admin.device.android.capture.LogcatCaptureService;
import org.rfcx.guardian.admin.device.android.capture.ScreenShotDb;
import org.rfcx.guardian.admin.device.android.capture.ScreenShotCaptureService;
import org.rfcx.guardian.admin.device.android.capture.ScheduledLogcatCaptureService;
import org.rfcx.guardian.admin.device.android.capture.ScheduledScreenShotCaptureService;
import org.rfcx.guardian.admin.device.android.control.AirplaneModeToggleService;
import org.rfcx.guardian.admin.device.android.control.ScheduledRebootService;
import org.rfcx.guardian.admin.device.android.control.ClockSyncJobService;
import org.rfcx.guardian.admin.device.android.control.ForceRoleRelaunchService;
import org.rfcx.guardian.admin.device.android.control.RebootTriggerService;
import org.rfcx.guardian.admin.device.sentinel.DeviceSentinelService;
import org.rfcx.guardian.admin.device.sentinel.SentinelPowerDb;
import org.rfcx.guardian.admin.device.sentinel.SentinelPowerUtils;
import org.rfcx.guardian.admin.receiver.AirplaneModeReceiver;
import org.rfcx.guardian.admin.receiver.ConnectivityReceiver;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

public class RfcxGuardian extends Application {
	
	public String version;
	
	public static final String APP_ROLE = "Admin";

	private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");

	public RfcxGuardianIdentity rfcxGuardianIdentity = null;
	public RfcxPrefs rfcxPrefs = null;
	public RfcxSvc rfcxSvc = null;
	public AdminStatus rfcxStatus = null;
	
	public ScreenShotDb screenShotDb = null;
	public CameraCaptureDb cameraCaptureDb = null;
	public LogcatDb logcatDb = null;
	public SentinelPowerDb sentinelPowerDb = null;
	public SentinelSensorDb sentinelSensorDb = null;
	public DeviceSystemDb deviceSystemDb = null;
    public DeviceSensorDb deviceSensorDb = null;
    public DeviceRebootDb rebootDb = null;
    public DeviceDataTransferDb deviceDataTransferDb = null;
    public DeviceSpaceDb deviceSpaceDb = null;
    public SmsMessageDb smsMessageDb = null;
    public SbdMessageDb sbdMessageDb = null;
	public SwmMessageDb swmMessageDb = null;

	public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
	public DeviceAirplaneMode deviceAirplaneMode = new DeviceAirplaneMode(APP_ROLE);

	// Android Device Handlers
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
    public DeviceNetworkStats deviceNetworkStats = new DeviceNetworkStats(APP_ROLE);
    public DeviceCPU deviceCPU = new DeviceCPU(APP_ROLE);
    public DeviceUtils deviceUtils = null;
	public DeviceMobilePhone deviceMobilePhone = null;
	public DeviceMobileNetwork deviceMobileNetwork = new DeviceMobileNetwork(APP_ROLE);
	public DeviceSystemSettings deviceSystemSettings = new DeviceSystemSettings(APP_ROLE);
	public AssetUtils assetUtils = null;

	public DeviceI2cUtils deviceI2cUtils = new DeviceI2cUtils(APP_ROLE);
	public DeviceGpioUtils deviceGpioUtils = new DeviceGpioUtils(APP_ROLE);
	public DeviceUartUtils deviceUartUtils= new DeviceUartUtils(APP_ROLE);

	public SentinelPowerUtils sentinelPowerUtils = null;
	public SentinelAccelUtils sentinelAccelUtils = null;

	public SbdUtils sbdUtils = null;
	public SwmUtils swmUtils = null;

	// Receivers
	private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
	private final BroadcastReceiver airplaneModeReceiver = new AirplaneModeReceiver();
	private final ComponentName devAdminReceiver = null;
	
	public String[] RfcxCoreServices = 
			new String[] {
				DeviceSystemService.SERVICE_NAME,
				DeviceSentinelService.SERVICE_NAME,
				SmsDispatchCycleService.SERVICE_NAME,
				SbdDispatchCycleService.SERVICE_NAME
			};

	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxSvc = new RfcxSvc(this, APP_ROLE);
		this.rfcxStatus = new AdminStatus(this);

		this.version = RfcxRole.getRoleVersion(this, logTag);
		RfcxRole.writeVersionToFile(this, logTag, this.version);

		this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		this.registerReceiver(airplaneModeReceiver, new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));

		setDbHandlers();
		setServiceHandlers();

		this.deviceUtils = new DeviceUtils(this);
		this.sentinelPowerUtils = new SentinelPowerUtils(this);
		this.sentinelAccelUtils = new SentinelAccelUtils(this);
		SentinelUtils.setVerboseSentinelLogging(this);
		this.assetUtils = new AssetUtils(this);
		this.sbdUtils = new SbdUtils(this);
		this.swmUtils = new SwmUtils(this);

		// Hardware-specific hacks and modifications
		runHardwareSpecificModifications();

		// Initialize I2C Handler
		this.deviceI2cUtils.initializeOrReInitialize();

		initializeRoleServices();

		DateTimeUtils.setSystemTimezone(this.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_SYSTEM_TIMEZONE), this);

		DeviceSystemProperties.setVal("net.hostname", "rfcx-" + this.rfcxGuardianIdentity.getGuid());

//		this.devAdminReceiver = new ComponentName(this, this.devAdminReceiver.cla);
//		DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
//		if (mDPM.isAdminActive(this.devAdminReceiver)) {
//			mDPM.lockNow();
//		}

	}
	
	public void onTerminate() {
		super.onTerminate();

		this.unregisterReceiver(connectivityReceiver);
		this.unregisterReceiver(airplaneModeReceiver);
	}
	
	public void appResume() { }
	
	public void appPause() { }

	public ContentResolver getResolver() {
		return this.getApplicationContext().getContentResolver();
	}

	public boolean isGuardianRegistered() {
		return (this.rfcxGuardianIdentity.getAuthToken() != null);
	}
	
	public void initializeRoleServices() {
		
		if (!this.rfcxSvc.hasRun("OnLaunchServiceSequence")) {
			
			String[] runOnceOnlyOnLaunch = new String[] {
					ServiceMonitor.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
							+ "|" + ServiceMonitor.SERVICE_MONITOR_CYCLE_DURATION
							,
					ScheduledRebootService.SERVICE_NAME
							+ "|" + DateTimeUtils.nextOccurrenceOf(this.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.REBOOT_FORCED_DAILY_AT)).getTimeInMillis()
							+ "|" + ScheduledRebootService.SCHEDULED_REBOOT_CYCLE_DURATION
							,
					ScheduledAssetCleanupService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
							+ "|" + ( ScheduledAssetCleanupService.ASSET_CLEANUP_CYCLE_DURATION_MINUTES * 60 * 1000 )
							,
					ScheduledScreenShotCaptureService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:00:45").getTimeInMillis() // waits forty five seconds before running
							+ "|" + ( this.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.ADMIN_SCREENSHOT_CAPTURE_CYCLE) * 60 * 1000 )
							,
					ScheduledLogcatCaptureService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
							+ "|" + ( this.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.ADMIN_LOG_CAPTURE_CYCLE) * 60 * 1000 )
							,
					ScheduledCameraCaptureService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:04:00").getTimeInMillis() // waits four minutes before running
							+ "|" + ( this.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.ADMIN_CAMERA_CAPTURE_CYCLE) * 60 * 1000 )
							,
					SystemSettingsService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:00:04").getTimeInMillis() // waits a few seconds before running
							+ "|" + "norepeat"
							,
					WifiHotspotStateSetService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:00:08").getTimeInMillis() // waits a few seconds before running
							+ "|" + "norepeat"
							,
					ADBStateSetService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:00:12").getTimeInMillis() // waits a few seconds before running
							+ "|" + "norepeat"
					,
					AirplaneModeSetService.SERVICE_NAME
							+ "|" + DateTimeUtils.nowPlusThisLong("00:00:16").getTimeInMillis() // waits a few seconds before running
							+ "|" + "norepeat"
			};
			
			String[] onLaunchServices = new String[ RfcxCoreServices.length + runOnceOnlyOnLaunch.length ];
			System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
			System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
			this.rfcxSvc.triggerServiceSequence( "OnLaunchServiceSequence", onLaunchServices, false, 0);
		}
	}
	
	private void setDbHandlers() {
		
		this.sentinelPowerDb = new SentinelPowerDb(this, this.version);
		this.sentinelSensorDb = new SentinelSensorDb(this, this.version);
		this.screenShotDb = new ScreenShotDb(this, this.version);
		this.cameraCaptureDb = new CameraCaptureDb(this, this.version);
		this.logcatDb = new LogcatDb(this, this.version);
		this.deviceSystemDb = new DeviceSystemDb(this, this.version);
        this.deviceSensorDb = new DeviceSensorDb(this, this.version);
        this.rebootDb = new DeviceRebootDb(this, this.version);
        this.deviceDataTransferDb = new DeviceDataTransferDb(this, this.version);
        this.deviceSpaceDb = new DeviceSpaceDb(this, this.version);
        this.smsMessageDb = new SmsMessageDb(this, this.version);
        this.sbdMessageDb = new SbdMessageDb(this, this.version);
		this.swmMessageDb = new SwmMessageDb(this, this.version);
		this.deviceMobilePhone = new DeviceMobilePhone(this);
	}

	private void setServiceHandlers() {

		this.rfcxSvc.addService( ServiceMonitor.SERVICE_NAME, ServiceMonitor.class);
		this.rfcxSvc.addService( StatusCacheService.SERVICE_NAME, StatusCacheService.class);
		this.rfcxSvc.addService( ScheduledAssetCleanupService.SERVICE_NAME, ScheduledAssetCleanupService.class);

		this.rfcxSvc.addService( AirplaneModeToggleService.SERVICE_NAME, AirplaneModeToggleService.class);
		this.rfcxSvc.addService( AirplaneModeSetService.SERVICE_NAME, AirplaneModeSetService.class);

		this.rfcxSvc.addService( WifiHotspotStateSetService.SERVICE_NAME, WifiHotspotStateSetService.class);
		this.rfcxSvc.addService( ADBStateSetService.SERVICE_NAME, ADBStateSetService.class);
		this.rfcxSvc.addService( SystemSettingsService.SERVICE_NAME, SystemSettingsService.class);

        this.rfcxSvc.addService( SmsDispatchService.SERVICE_NAME, SmsDispatchService.class);
		this.rfcxSvc.addService( SmsDispatchCycleService.SERVICE_NAME, SmsDispatchCycleService.class);

		this.rfcxSvc.addService( SbdDispatchService.SERVICE_NAME, SbdDispatchService.class);
		this.rfcxSvc.addService( SbdDispatchCycleService.SERVICE_NAME, SbdDispatchCycleService.class);
		this.rfcxSvc.addService( SbdDispatchTimeoutService.SERVICE_NAME, SbdDispatchTimeoutService.class);

//		this.rfcxSvc.addService( SwmDispatchService.SERVICE_NAME, SwmDispatchService.class);
//		this.rfcxSvc.addService( SwmDispatchCycleService.SERVICE_NAME, SwmDispatchCycleService.class);
		this.rfcxSvc.addService( SwmDispatchTimeoutService.SERVICE_NAME, SwmDispatchTimeoutService.class);

		this.rfcxSvc.addService( ClockSyncJobService.SERVICE_NAME, ClockSyncJobService.class);
		this.rfcxSvc.addService( ScheduledClockSyncService.SERVICE_NAME, ScheduledClockSyncService.class);

		this.rfcxSvc.addService( ForceRoleRelaunchService.SERVICE_NAME, ForceRoleRelaunchService.class);

		this.rfcxSvc.addService( RebootTriggerService.SERVICE_NAME, RebootTriggerService.class);
		this.rfcxSvc.addService( ScheduledRebootService.SERVICE_NAME, ScheduledRebootService.class);

		this.rfcxSvc.addService( DeviceSystemService.SERVICE_NAME, DeviceSystemService.class);
		this.rfcxSvc.addService( DeviceSentinelService.SERVICE_NAME, DeviceSentinelService.class);

		this.rfcxSvc.addService( ScreenShotCaptureService.SERVICE_NAME, ScreenShotCaptureService.class);
		this.rfcxSvc.addService( ScheduledScreenShotCaptureService.SERVICE_NAME, ScheduledScreenShotCaptureService.class);

		this.rfcxSvc.addService( LogcatCaptureService.SERVICE_NAME, LogcatCaptureService.class);
		this.rfcxSvc.addService( ScheduledLogcatCaptureService.SERVICE_NAME, ScheduledLogcatCaptureService.class);

		this.rfcxSvc.addService( CameraCaptureService.SERVICE_NAME, CameraCaptureService.class);
		this.rfcxSvc.addService( ScheduledCameraCaptureService.SERVICE_NAME, ScheduledCameraCaptureService.class);

		this.rfcxSvc.addService("SSHServerControl", SSHServerControlService.class);

	}

	public void onPrefReSync(String prefKey) {

		if (prefKey != null) {

			if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_HOTSPOT) || prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_WIFI_HOTSPOT_PASSWORD) || prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_CONNECTION)) {
				rfcxSvc.triggerService(WifiHotspotStateSetService.SERVICE_NAME, false);
				rfcxSvc.triggerService(ADBStateSetService.SERVICE_NAME, false);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_TCP_ADB)) {
				rfcxSvc.triggerService(ADBStateSetService.SERVICE_NAME, false);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_AIRPLANE_MODE)) {
				rfcxSvc.triggerService(AirplaneModeSetService.SERVICE_NAME, false);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)) {
				this.rfcxStatus.setOrResetCacheExpirations(this.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_SYSTEM_TIMEZONE)) {
				DateTimeUtils.setSystemTimezone(this.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.ADMIN_SYSTEM_TIMEZONE), this);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.REBOOT_FORCED_DAILY_AT)) {
				Log.e(logTag, "Pref ReSync: ADD CODE FOR FORCING RESET OF SCHEDULED REBOOT");

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_VERBOSE_SENTINEL)) {
				SentinelUtils.setVerboseSentinelLogging(this);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_SSH_SERVER)) {
				rfcxSvc.triggerService("SSHServerControl", false);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_SYSTEM_SETTINGS_OVERRIDE)) {
				rfcxSvc.triggerService(SystemSettingsService.SERVICE_NAME, false);

			} else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_GEOPOSITION_CAPTURE) || prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_GEOPOSITION_CAPTURE_CYCLE)) {
				rfcxSvc.triggerService(DeviceSystemService.SERVICE_NAME, true);
			}
		}
	}

	private void runHardwareSpecificModifications() {

		if (DeviceHardware_OrangePi_3G_IOT.isDevice_OrangePi_3G_IOT()) {

			// Disable Sensor Listeners for sensors the don't exist on the OrangePi 3G-IoT
			this.deviceUtils.disableSensorListener("accel"); // accelerometer
			this.deviceUtils.disableSensorListener("light");  // light meter

			// Set Desktop Wallpaper to empty black
			DeviceWallpaper.setWallpaper(this, R.drawable.black);

			// Rename Device Hardware with /system/build.prop.
			// Only occurs once, on initial launch, and requires reboot if changes are made.
			DeviceHardware_OrangePi_3G_IOT.checkSetDeviceHardwareIdentification(this);

			// Load System settings
			this.deviceSystemSettings.loadActiveVals(DeviceHardware_OrangePi_3G_IOT.DEVICE_SYSTEM_SETTINGS);

			// Sets I2C interface
			this.deviceI2cUtils.setInterface(DeviceHardware_OrangePi_3G_IOT.DEVICE_I2C_INTERFACE);

			// Sets GPIO interface
			this.deviceGpioUtils.setGpioHandlerFilepath(DeviceHardware_OrangePi_3G_IOT.DEVICE_GPIO_HANDLER_FILEPATH);
			this.deviceGpioUtils.setupAddresses(DeviceHardware_OrangePi_3G_IOT.DEVICE_GPIO_MAP);

			// Sets Satellite interfaces
			this.sbdUtils.setupSbdUtils();
			this.swmUtils.setupSwmUtils();

		}

	}
    
}
