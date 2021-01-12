package org.rfcx.guardian.guardian;

import java.util.Map;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInUtils;
import org.rfcx.guardian.guardian.api.methods.command.ApiCommandUtils;
import org.rfcx.guardian.guardian.api.methods.download.AssetDownloadDb;
import org.rfcx.guardian.guardian.api.methods.download.AssetDownloadJobService;
import org.rfcx.guardian.guardian.api.methods.download.AssetDownloadUtils;
import org.rfcx.guardian.guardian.api.methods.ping.ApiPingJsonUtils;
import org.rfcx.guardian.guardian.api.methods.ping.ApiPingUtils;
import org.rfcx.guardian.guardian.api.methods.ping.SendApiPingService;
import org.rfcx.guardian.guardian.api.methods.segment.ApiSegmentUtils;
import org.rfcx.guardian.guardian.api.protocols.ApiRestUtils;
import org.rfcx.guardian.guardian.api.protocols.ApiSbdUtils;
import org.rfcx.guardian.guardian.api.protocols.ApiSmsUtils;
import org.rfcx.guardian.guardian.asset.AssetLibraryDb;
import org.rfcx.guardian.guardian.asset.AssetLibraryUtils;
import org.rfcx.guardian.guardian.asset.AssetUtils;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInHealthUtils;
import org.rfcx.guardian.guardian.asset.AudioDetectionDb;
import org.rfcx.guardian.guardian.asset.MetaSnapshotService;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInJsonUtils;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInStatsDb;
import org.rfcx.guardian.guardian.api.methods.ping.ScheduledApiPingService;
import org.rfcx.guardian.guardian.api.methods.segment.ApiSegmentDb;
import org.rfcx.guardian.guardian.asset.ScheduledAssetCleanupService;
import org.rfcx.guardian.guardian.asset.AudioClassifierDb;
import org.rfcx.guardian.guardian.audio.classify.AudioClassifyDb;
import org.rfcx.guardian.guardian.audio.classify.AudioClassifyPrepareService;
import org.rfcx.guardian.guardian.audio.classify.AudioClassifyUtils;
import org.rfcx.guardian.guardian.audio.encode.AudioVaultDb;
import org.rfcx.guardian.guardian.audio.playback.AudioPlaybackDb;
import org.rfcx.guardian.guardian.audio.playback.AudioPlaybackJobService;
import org.rfcx.guardian.guardian.instructions.InstructionsCycleService;
import org.rfcx.guardian.guardian.instructions.InstructionsDb;
import org.rfcx.guardian.guardian.instructions.InstructionsExecutionService;
import org.rfcx.guardian.guardian.instructions.InstructionsUtils;
import org.rfcx.guardian.guardian.socket.WifiCommunicationService;
import org.rfcx.guardian.guardian.socket.WifiCommunicationUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
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
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.util.Log;
import org.rfcx.guardian.guardian.asset.AssetExchangeLogDb;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInDb;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInJobService;
import org.rfcx.guardian.guardian.asset.MetaDb;
import org.rfcx.guardian.guardian.api.protocols.ApiMqttUtils;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInQueueService;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInArchiveService;
import org.rfcx.guardian.guardian.api.methods.checkin.ApiCheckInArchiveDb;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureService;
import org.rfcx.guardian.guardian.audio.capture.AudioCaptureUtils;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeDb;
import org.rfcx.guardian.guardian.audio.encode.AudioEncodeJobService;
import org.rfcx.guardian.guardian.audio.capture.AudioQueuePostProcessingService;
import org.rfcx.guardian.guardian.api.methods.clocksync.ClockSyncJobService;
import org.rfcx.guardian.guardian.device.android.DeviceSystemDb;
import org.rfcx.guardian.guardian.api.methods.clocksync.ScheduledClockSyncService;
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
    public AudioVaultDb audioVaultDb = null;
    public ApiCheckInDb apiCheckInDb = null;
    public MetaDb metaDb = null;
    public ApiCheckInStatsDb apiCheckInStatsDb = null;
    public AssetExchangeLogDb assetExchangeLogDb = null;
    public ApiCheckInArchiveDb apiCheckInArchiveDb = null;
    public ApiSegmentDb apiSegmentDb = null;
    public AssetDownloadDb assetDownloadDb = null;
    public InstructionsDb instructionsDb = null;
    public DeviceSystemDb deviceSystemDb = null;

    public AudioClassifyDb audioClassifyDb = null;
    public AudioDetectionDb audioDetectionDb = null;
    public AudioClassifierDb audioClassifierDb = null;
    public AudioPlaybackDb audioPlaybackDb = null;
    public AssetLibraryDb assetLibraryDb = null;

    // Receivers
    private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

    // Android Device Handlers
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);

    // Misc
    public AudioCaptureUtils audioCaptureUtils = null;
    public AudioClassifyUtils audioClassifyUtils = null;
    public ApiMqttUtils apiMqttUtils = null;
    public ApiSmsUtils apiSmsUtils = null;
    public ApiRestUtils apiRestUtils = null;
    public AssetDownloadUtils assetDownloadUtils = null;
    public AssetLibraryUtils assetLibraryUtils = null;
    public ApiSbdUtils apiSbdUtils = null;
    public ApiCheckInUtils apiCheckInUtils = null;
    public ApiCheckInJsonUtils apiCheckInJsonUtils = null;
    public ApiPingJsonUtils apiPingJsonUtils = null;
    public ApiPingUtils apiPingUtils = null;
    public ApiCommandUtils apiCommandUtils = null;
    public ApiSegmentUtils apiSegmentUtils = null;
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
                    AudioEncodeJobService.SERVICE_NAME,
                    AudioClassifyPrepareService.SERVICE_NAME,
                    InstructionsCycleService.SERVICE_NAME
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
        this.audioClassifyUtils = new AudioClassifyUtils(this);
        this.apiMqttUtils = new ApiMqttUtils(this);
        this.apiSmsUtils = new ApiSmsUtils(this);
        this.apiRestUtils = new ApiRestUtils(this);
        this.apiSbdUtils = new ApiSbdUtils(this);
        this.apiCheckInUtils = new ApiCheckInUtils(this);
        this.assetDownloadUtils = new AssetDownloadUtils(this);
        this.assetLibraryUtils = new AssetLibraryUtils(this);
        this.apiCheckInJsonUtils = new ApiCheckInJsonUtils(this);
        this.apiPingJsonUtils = new ApiPingJsonUtils(this);
        this.apiPingUtils = new ApiPingUtils(this);
        this.apiCommandUtils = new ApiCommandUtils(this);
        this.apiSegmentUtils = new ApiSegmentUtils(this);
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

    public ContentResolver getResolver() {
        return this.getApplicationContext().getContentResolver();
    }

    public void saveGuardianRegistration(String regJsonStr) {
        try {
            JSONObject regJson = new JSONObject(regJsonStr);
            if (regJson.has("guid") && regJson.has("token")) {
                this.rfcxGuardianIdentity.setAuthToken(regJson.getString("token"));
                this.rfcxGuardianIdentity.setKeystorePassPhrase(regJson.getString("keystore_passphrase"));
                if (regJson.has("pin_code")) { this.rfcxGuardianIdentity.setPinCode(regJson.getString("pin_code")); }
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
        this.rfcxGuardianIdentity.unSetIdentityValue("pin_code");
    }

    public boolean isGuardianRegistered() {
        return (this.rfcxGuardianIdentity.getAuthToken() != null);
    }

    public void initializeRoleServices() {

        if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {

            String[] runOnceOnlyOnLaunch = new String[]{
                    "ServiceMonitor"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
                            + "|" + ServiceMonitor.SERVICE_MONITOR_CYCLE_DURATION
                            ,
                    "ScheduledAssetCleanup"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:03:00").getTimeInMillis() // waits three minutes before running
                            + "|" + ( ScheduledAssetCleanupService.ASSET_CLEANUP_CYCLE_DURATION_MINUTES * 60 * 1000 )
                            ,
                    "ScheduledClockSync"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:05:00").getTimeInMillis() // waits five minutes before running
                            + "|" + ( this.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.API_CLOCK_SYNC_CYCLE_DURATION) * 60 * 1000 )
                            ,
                    ScheduledApiPingService.SERVICE_NAME
                            + "|" + DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits two minutes before running
                            + "|" + ( this.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.API_PING_CYCLE_DURATION) * 60 * 1000 )
                            ,
                    "WifiCommunication"
                            + "|" + DateTimeUtils.nowPlusThisLong("00:01:00").getTimeInMillis() // waits one minutes before running
                            + "|" + "norepeat"
            };

            String[] onLaunchServices = new String[RfcxCoreServices.length + runOnceOnlyOnLaunch.length];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxServiceHandler.triggerServiceSequence( "OnLaunchServiceSequence", onLaunchServices, false, 0);
        }
    }

    private void setDbHandlers() {

        this.audioEncodeDb = new AudioEncodeDb(this, this.version);
        this.audioPlaybackDb = new AudioPlaybackDb(this, this.version);
        this.assetLibraryDb = new AssetLibraryDb(this, this.version);
        this.audioVaultDb = new AudioVaultDb(this, this.version);
        this.apiCheckInDb = new ApiCheckInDb(this, this.version);
        this.metaDb = new MetaDb(this, this.version);
        this.apiCheckInStatsDb = new ApiCheckInStatsDb(this, this.version);
        this.assetExchangeLogDb = new AssetExchangeLogDb(this, this.version);
        this.apiCheckInArchiveDb = new ApiCheckInArchiveDb(this, this.version);
        this.apiSegmentDb = new ApiSegmentDb(this, this.version);
        this.assetDownloadDb = new AssetDownloadDb(this, this.version);
        this.instructionsDb = new InstructionsDb(this, this.version);
        this.deviceSystemDb = new DeviceSystemDb(this, this.version);
        this.audioClassifyDb = new AudioClassifyDb(this, this.version);
        this.audioDetectionDb = new AudioDetectionDb(this, this.version);
        this.audioClassifierDb = new AudioClassifierDb(this, this.version);

    }

    private void setServiceHandlers() {

        this.rfcxServiceHandler.addService( ServiceMonitor.SERVICE_NAME, ServiceMonitor.class);
        this.rfcxServiceHandler.addService( ScheduledAssetCleanupService.SERVICE_NAME, ScheduledAssetCleanupService.class);

        this.rfcxServiceHandler.addService( AudioCaptureService.SERVICE_NAME, AudioCaptureService.class);
        this.rfcxServiceHandler.addService( AudioQueuePostProcessingService.SERVICE_NAME, AudioQueuePostProcessingService.class);
        this.rfcxServiceHandler.addService( AudioEncodeJobService.SERVICE_NAME, AudioEncodeJobService.class);
        this.rfcxServiceHandler.addService( AudioClassifyPrepareService.SERVICE_NAME, AudioClassifyPrepareService.class);
        this.rfcxServiceHandler.addService( AudioPlaybackJobService.SERVICE_NAME, AudioPlaybackJobService.class);

        this.rfcxServiceHandler.addService( ApiCheckInQueueService.SERVICE_NAME, ApiCheckInQueueService.class);
        this.rfcxServiceHandler.addService( ApiCheckInJobService.SERVICE_NAME, ApiCheckInJobService.class);

        this.rfcxServiceHandler.addService( ScheduledApiPingService.SERVICE_NAME, ScheduledApiPingService.class);
        this.rfcxServiceHandler.addService( SendApiPingService.SERVICE_NAME, SendApiPingService.class);

        this.rfcxServiceHandler.addService( ClockSyncJobService.SERVICE_NAME, ClockSyncJobService.class);
        this.rfcxServiceHandler.addService( ScheduledClockSyncService.SERVICE_NAME, ScheduledClockSyncService.class);

        this.rfcxServiceHandler.addService( ApiCheckInArchiveService.SERVICE_NAME, ApiCheckInArchiveService.class);
        this.rfcxServiceHandler.addService( MetaSnapshotService.SERVICE_NAME, MetaSnapshotService.class);

        this.rfcxServiceHandler.addService( AssetDownloadJobService.SERVICE_NAME, AssetDownloadJobService.class);

        this.rfcxServiceHandler.addService( InstructionsCycleService.SERVICE_NAME, InstructionsCycleService.class);
        this.rfcxServiceHandler.addService( InstructionsExecutionService.SERVICE_NAME, InstructionsExecutionService.class);

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

    public void reSyncIdentityAcrossRoles() {
        for (String roleName : RfcxRole.ALL_ROLES) {
            if (!roleName.equalsIgnoreCase(APP_ROLE)) {
                this.rfcxGuardianIdentity.reSyncIdentityInExternalRoleViaContentProvider(roleName.toLowerCase(), this);
            }
        }
    }

    public void onPrefReSync(String prefKey) {

        if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)) {
            this.apiMqttUtils.getSetCheckInPublishTimeOutLength();

        } else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ADMIN_ENABLE_WIFI_SOCKET)) {
            this.rfcxServiceHandler.triggerService("WifiCommunication", false);

        } else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.CHECKIN_FAILURE_THRESHOLDS)) {
            this.apiMqttUtils.initializeFailedCheckInThresholds();

        } else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ENABLE_CHECKIN_PUBLISH)) {
            this.apiMqttUtils.initializeFailedCheckInThresholds();
            this.rfcxServiceHandler.triggerService( ApiCheckInJobService.SERVICE_NAME, false);

        } else if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.ENABLE_CUTOFFS_SAMPLING_RATIO)) {
            this.audioCaptureUtils.samplingRatioIteration = 0;

        }
    }

}
