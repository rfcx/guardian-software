package org.rfcx.guardian.admin.companion;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class CompanionSocketService extends Service {

    public static final String SERVICE_NAME = "CompanionSocket";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "CompanionSocketService");
    private static final long minPushCycleDurationMs = 667;
    private static final int ifSendFailsThenExtendLoopByAFactorOf = 4;
    private static final int maxSendFailureThreshold = 24;
    private RfcxGuardian app;
    private boolean runFlag = false;
    private CompanionSocketSvc companionSocketSvc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.companionSocketSvc = new CompanionSocketSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.companionSocketSvc.start();
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
        this.companionSocketSvc.interrupt();
        this.companionSocketSvc = null;
    }


    private class CompanionSocketSvc extends Thread {

        public CompanionSocketSvc() {
            super("CompanionSocketService-CompanionSocketSvc");
        }

        @Override
        public void run() {
            CompanionSocketService companionSocketInstance = CompanionSocketService.this;

            app = (RfcxGuardian) getApplication();

            if (app.companionSocketUtils.socketUtils.isSocketServerEnablable(true, app.rfcxPrefs)) {

                int currFailureThreshold = maxSendFailureThreshold + 1;
                long pingPushCycleDurationMs = Math.max(app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.COMPANION_TELEMETRY_PUSH_CYCLE), minPushCycleDurationMs);

                while (companionSocketInstance.runFlag) {

                    try {

                        app.rfcxSvc.reportAsActive(SERVICE_NAME);

                        if (currFailureThreshold >= maxSendFailureThreshold) {
                            app.companionSocketUtils.socketUtils.stopServer();
                            app.companionSocketUtils.startServer();
                            app.companionSocketUtils.socketUtils.setupTimerForClientConnection();
                            currFailureThreshold = 0;
                            pingPushCycleDurationMs = Math.max(app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.COMPANION_TELEMETRY_PUSH_CYCLE), minPushCycleDurationMs);
                            Thread.sleep(pingPushCycleDurationMs);
                        }

                        if (app.companionSocketUtils.socketUtils.isReceivingMessageFromClient && app.companionSocketUtils.sendSocketPing()) {
                            Thread.sleep(pingPushCycleDurationMs);
                            currFailureThreshold = 0;
                            app.companionSocketUtils.updatePingJson(true);
                        } else {
                            Thread.sleep(ifSendFailsThenExtendLoopByAFactorOf * pingPushCycleDurationMs);
                            currFailureThreshold++;
                        }


                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        companionSocketInstance.runFlag = false;
                    }
                }
            } else {
                app.companionSocketUtils.socketUtils.stopServer();
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            companionSocketInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }
    }


}
