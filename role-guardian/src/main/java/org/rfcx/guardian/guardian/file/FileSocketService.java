package org.rfcx.guardian.guardian.file;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class FileSocketService extends Service {

    public static final String SERVICE_NAME = "FileSocket";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "FileSocketService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private FileSocketSvc fileSocketSvc;

    private static final long minPushCycleDurationMs = 667;
    private static final int ifSendFailsThenExtendLoopByAFactorOf = 4;
    private static final int maxSendFailureThreshold = 24;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.fileSocketSvc = new FileSocketSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.fileSocketSvc.start();
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
        this.fileSocketSvc.interrupt();
        this.fileSocketSvc = null;
    }


    private class FileSocketSvc extends Thread {

        public FileSocketSvc() {
            super("FileSocketService-FileSocketSvc");
        }

        @Override
        public void run() {
            FileSocketService fileSocketServiceInstance = FileSocketService.this;

            app = (RfcxGuardian) getApplication();

            int currFailureThreshold = maxSendFailureThreshold + 1;
            long pingPushCycleDurationMs = Math.max(app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.COMPANION_TELEMETRY_PUSH_CYCLE), minPushCycleDurationMs);

            if (app.fileSocketUtils.isSocketServerEnablable(true, app.rfcxPrefs)) {
                while (fileSocketServiceInstance.runFlag) {

                    try {

                        app.rfcxSvc.reportAsActive(SERVICE_NAME);
                        if (currFailureThreshold >= maxSendFailureThreshold) {
                            app.fileSocketUtils.socketUtils.stopServer();
                            app.fileSocketUtils.startServer();
                            app.fileSocketUtils.resetPingObject();
                            currFailureThreshold = 0;
                            pingPushCycleDurationMs = Math.max(app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.COMPANION_TELEMETRY_PUSH_CYCLE), minPushCycleDurationMs);
                            Thread.sleep(pingPushCycleDurationMs);
                        }

                        if (app.fileSocketUtils.sendPingCheckingConnection()) {
                            Thread.sleep(pingPushCycleDurationMs);
                            currFailureThreshold = 0;
                        } else {
                            Thread.sleep(ifSendFailsThenExtendLoopByAFactorOf * pingPushCycleDurationMs);
                            currFailureThreshold++;
                        }
                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        fileSocketServiceInstance.runFlag = false;
                    }
                }
            } else {
                app.fileSocketUtils.resetPingObject();
                app.fileSocketUtils.socketUtils.stopServer();
            }
            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            fileSocketServiceInstance.runFlag = false;
            app.fileSocketUtils.resetPingObject();
            Log.v(logTag, "Stopping service: " + logTag);
        }
    }


}
