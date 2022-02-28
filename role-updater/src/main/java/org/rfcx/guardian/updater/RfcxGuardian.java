package org.rfcx.guardian.updater;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

import org.rfcx.guardian.updater.api.ApiUpdateRequestUtils;
import org.rfcx.guardian.updater.receiver.ConnectivityReceiver;
import org.rfcx.guardian.updater.service.ApiUpdateRequestService;
import org.rfcx.guardian.updater.service.ApiUpdateRequestTrigger;
import org.rfcx.guardian.updater.service.DownloadFileService;
import org.rfcx.guardian.updater.service.InstallAppService;
import org.rfcx.guardian.updater.service.RebootTriggerService;
import org.rfcx.guardian.updater.status.UpdaterStatus;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.install.InstallUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.rfcx.RfcxSvc;

public class RfcxGuardian extends Application {

    public static final String APP_ROLE = "Updater";
    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");
    private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();
    public String version;
    public RfcxGuardianIdentity rfcxGuardianIdentity = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxSvc rfcxSvc = null;
    public UpdaterStatus rfcxStatus = null;
    public ApiUpdateRequestUtils apiUpdateRequestUtils = null;
    public InstallUtils installUtils = null;

    // for checking battery level
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
    public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);

    public String[] RfcxCoreServices =
            new String[]{
            };

    @Override
    public void onCreate() {

        super.onCreate();

        this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
        this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
        this.rfcxSvc = new RfcxSvc(this, APP_ROLE);
        this.rfcxStatus = new UpdaterStatus(this);

        this.version = RfcxRole.getRoleVersion(this, logTag);
        RfcxRole.writeVersionToFile(this, logTag, this.version);

        this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        this.apiUpdateRequestUtils = new ApiUpdateRequestUtils(this);

        this.installUtils = new InstallUtils(this, APP_ROLE);

        setDbHandlers();
        setServiceHandlers();

        initializeRoleServices();
    }

    public void onTerminate() {
        super.onTerminate();
        this.unregisterReceiver(connectivityReceiver);
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
                    ApiUpdateRequestTrigger.SERVICE_NAME
                            + "|" + DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits 2 minutes before running
                            + "|" + ((2 * ApiUpdateRequestUtils.minimumAllowedIntervalBetweenUpdateRequests) * (60 * 1000)) // repeats hourly
            };

            String[] onLaunchServices = new String[RfcxCoreServices.length + runOnceOnlyOnLaunch.length];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxSvc.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
        }

    }

    private void setDbHandlers() {

    }

    private void setServiceHandlers() {
        this.rfcxSvc.addService(ApiUpdateRequestTrigger.SERVICE_NAME, ApiUpdateRequestTrigger.class);
        this.rfcxSvc.addService(ApiUpdateRequestService.SERVICE_NAME, ApiUpdateRequestService.class);
        this.rfcxSvc.addService(DownloadFileService.SERVICE_NAME, DownloadFileService.class);
        this.rfcxSvc.addService(InstallAppService.SERVICE_NAME, InstallAppService.class);
        this.rfcxSvc.addService(RebootTriggerService.SERVICE_NAME, RebootTriggerService.class);
    }

    public void onPrefReSync(String prefKey) {

        if (prefKey != null) {

            if (prefKey.equalsIgnoreCase(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION)) {
                this.rfcxStatus.setOrResetCacheExpirations(this.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION));

            }

        }
    }

}
