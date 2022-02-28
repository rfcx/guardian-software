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
    private final long swmDispatchCycleDuration = 15000;
    private RfcxGuardian app;
    private boolean runFlag = false;
    private SwmDispatchCycleSvc swmDispatchCycleSvc;

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

            app.rfcxSvc.reportAsActive(SERVICE_NAME);

            if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("swm")) {

                int cyclesSinceLastActivity = 0;
                int powerOffAfterThisManyInactiveCycles = 6;

                app.rfcxSvc.triggerService(SwmDispatchTimeoutService.SERVICE_NAME, true);
                app.swmUtils.setupSwmUtils();

                while (swmDispatchCycleInstance.runFlag) {

                    try {

                        app.rfcxSvc.reportAsActive(SERVICE_NAME);

                        Thread.sleep(swmDispatchCycleDuration);

                        // check for satellite on/off hours and enable/disable swarm tile accordingly
                        if (!app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
                            if (!app.swmUtils.isInFlight) {
                                // to kill the process before calling PO command
                                app.rfcxSvc.triggerService(SwmDispatchTimeoutService.SERVICE_NAME, true);
                                app.swmUtils.getPower().setOn(false);
                            }

                            // power on if power is off but in working period
                        } else if (!app.swmUtils.getPower().getOn()) {
                            app.swmUtils.getPower().setOn(true);

                            // swarm is on and in working period
                        } else {
                            if (app.swmMessageDb.dbSwmQueued.getCount() == 0) {

                                // let's add something that checks and eventually powers off the satellite board if not used for a little while
                                if (cyclesSinceLastActivity == powerOffAfterThisManyInactiveCycles) {
                                    app.swmUtils.getPower().setOn(true); //app.swmUtils.setPower(false);
                                }
                                cyclesSinceLastActivity++;


                            } else if (!app.swmUtils.isInFlight) {

                                boolean isAbleToSend = app.swmUtils.getPower().getOn();

                                if (!isAbleToSend) {
                                    Log.i(logTag, "Swarm board is powered OFF. Turning power ON...");
                                    app.swmUtils.getPower().setOn(true);
                                    isAbleToSend = app.swmUtils.getPower().getOn();
                                }

                                if (!isAbleToSend) {
                                    Log.e(logTag, "Swarm board is STILL powered off. Unable to proceed with SWM send...");
                                } else {
                                    app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SwmDispatchService.SERVICE_NAME, Math.round(1.5 * SwmUtils.sendCmdTimeout));
                                    cyclesSinceLastActivity = 0;
                                }

                            } else {
                                app.rfcxSvc.triggerOrForceReTriggerIfTimedOut(SwmDispatchService.SERVICE_NAME, Math.round(1.5 * SwmUtils.sendCmdTimeout));
                                cyclesSinceLastActivity = 0;
                            }
                        }

                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        swmDispatchCycleInstance.runFlag = false;
                    }
                }
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            swmDispatchCycleInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }
    }


}
