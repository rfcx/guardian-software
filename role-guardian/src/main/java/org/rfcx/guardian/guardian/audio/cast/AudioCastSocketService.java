package org.rfcx.guardian.guardian.audio.cast;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AudioCastSocketService extends Service {

    public static final String SERVICE_NAME = "AudioCastSocket";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioCastSocketService");
    private static final long minPushCycleDurationMs = 100;
    private static final int ifSendFailsThenExtendLoopByAFactorOf = 4;
    private static final int maxSendFailureThreshold = 24 * 6;
    private RfcxGuardian app;
    private boolean runFlag = false;
    private AudioCastSocketSvc audioCastSocketSvc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.audioCastSocketSvc = new AudioCastSocketSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.audioCastSocketSvc.start();
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
        this.audioCastSocketSvc.interrupt();
        this.audioCastSocketSvc = null;
    }


    private class AudioCastSocketSvc extends Thread {

        public AudioCastSocketSvc() {
            super("AudioCastSocketService-AudioCastSocketSvc");
        }

        @Override
        public void run() {
            AudioCastSocketService audioCastSocketInstance = AudioCastSocketService.this;

            app = (RfcxGuardian) getApplication();

            if (app.audioCastUtils.isAudioCastEnablable(true, app.rfcxPrefs)) {

                int currFailureThreshold = maxSendFailureThreshold + 1;
                long pingPushCycleDurationMs = 100;

                while (audioCastSocketInstance.runFlag) {

                    try {

                        app.rfcxSvc.reportAsActive(SERVICE_NAME);

                        if (currFailureThreshold >= maxSendFailureThreshold) {
                            app.audioCastUtils.socketUtils.stopServer();
                            app.audioCastUtils.startServer();
                            app.audioCastUtils.socketUtils.setupTimerForClientConnection();
                            currFailureThreshold = 0;
                            pingPushCycleDurationMs = minPushCycleDurationMs;
                            Thread.sleep(pingPushCycleDurationMs);
                            app.audioCastUtils.updatePingJson(false);
                        }
                        if (app.audioCastUtils.socketUtils.isReceivingMessageFromClient && app.audioCastUtils.sendSocketPing()) {
                            Thread.sleep(pingPushCycleDurationMs);
                            currFailureThreshold = 0;
                            app.audioCastUtils.updatePingJson(false);
                        } else {
                            Thread.sleep(ifSendFailsThenExtendLoopByAFactorOf * pingPushCycleDurationMs);
                            currFailureThreshold++;
                        }


                    } catch (Exception e) {
                        RfcxLog.logExc(logTag, e);
                        app.rfcxSvc.setRunState(SERVICE_NAME, false);
                        audioCastSocketInstance.runFlag = false;
                    }
                }
            } else {
                app.audioCastUtils.socketUtils.stopServer();
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            audioCastSocketInstance.runFlag = false;
            Log.v(logTag, "Stopping service: " + logTag);
        }
    }


}
