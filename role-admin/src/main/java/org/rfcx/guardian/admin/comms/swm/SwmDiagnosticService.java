package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTBackground;
import org.rfcx.guardian.admin.comms.swm.data.SwmRTSatellite;
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

            int checkIntervalCount = 60000;

            if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_SATELLITE_PROTOCOL).equalsIgnoreCase("swm")) {
                while (swmDiagnosticInstance.runFlag) {

                    try {
                        // saving every 60s
                        Thread.sleep(checkIntervalCount);

                        SwmRTBackground rtBackground = app.swmUtils.getSwmCommand().getRTBackground();
                        SwmRTSatellite rtSatellite = app.swmUtils.getSwmCommand().getRTSatellite();

                        if (rtBackground != null || rtSatellite != null) {
                            app.swmMetaDb.dbSwmDiagnostic.insert(
                                    rtBackground != null ? rtBackground.getRssi() : null,
                                    rtSatellite != null ? rtSatellite.getRssi() : null,
                                    rtSatellite != null ? rtSatellite.getSignalToNoiseRatio() : null,
                                    rtSatellite != null ? rtSatellite.getFrequencyDeviation() : null,
                                    rtSatellite != null ? rtSatellite.getPacketTimestamp() : null,
                                    rtSatellite != null ? rtSatellite.getSatelliteId() : null
                            );
                        }


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
