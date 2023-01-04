package org.rfcx.guardian.guardian.api.methods.checkin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.device.capture.DeviceStorage;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ApiCheckInArchiveService extends Service {

    public static final String SERVICE_NAME = "ApiCheckInArchive";
    public static final String EXTRA_ARCHIVE_ALL = "ARCHIVE_ALL";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInArchiveService");
    private RfcxGuardian app;
    private boolean archiveAll = false;
    private boolean runFlag = false;
    private ApiCheckInArchive apiCheckInArchive;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.apiCheckInArchive = new ApiCheckInArchive();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
        this.archiveAll = intent.getBooleanExtra(EXTRA_ARCHIVE_ALL, false);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.apiCheckInArchive.start();
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
        this.apiCheckInArchive.interrupt();
        this.apiCheckInArchive = null;
		Log.v(logTag, "Stopping service: "+logTag);
    }

    private class ApiCheckInArchive extends Thread {

        public ApiCheckInArchive() {
            super("ApiCheckInArchiveService-ApiCheckInArchive");
        }

        @Override
        public void run() {
            ApiCheckInArchiveService apiCheckInArchiveInstance = ApiCheckInArchiveService.this;

            app = (RfcxGuardian) getApplication();

            app.apiCheckInArchiveUtils.cleanupTempArchiveDir();
            if (archiveAll) {
                app.apiCheckInArchiveUtils.archiveAllQueueAndStash();
            } else {
                app.apiCheckInArchiveUtils.archiveCheckIn();
            }

            apiCheckInArchiveInstance.runFlag = false;
            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            app.rfcxSvc.stopService(SERVICE_NAME, false);
        }
    }

}
