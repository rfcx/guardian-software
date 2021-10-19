package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTBackgroundResponse;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTResponse;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class SwmDiagnosticService extends Service {

    public static final String SERVICE_NAME = "SwmDiagnosticService";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDiagnosticServiceService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private SwmDiagnosticSvc swmDiagnosticSvc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.swmDiagnosticSvc = new SwmDiagnosticSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.swmDiagnosticSvc.start();
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
        this.swmDiagnosticSvc.interrupt();
        this.swmDiagnosticSvc = null;
    }


    private class SwmDiagnosticSvc extends Thread {

        public SwmDiagnosticSvc() {
            super("SwmDiagnosticService-SwmDiagnosticSvc");
        }

        @Override
        public void run() {
            SwmDiagnosticService swmDiagnosticInstance = SwmDiagnosticService.this;

            app = (RfcxGuardian) getApplication();

            int checkIntervalCount = 70000;

            if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("swm")) {
                while (swmDiagnosticInstance.runFlag) {

                    try {
                        // if swarm is on and in working period then do all of these
                        if (app.swmUtils.power.getOn() && app.swmUtils.isSatelliteAllowedAtThisTimeOfDay()) {
                            if (!app.swmUtils.isInFlight) {
                                app.swmUtils.isInFlight = true;
                                app.rfcxSvc.triggerService(SwmDispatchTimeoutService.SERVICE_NAME, true);
                                SwmRTBackgroundResponse rtBackground = app.swmUtils.getApi().getRTBackground();
                                SwmRTResponse rtSatellite = app.swmUtils.getApi().getRTSatellite();
                                Integer unsentMessageNumbers = app.swmUtils.getApi().getNumberOfUnsentMessages();
                                app.swmUtils.isInFlight = false;

                                Integer rssiBackground = null;
                                if (rtBackground != null) {
                                    rssiBackground = rtBackground.getRssi();
                                }

                                Integer rssiSat = null;
                                Integer snr = null;
                                Integer fdev = null;
                                String time = null;
                                String satId = null;
                                if (rtSatellite != null) {
                                    if (rtSatellite.getRssi() != 0) rssiSat = rtSatellite.getRssi();
                                    if (rtSatellite.getSignalToNoiseRatio() != 0) snr = rtSatellite.getSignalToNoiseRatio();
                                    if (rtSatellite.getFrequencyDeviation() != 0) fdev = rtSatellite.getFrequencyDeviation();
                                    if (!rtSatellite.getPacketTimestamp().equals("1970-01-01 00:00:00")) time = rtSatellite.getPacketTimestamp();
                                    if (!rtSatellite.getSatelliteId().equals("0x000000")) satId = rtSatellite.getSatelliteId();
                                }

                                if (rtBackground != null || rtSatellite != null) {
                                    app.swmMetaDb.dbSwmDiagnostic.insert(rssiBackground, rssiSat, snr, fdev, time, satId, unsentMessageNumbers);
                                }
                            }
                        }
                        
                        Thread.sleep(checkIntervalCount);

                    } catch (InterruptedException e) {
                        swmDiagnosticInstance.runFlag = false;
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        RfcxLog.logExc(logTag, e);
                    }
                }
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            swmDiagnosticInstance.runFlag = false;
        }
    }


}
