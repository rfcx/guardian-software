package org.rfcx.guardian.updater;

import org.rfcx.guardian.updater.api.ApiCheckVersionUtils;
import org.rfcx.guardian.updater.install.InstallUtils;
import org.rfcx.guardian.updater.receiver.ConnectivityReceiver;
import org.rfcx.guardian.updater.service.ApiCheckVersionTrigger;
import org.rfcx.guardian.updater.service.ApiCheckVersionService;
import org.rfcx.guardian.updater.service.DownloadFileService;
import org.rfcx.guardian.updater.service.InstallAppService;
import org.rfcx.guardian.updater.service.RebootTriggerService;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.device.DeviceConnectivity;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.net.ConnectivityManager;

public class RfcxGuardian extends Application {

    public String version;

    public static final String APP_ROLE = "Updater";

    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");

    public RfcxGuardianIdentity rfcxGuardianIdentity = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxServiceHandler rfcxServiceHandler = null;

    private final BroadcastReceiver connectivityReceiver = new ConnectivityReceiver();

    public ApiCheckVersionUtils apiCheckVersionUtils = null;
    public InstallUtils installUtils = null;

    // for checking battery level
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);
    public DeviceConnectivity deviceConnectivity = new DeviceConnectivity(APP_ROLE);

    public long lastApiCheckTriggeredAt = System.currentTimeMillis();
    public String targetAppRole = "";

    public String[] RfcxCoreServices =
            new String[]{
            };

    @Override
    public void onCreate() {

        super.onCreate();

        this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
        this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
        this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);

        this.version = RfcxRole.getRoleVersion(this, logTag);
        this.rfcxPrefs.writeVersionToFile(this.version);

        this.registerReceiver(connectivityReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        this.apiCheckVersionUtils = new ApiCheckVersionUtils(this);
        this.apiCheckVersionUtils.setApiCheckVersionEndpoint(this.rfcxGuardianIdentity.getGuid());

        this.installUtils = new InstallUtils(this);

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

    public void initializeRoleServices() {

        if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {

            String[] runOnceOnlyOnLaunch = new String[] {
                    "ApiCheckVersionTrigger"
                            +"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits 2 minutes before running
                            +"|"+ ( ( 2 * ApiCheckVersionUtils.minimumAllowedIntervalBetweenCheckIns ) * ( 60 * 1000 ) ) // repeats hourly
            };

            String[] onLaunchServices = new String[ RfcxCoreServices.length + runOnceOnlyOnLaunch.length ];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
        }

    }

    private void setDbHandlers() {

    }

    private void setServiceHandlers() {
        this.rfcxServiceHandler.addService("ApiCheckVersionTrigger", ApiCheckVersionTrigger.class);
        this.rfcxServiceHandler.addService("ApiCheckVersion", ApiCheckVersionService.class);
        this.rfcxServiceHandler.addService("DownloadFile", DownloadFileService.class);
        this.rfcxServiceHandler.addService("InstallApp", InstallAppService.class);
        this.rfcxServiceHandler.addService("RebootTrigger", RebootTriggerService.class);
    }


}