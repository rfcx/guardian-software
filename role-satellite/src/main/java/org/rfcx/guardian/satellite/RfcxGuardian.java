package org.rfcx.guardian.satellite;

import org.rfcx.guardian.satellite.sbd.SbdMessageDb;
import org.rfcx.guardian.satellite.service.ClockSyncJobService;
import org.rfcx.guardian.satellite.service.SbdDispatchCycleService;
import org.rfcx.guardian.satellite.service.SbdDispatchService;
import org.rfcx.guardian.utility.device.capture.DeviceBattery;
import org.rfcx.guardian.utility.rfcx.RfcxGuardianIdentity;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;

public class RfcxGuardian extends Application {

    public String version;

    public static final String APP_ROLE = "Satellite";

    private static final String logTag = RfcxLog.generateLogTag(APP_ROLE, "RfcxGuardian");

    public RfcxGuardianIdentity rfcxGuardianIdentity = null;
    public RfcxPrefs rfcxPrefs = null;
    public RfcxServiceHandler rfcxServiceHandler = null;

    // Database Handlers
    public SbdMessageDb sbdMessageDb = null;

    // for checking battery level
    public DeviceBattery deviceBattery = new DeviceBattery(APP_ROLE);

    public String[] RfcxCoreServices =
            new String[]{
                    "SbdDispatchCycle"
            };

    @Override
    public void onCreate() {

        super.onCreate();

        this.rfcxGuardianIdentity = new RfcxGuardianIdentity(this, APP_ROLE);
        this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
        this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);

        this.version = RfcxRole.getRoleVersion(this, logTag);
        RfcxRole.writeVersionToFile(this, logTag, this.version);

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

    public void initializeRoleServices() {

        if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {

            String[] runOnceOnlyOnLaunch = new String[] {
//                    "ApiUpdateRequestTrigger"
//                            +"|"+DateTimeUtils.nowPlusThisLong("00:02:00").getTimeInMillis() // waits 2 minutes before running
//                            +"|"+ ( ( 2 * ApiUpdateRequestUtils.minimumAllowedIntervalBetweenUpdateRequests) * ( 60 * 1000 ) ) // repeats hourly
            };

            String[] onLaunchServices = new String[ RfcxCoreServices.length + runOnceOnlyOnLaunch.length ];
            System.arraycopy(RfcxCoreServices, 0, onLaunchServices, 0, RfcxCoreServices.length);
            System.arraycopy(runOnceOnlyOnLaunch, 0, onLaunchServices, RfcxCoreServices.length, runOnceOnlyOnLaunch.length);
            this.rfcxServiceHandler.triggerServiceSequence("OnLaunchServiceSequence", onLaunchServices, true, 0);
        }

    }

    private void setDbHandlers() {

        this.sbdMessageDb = new SbdMessageDb(this, this.version);

    }

    private void setServiceHandlers() {

        this.rfcxServiceHandler.addService("ClockSyncJob", ClockSyncJobService.class);
        this.rfcxServiceHandler.addService("SbdDispatch", SbdDispatchService.class);
        this.rfcxServiceHandler.addService("SbdDispatchCycle", SbdDispatchCycleService.class);

    }

    public void onPrefReSync(String prefKey) {

    }

}
