package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class SwmDispatchCycleService extends Service {

    public static final String SERVICE_NAME = "SwmDispatchCycle";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchCycleService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private SwmDispatchCycleSvc swmDispatchCycleSvc;

    private final long swmDispatchCycleDuration = 15000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.swmDispatchCycleSvc = new SwmDispatchCycleSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.swmDispatchCycleSvc.start();
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
        this.swmDispatchCycleSvc.interrupt();
        this.swmDispatchCycleSvc = null;
    }


    private class SwmDispatchCycleSvc extends Thread {

        public SwmDispatchCycleSvc() {
            super("SwmDispatchCycleService-SwmDispatchCycleSvc");
        }

        @Override
        public void run() {
            SwmDispatchCycleService swmDispatchCycleInstance = SwmDispatchCycleService.this;
            app = (RfcxGuardian) getApplication();

            if (!app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("swm")) {
                Log.i(logTag, "Swarm is disabled");
                return;
            }

            app.rfcxSvc.reportAsActive(SERVICE_NAME);
            Log.i(logTag, "Setting up Swarm");
            app.swmUtils.setupSwmUtils();

            while (swmDispatchCycleInstance.runFlag) {

                app.rfcxSvc.reportAsActive(SERVICE_NAME);
                try {
                    Thread.sleep(swmDispatchCycleDuration);
                    trigger();
                } catch (Exception e) {
                    RfcxLog.logExc(logTag, e);
                    break;
                }
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            swmDispatchCycleInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }

        private void trigger() {
            // Check if Swarm should be OFF due to prefs
            if (!app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
                Log.d(logTag, "Swarm is OFF at this time");
                app.swmUtils.getPower().setOn(false);
                return;
            }

            // Check if Swarm should be OFF due to inactivity
            int queuedOnSwarm = app.swmUtils.getApi().getUnsentMessages().size();
            int queuedInDatabase = app.swmMessageDb.dbSwmQueued.getCount();
            if (queuedOnSwarm == 0 && queuedInDatabase == 0) {
                Log.d(logTag, "Swarm is OFF due to inactivity");
                app.swmUtils.getPower().setOn(false);
                return;
            }

            // Make sure it is on and dispatching
            app.swmUtils.getPower().setOn(true);
            Log.d(logTag, "Swarm is ON");
            app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SwmDispatchService.SERVICE_NAME, Math.round(1.5 * SwmUtils.sendCmdTimeout));
        }
    }


}
