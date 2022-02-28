package org.rfcx.guardian.admin.comms.sbd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class SbdDispatchTimeoutService extends Service {

    public static final String SERVICE_NAME = "SbdDispatchTimeout";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdDispatchTimeoutService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private SbdDispatchTimeoutSvc sbdDispatchTimeoutSvc;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.sbdDispatchTimeoutSvc = new SbdDispatchTimeoutSvc();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.sbdDispatchTimeoutSvc.start();
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
        this.sbdDispatchTimeoutSvc.interrupt();
        this.sbdDispatchTimeoutSvc = null;
    }


    private class SbdDispatchTimeoutSvc extends Thread {

        public SbdDispatchTimeoutSvc() {
            super("SbdDispatchTimeoutService-SbdDispatchTimeoutSvc");
        }

        @Override
        public void run() {
            SbdDispatchTimeoutService sbdDispatchTimeoutInstance = SbdDispatchTimeoutService.this;

            app = (RfcxGuardian) getApplication();

            int checkIntervalCount = Math.round((SbdUtils.sendCmdTimeout + (3 * SbdUtils.prepCmdTimeout)) / 667);

            try {

                app.rfcxSvc.reportAsActive(SERVICE_NAME);

                for (int i = 0; i <= checkIntervalCount; i++) {
                    if (app.sbdUtils.isInFlight) {
                        Thread.sleep(667);
                        if (i == checkIntervalCount) {
                            Log.e(logTag, "Timeout Reached for SBD Send. Killing serial processes...");
                            ShellCommands.killProcessesByIds(app.sbdUtils.findRunningSerialProcessIds());
                        }
                    } else {
                        break;
                    }
                }

            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
                app.rfcxSvc.setRunState(SERVICE_NAME, false);
                sbdDispatchTimeoutInstance.runFlag = false;
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            sbdDispatchTimeoutInstance.runFlag = false;
            //	Log.v(logTag, "Stopping service: "+logTag);
        }
    }


}
