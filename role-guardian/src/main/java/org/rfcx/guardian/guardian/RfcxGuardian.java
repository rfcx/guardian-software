package org.rfcx.guardian.guardian;

import java.util.Map;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.api.asset.AssetUtils;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInHealthUtils;
import org.rfcx.guardian.guardian.api.asset.MetaSnapshotService;
import org.rfcx.guardian.guardian.api.mqtt.ApiJsonUtils;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInStatsDb;
import org.rfcx.guardian.guardian.api.mqtt.ScheduledApiPingService;
import org.rfcx.guardian.guardian.instructions.InstructionsCycleService;
import org.rfcx.guardian.guardian.instructions.InstructionsDb;
import org.rfcx.guardian.guardian.instructions.InstructionsExecutionService;
import org.rfcx.guardian.guardian.instructions.InstructionsUtils;
import org.rfcx.guardian.guardian.socket.WifiCommunicationService;
import org.rfcx.guardian.guardian.socket.WifiCommunicationUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.capture.DeviceMobilePhone;
import org.rfcx.guardian.utility.device.control.DeviceControlUtils;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
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
import android.preference.PreferenceManager;
import android.util.Log;
import org.rfcx.guardian.guardian.api.asset.AssetExchangeLogDb;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInDb;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInJobService;
import org.rfcx.guardian.guardian.api.asset.MetaDb;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInUtils;
import org.rfcx.guardian.guardian.api.mqtt.ApiCheckInQueueService;
import org.rfcx.guardian.guardian.archive.ApiCheckInArchiveService;
import org.rfcx.guardian.guardian.archive.ArchiveDb;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureService;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeDb;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.guardian.audio.encode.AudioQueueEncodeService;
import org.rfcx.guardian.guardian.api.sntp.SntpSyncJobService;
import org.rfcx.guardian.guardian.device.android.DeviceSystemDb;
import org.rfcx.guardian.guardian.api.sntp.ScheduledSntpSyncService;
import org.rfcx.guardian.guardian.receiver.ConnectivityReceiver;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

    public String version;

    public static final String APP_ROLE = "Guardian";

    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");

    public RfcxGuardianIdentity rfcxGuardianIdentity = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxServiceHandler rfcxServiceHandler = null;

    public SharedPreferences sharedPrefs = null;

    // Database Handlers
    public AudioEncodeDb audioEncodeDb = null;
    public ApiCheckInDb apiCheckInDb = null;
    public MetaDb metaDb = null;
    public ApiCheckInStatsDb apiCheckInStatsDb = null;
    public AssetExchangeLogDb assetExchangeLogDb = null;
    public ArchiveDb archiveDb = null;
    public InstructionsDb instructionsDb = null;
    public DeviceSystemDb deviceSystemDb = null;

    // Receivers
    private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

    // Android Device Handlers
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);

    // Misc
    public AudioCaptureUtils audioCaptureUtils = null;
    public ApiCheckInUtils apiCheckInUtils = null;
    public ApiJsonUtils apiJsonUtils = null;
    public ApiCheckInHealthUtils apiCheckInHealthUtils = null;
    public AssetUtils assetUtils = null;
    public InstructionsUtils instructionsUtils = null;
    public WifiCommunicationUtils wifiCommunicationUtils = null;
    public DeviceMobilePhone deviceMobilePhone = null;
    public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);

    public DeviceControlUtils deviceControlUtils = new DeviceControlUtils(APP_ROLE);

    public String[] RfcxCoreServices =
            new String[]{
                    "AudioCapture",
                    "ApiCheckInJob",
                    "AudioEncodeJob",
                    "InstructionsCycle"
            };

    @Override
    public void onCreate() {

        super.onCreate();

        this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
        this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
        this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);

        this.version = RfcxRole.getRoleVersion(this, logTag);
        RfcxRole.writeVersionToFile(this, logTag, this.version);

        this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        PreferenceManager.setDefaultValues(this, R.xml.prefs, true);
        this.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        this.syncSharedPrefs();

        setDbHandlers();
        setServiceHandlers();

        this.audioCaptureUtils = new AudioCaptureUtils(this);
        this.apiCheckInUtils = new ApiCheckInUtils(this);
        this.apiJsonUtils = new ApiJsonUtils(this);
        this.apiCheckInHealthUtils = new ApiCheckInHealthUtils(this);
        this.assetUtils = new AssetUtils(this);
        this.instructionsUtils = new InstructionsUtils(this);
        this.wifiCommunicationUtils = new WifiCommunicationUtils(this);
        this.deviceMobilePhone = new DeviceMobilePhone(this);

    //    reSyncIdentityAcrossRoles();
        reSyncPrefAcrossRoles("all");

        initializeRoleServices();

    }

    public void onTerminate() {
        super.onTerminate();

        this.unregisterReceiver(connectivityReceiver);
    }

    public void appResume() {
        syncSharedPrefs();
    }

    public void appPause() { }


    public void saveGuardianRegistration(String regJsonStr) {
        try {
            JSONObject regJson = new JSONObject(regJsonStr);
            if (regJson.has("guid") && regJson.has("token")) {
                this.rfcxGuardianIdentity.setAuthToken(regJson.getString("token"));
                this.rfcxGuardianIdentity.setKeystorePassPhrase(regJson.getString("keystore_passphrase"));
                if (regJson.has("api_mqtt_host")) { setSharedPref("api_mqtt_host", regJson.getString("api_mqtt_host")); }
                if (regJson.has("api_sms_address")) { setSharedPref("api_sms_address", regJson.getString("api_sms_address")); }
            } else {
                Log.e(logTag, "doesn't have token or guid");
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public void clearRegistration() {
        this.rfcxGuardianIdentity.unSetIdentityValue("token");
        this.rfcxGuardianIdentity.unSetIdentityValue("keystore_passphrase");
    }

    public boolean isGuardianRegistered() {
        return (this.rfcxGuardianIdentity.getAuthToken() != null);
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
                            + "|" + ( this.rfcxPrefs.getPrefAsLong("api_sntp_cycle_duration") * 60 * 1000 )
                            ,
                    "ScheduledApiPing"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
                            + "|" + ( this.rfcxPrefs.getPrefAsLong("api_ping_cycle_duration") * 60 * 1000 )
                            ,
                    "WifiCommunication"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:01:00").getTimeInMillis() // waits one minutes before running
                            + "|" + "norepeat"
            };

            String[] onLaunchServices = new String[RfcxCoreServices.length + runOnceOnlyOnLaunch.length];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, false, 0);
        }
    }

    private void setDbHandlers() {

        this.audioEncodeDb = new AudioEncodeDb(this, this.version);
        this.apiCheckInDb = new ApiCheckInDb(this, this.version);
        this.metaDb = new MetaDb(this, this.version);
        this.apiCheckInStatsDb = new ApiCheckInStatsDb(this, this.version);
        this.assetExchangeLogDb = new AssetExchangeLogDb(this, this.version);
        this.archiveDb = new ArchiveDb(this, this.version);
        this.instructionsDb = new InstructionsDb(this, this.version);
        this.deviceSystemDb = new DeviceSystemDb(this, this.version);

    }

    private void setServiceHandlers() {

        this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitor.class);
        this.rfcxServiceHandler.addService("AudioCapture", AudioCaptureService.class);

        this.rfcxServiceHandler.addService("AudioQueueEncode", AudioQueueEncodeService.class);
        this.rfcxServiceHandler.addService("AudioEncodeJob", AudioEncodeJobService.class);

        this.rfcxServiceHandler.addService("ApiCheckInQueue", ApiCheckInQueueService.class);
        this.rfcxServiceHandler.addService("ApiCheckInJob", ApiCheckInJobService.class);

        this.rfcxServiceHandler.addService("ScheduledApiPing", ScheduledApiPingService.class);

        this.rfcxServiceHandler.addService("SntpSyncJob", SntpSyncJobService.class);
        this.rfcxServiceHandler.addService("ScheduledSntpSync", ScheduledSntpSyncService.class);

        this.rfcxServiceHandler.addService("ApiCheckInArchive", ApiCheckInArchiveService.class);
        this.rfcxServiceHandler.addService("MetaSnapshot", MetaSnapshotService.class);

        this.rfcxServiceHandler.addService("InstructionsCycle", InstructionsCycleService.class);
        this.rfcxServiceHandler.addService("InstructionsExecution", InstructionsExecutionService.class);

        this.rfcxServiceHandler.addService("WifiCommunication", WifiCommunicationService.class);
    }

    @Override
    public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefKey) {
        Log.v(logTag, "Pref Changed: '" + prefKey + "' = " + this.sharedPrefs.getString(prefKey, null));
        syncSharedPrefs();
        reSyncPrefAcrossRoles(prefKey);
        onPrefReSync(prefKey);
    }

    private void syncSharedPrefs() {
        for (Map.Entry<String, ?> pref : this.sharedPrefs.getAll().entrySet()) {
            this.rfcxPrefs.setPref(pref.getKey(), pref.getValue().toString());
        }
    }

    private void reSyncPrefAcrossRoles(String prefKey) {
        for (String roleName : RfcxRole.ALL_ROLES) {
            if (!roleName.equalsIgnoreCase(APP_ROLE)) {
                this.rfcxPrefs.reSyncPrefInExternalRoleViaContentProvider(roleName.toLowerCase(), prefKey, this);
            }
        }
    }

    // setSharedPref is currently the preferred method for updating pref values, universally, across this role (and all roles, by contingency).
    public boolean setSharedPref(String prefKey, String prefValue) {
        return this.sharedPrefs.edit().putString(prefKey, prefValue).commit();
    }

    private void reSyncIdentityAcrossRoles() {
        for (String roleName : RfcxRole.ALL_ROLES) {
            if (!roleName.equalsIgnoreCase(APP_ROLE)) {
                this.rfcxGuardianIdentity.reSyncIdentityInExternalRoleViaContentProvider(roleName.toLowerCase(), this);
            }
        }
    }

    public void onPrefReSync(String prefKey) {

        if (prefKey.equalsIgnoreCase("audio_cycle_duration")) {
            this.apiCheckInUtils.getSetCheckInPublishTimeOutLength();

        } else if (prefKey.equalsIgnoreCase("admin_enable_wifi_socket")) {
            this.rfcxServiceHandler.triggerService("WifiCommunication", false);

        } else if (prefKey.equalsIgnoreCase("checkin_failure_thresholds")) {
            this.apiCheckInUtils.initializeFailedCheckInThresholds();

        } else if (prefKey.equalsIgnoreCase("enable_checkin_publish")) {
            this.apiCheckInUtils.initializeFailedCheckInThresholds();

        } else if (prefKey.equalsIgnoreCase("enable_cutoffs_sampling_ratio")) {
            this.audioCaptureUtils.samplingRatioIteration = 0;

        }




    }

}
