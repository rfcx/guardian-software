package org.rfcx.guardian.guardian.file;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class FileSocketService extends Service {

    public static final String SERVICE_NAME = "FileSocket";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "FileSocketService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private FileSocketSvc fileSocketSvc;

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
        Log.v(logTag, "Starting service: "+logTag);
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

        public FileSocketSvc() { super("FileSocketService-FileSocketSvc"); }

        @Override
        public void run() {
            FileSocketService fileSocketServiceInstance = FileSocketService.this;

            app = (RfcxGuardian) getApplication();

            if (app.fileSocketUtils.isSocketServerEnablable(true, app.rfcxPrefs)) {
                app.fileSocketUtils.socketUtils.stopServer();
                app.fileSocketUtils.startServer();
            } else {

                app.rfcxSvc.setRunState(SERVICE_NAME, false);
                fileSocketServiceInstance.runFlag = false;
                Log.v(logTag, "Stopping service: "+logTag);
            }
        }
    }


}
