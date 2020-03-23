package org.rfcx.guardian.guardian;

import java.io.File;
import java.util.Map;

import android.content.Context;
import android.location.LocationManager;

import org.rfcx.guardian.guardian.utils.CheckAppPermissionUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceCPU;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.DeviceMobilePhone;
import org.rfcx.guardian.utility.device.DeviceNetworkStats;
import org.rfcx.guardian.utility.device.control.DeviceControlUtils;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceGuid;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import org.rfcx.guardian.guardian.api.ApiAssetExchangeLogDb;
import org.rfcx.guardian.guardian.api.ApiCheckInDb;
import org.rfcx.guardian.guardian.api.ApiCheckInJobService;
import org.rfcx.guardian.guardian.api.ApiCheckInMetaDb;
import org.rfcx.guardian.guardian.api.ApiCheckInUtils;
import org.rfcx.guardian.guardian.api.ApiQueueCheckInService;
import org.rfcx.guardian.guardian.archive.ApiCheckInArchiveService;
import org.rfcx.guardian.guardian.archive.ArchiveDb;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureService;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeDb;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.guardian.audio.encode.AudioQueueEncodeService;
import org.rfcx.guardian.guardian.camera.capture.PhotoCaptureJobService;
import org.rfcx.guardian.guardian.device.android.SntpSyncJobService;
import org.rfcx.guardian.guardian.device.android.DeviceDataTransferDb;
import org.rfcx.guardian.guardian.device.android.DeviceDiskDb;
import org.rfcx.guardian.guardian.device.android.DeviceRebootDb;
import org.rfcx.guardian.guardian.device.android.DeviceSensorDb;
import org.rfcx.guardian.guardian.device.android.DeviceSystemDb;
import org.rfcx.guardian.guardian.device.android.DeviceSystemService;
import org.rfcx.guardian.guardian.device.android.DeviceUtils;
import org.rfcx.guardian.guardian.device.android.ScheduledSntpSyncService;
import org.rfcx.guardian.guardian.receiver.ConnectivityReceiver;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

    public String version;

    public static final String APP_ROLE = "Guardian";

    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, RfcxGuardian.class);

    public RfcxDeviceGuid rfcxDeviceGuid = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxServiceHandler rfcxServiceHandler = null;

    public SharedPreferences sharedPrefs = null;

    // Database Handlers
    public AudioEncodeDb audioEncodeDb = null;
    public ApiCheckInDb apiCheckInDb = null;
    public ApiCheckInMetaDb apiCheckInMetaDb = null;
    public ApiAssetExchangeLogDb apiAssetExchangeLogDb = null;
    public DeviceSystemDb deviceSystemDb = null;
    public DeviceSensorDb deviceSensorDb = null;
    public DeviceRebootDb rebootDb = null;
    public DeviceDataTransferDb deviceDataTransferDb = null;
    public DeviceDiskDb deviceDiskDb = null;
    public ArchiveDb archiveDb = null;

    // Receivers
    private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

    // Android Device Handlers
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
    public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);
    public DeviceNetworkStats deviceNetworkStats = new DeviceNetworkStats(APP_ROLE);
    public DeviceCPU deviceCPU = new DeviceCPU(APP_ROLE);

    // Misc
    public AudioCaptureUtils audioCaptureUtils = null;
    public ApiCheckInUtils apiCheckInUtils = null;
    public DeviceUtils deviceUtils = null;
    public DeviceMobilePhone deviceMobilePhone = null;

    public DeviceControlUtils deviceControlUtils = new DeviceControlUtils(APP_ROLE);

    public PowerManager.WakeLock wakelock;

    public String[] RfcxCoreServices =
            new String[]{
                    "AudioCapture",
                    "DeviceSystem",
                    "ApiCheckInJob",
                    "AudioEncodeJob"
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

        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.syncSharedPrefs();

//		setSharedPref("enable_cutoffs_schedule_off_hours", "true");
//		setSharedPref("audio_schedule_off_hours", "19:00-23:45,00:05-05:55");
        setSharedPref("audio_battery_cutoff", "10");

        setDbHandlers();
        setServiceHandlers();

        this.audioCaptureUtils = new AudioCaptureUtils(getApplicationContext());
        this.apiCheckInUtils = new ApiCheckInUtils(getApplicationContext());
        this.deviceUtils = new DeviceUtils(getApplicationContext());
        this.deviceMobilePhone = new DeviceMobilePhone(getApplicationContext());

        startAllServices();
    }

    public void onTerminate() {
        super.onTerminate();

        this.unregisterReceiver(connectivityReceiver);
    }

    public void appResume() {
        syncSharedPrefs();
    }

    public void appPause() {

    }

    public void startAllServices() {
        if (isRequirementPassed()) {
            if(!getRecordingState()){
                initializeRoleServices();
            }
        } else {
            this.rfcxServiceHandler.stopAllServices();
        }
    }

    public boolean isRequirementPassed() {
        return isGuidExisted() && isLocationEnabled();
    }

    private Boolean isGuidExisted() {
        String directoryPath = getBaseContext().getFilesDir().toString() + "/txt/";
        File txtFile = new File(directoryPath + "/guardian_guid.txt");
        return txtFile.exists();
    }

    public void initializeRoleServices() {

        if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {

            String[] runOnceOnlyOnLaunch = new String[]{
                    "ServiceMonitor"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
                            + "|" + ServiceMonitor.SERVICE_MONITOR_CYCLE_DURATION
                    ,
                    "ScheduledSntpSync"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:05:00").getTimeInMillis() // waits five minutes before running
                            + "|" + ScheduledSntpSyncService.SCHEDULED_SNTP_SYNC_CYCLE_DURATION
            };

            String[] onLaunchServices = new String[RfcxCoreServices.length + runOnceOnlyOnLaunch.length];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
        }
    }

    public Boolean getRecordingState() {
        return rfcxServiceHandler.isRunning("AudioCapture");
    }

    public Boolean isLocationEnabled () {
        if (CheckAppPermissionUtils.INSTANCE.checkLocationPermission(getApplicationContext())) {
            return false;
        }
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void setDbHandlers() {

        this.audioEncodeDb = new AudioEncodeDb(this, this.version);
        this.apiCheckInDb = new ApiCheckInDb(this, this.version);
        this.apiCheckInMetaDb = new ApiCheckInMetaDb(this, this.version);
        this.apiAssetExchangeLogDb = new ApiAssetExchangeLogDb(this, this.version);
        this.deviceSystemDb = new DeviceSystemDb(this, this.version);
        this.deviceSensorDb = new DeviceSensorDb(this, this.version);
        this.rebootDb = new DeviceRebootDb(this, this.version);
        this.deviceDataTransferDb = new DeviceDataTransferDb(this, this.version);
        this.deviceDiskDb = new DeviceDiskDb(this, this.version);
        this.archiveDb = new ArchiveDb(this, this.version);

    }

    private void setServiceHandlers() {

        this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
        this.rfcxServiceHandler.addService("AudioCapture", AudioCaptureService.class);
        this.rfcxServiceHandler.addService("AudioQueueEncode", AudioQueueEncodeService.class);
        this.rfcxServiceHandler.addService("AudioEncodeJob", AudioEncodeJobService.class);
        this.rfcxServiceHandler.addService("PhotoCaptureJob", PhotoCaptureJobService.class);
        this.rfcxServiceHandler.addService("DeviceSystem", DeviceSystemService.class);
        this.rfcxServiceHandler.addService("ApiQueueCheckIn", ApiQueueCheckInService.class);
        this.rfcxServiceHandler.addService("ApiCheckInJob", ApiCheckInJobService.class);
        this.rfcxServiceHandler.addService("ApiCheckInArchive", ApiCheckInArchiveService.class);
        this.rfcxServiceHandler.addService("SntpSyncJob", SntpSyncJobService.class);
        this.rfcxServiceHandler.addService("ScheduledSntpSync", ScheduledSntpSyncService.class);

    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
        Log.d(logTag, "Pref changed: " + prefKey + " = " + this.sharedPrefs.getString(prefKey, null));
        syncSharedPrefs();
        this.rfcxPrefs.reSyncPrefInExternalRoleViaContentProvider("admin", prefKey, this);
    }

    private void syncSharedPrefs() {
        for (Map.Entry<String, ?> pref : this.sharedPrefs.getAll().entrySet()) {
            this.rfcxPrefs.setPref(pref.getKey(), pref.getValue().toString());
        }
    }

    public boolean setSharedPref(String prefKey, String prefValue) {
        return this.sharedPrefs.edit().putString(prefKey, prefValue).commit();
    }


}
