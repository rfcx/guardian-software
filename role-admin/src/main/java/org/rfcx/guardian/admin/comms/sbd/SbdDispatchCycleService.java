package org.rfcx.guardian.admin.comms.sbd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class SbdDispatchCycleService extends Service {

    public static final String SERVICE_NAME = "SbdDispatchCycle";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdDispatchCycleService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private SbdDispatchCycleSvc sbdDispatchCycleSvc;

    private final long sbdDispatchCycleDuration = 15000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.sbdDispatchCycleSvc = new SbdDispatchCycleSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.sbdDispatchCycleSvc.start();
        } catch (IllegalThreadStateException e) {
            RfcxLog.logExc(logTag, e);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.runFlag = false;
        app.rfcxSvc.setRunState(SERVICE_NAME, false);
        this.sbdDispatchCycleSvc.interrupt();
        this.sbdDispatchCycleSvc = null;
    }


    private class SbdDispatchCycleSvc extends Thread {

        public SbdDispatchCycleSvc() {
            super("SbdDispatchCycleService-SbdDispatchCycleSvc");
        }

        @Override
        public void run() {
            SbdDispatchCycleService sbdDispatchCycleInstance = SbdDispatchCycleService.this;

            app = (RfcxGuardian) getApplication();

            app.rfcxSvc.reportAsActive(SERVICE_NAME);

            if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("sbd")) {

                int cyclesSinceLastActivity = 0;
                int powerOffAfterThisManyInactiveCycles = 6;

                ShellCommands.killProcessesByIds(app.sbdUtils.findRunningSerialProcessIds());
                app.sbdUtils.setupSbdUtils();

                while (sbdDispatchCycleInstance.runFlag) {

                    try {

                        app.rfcxSvc.reportAsActive(SERVICE_NAME);

                        Thread.sleep(sbdDispatchCycleDuration);

                        if (app.sbdMessageDb.dbSbdQueued.getCount() == 0) {

                            // let's add something that checks and eventually powers off the satellite board if not used for a little while
                            if (cyclesSinceLastActivity == powerOffAfterThisManyInactiveCycles) {
                                app.sbdUtils.setPower(false);
                            }
                            cyclesSinceLastActivity++;


                        } else if (!app.sbdUtils.isInFlight) {

                            boolean isAbleToSend = app.sbdUtils.isPowerOn();

                            if (!isAbleToSend) {
                                Log.i(logTag, "Iridium board is powered OFF. Turning power ON...");
                                app.sbdUtils.setPower(true);
                                isAbleToSend = app.sbdUtils.isPowerOn();
                            }

                            if (!isAbleToSend) {
                                Log.e(logTag, "Iridium board is STILL powered off. Unable to proceed with SBD send...");
                            } else if (!app.sbdUtils.isNetworkAvailable()) {
                                Log.e(logTag, "Iridium Network is not available. Unable to proceed with SBD send...");
                            } else {
                                app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SbdDispatchService.SERVICE_NAME, Math.round(1.5 * SbdUtils.sendCmdTimeout));
                                cyclesSinceLastActivity = 0;
                            }

                        } else {

                            app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SbdDispatchService.SERVICE_NAME, Math.round(1.5 * SbdUtils.sendCmdTimeout));
                            cyclesSinceLastActivity = 0;

                        }

                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        sbdDispatchCycleInstance.runFlag = false;
                    }
                }
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            sbdDispatchCycleInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }
    }


}
