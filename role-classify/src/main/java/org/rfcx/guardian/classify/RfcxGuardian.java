package org.rfcx.guardian.classify;

import android.app.Application;
import android.content.ContentResolver;

import org.rfcx.guardian.classify.service.AudioClassifyJobService;
import org.rfcx.guardian.classify.service.AudioDetectionSendService;
import org.rfcx.guardian.classify.status.ClassifyStatus;
import org.rfcx.guardian.classify.utils.AudioClassifyDb;
import org.rfcx.guardian.classify.utils.AudioClassifyModelUtils;
import org.rfcx.guardian.classify.utils.AudioClassifyUtils;
import org.rfcx.guardian.classify.utils.AudioDetectionDb;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class RfcxGuardian extends Application {

    public static final String APP_ROLE = "Classify";
    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");
    public String version;
    public RfcxGuardianIdentity rfcxGuardianIdentity = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxSvc rfcxSvc = null;
    public ClassifyStatus rfcxStatus = null;

    public AudioClassifyModelUtils audioClassifyModelUtils = null;
    public AudioClassifyUtils audioClassifyUtils = null;

    // Database Handlers
    public AudioClassifyDb audioClassifyDb = null;
    public AudioDetectionDb audioDetectionDb = null;

    // for checking battery level
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);

    public String[] RfcxCoreServices =
            new String[]{
                    AudioClassifyJobService.SERVICE_NAME,
                    AudioDetectionSendService.SERVICE_NAME
            };

    @Override
    public void onCreate() {

        super.onCreate();

        this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
        this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
        this.rfcxSvc = new RfcxSvc(this, APP_ROLE);
        this.rfcxStatus = new ClassifyStatus(this);

        this.version = RfcxRole.getRoleVersion(this, logTag);
        RfcxRole.writeVersionToFile(this, logTag, this.version);

        this.audioClassifyModelUtils = new AudioClassifyModelUtils(this);
        this.audioClassifyUtils = new AudioClassifyUtils(this);

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

    public ContentResolver getResolver() {
        return this.getApplicationContext().getContentResolver();
    }

    public void initializeRoleServices() {

        if (!this.rfcxSvc.hasRun("OnLaunchServiceSequence")) {

            String[] runOnceOnlyOnLaunch = new String[]{
//                    ApiUpdateRequestTrigger.SERVICE_NAME
//                            +"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits 2 minutes before running
//                            +"|"+ ( ( 2 * ApiUpdateRequestUtils.minimumAllowedIntervalBetweenUpdateRequests) * ( 60 * 1000 ) ) // repeats hourly
            };

            String[] onLaunchServices = new String[RfcxCoreServices.length + runOnceOnlyOnLaunch.length];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxSvc.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
        }

    }

    private void setDbHandlers() {

        this.audioClassifyDb = new AudioClassifyDb(this, this.version);
        this.audioDetectionDb = new AudioDetectionDb(this, this.version);
    }

    private void setServiceHandlers() {

        this.rfcxSvc.addService(ServiceMonitor.SERVICE_NAME, ServiceMonitor.class);
        this.rfcxSvc.addService(AudioClassifyJobService.SERVICE_NAME, AudioClassifyJobService.class);
        this.rfcxSvc.addService(AudioDetectionSendService.SERVICE_NAME, AudioDetectionSendService.class);

    }

    public void onPrefReSync(String prefKey) {

        if (prefKey != null) {

            if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)) {
                this.rfcxStatus.setOrResetCacheExpirations(this.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));

            }

        }
    }

}
