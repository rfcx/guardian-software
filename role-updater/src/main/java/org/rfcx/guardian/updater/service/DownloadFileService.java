package org.rfcx.guardian.updater.service;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DownloadFileService extends Service {

    public static final String SERVICE_NAME = "DownloadFile";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DownloadFileService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private DownloadFile downloadFile;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.downloadFile = new DownloadFile();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.v(logTag, "Starting service: " + logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.downloadFile.start();
        } catch (IllegalThreadStateException e) {
            RfcxLog.logExc(logTag, e);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.runFlag = false;
        app.rfcxSvc.setRunState(SERVICE_NAME, false);
        this.downloadFile.interrupt();
        this.downloadFile = null;
    }

    private class DownloadFile extends Thread {

        public DownloadFile() {
            super("DownloadFileService-DownloadFile");
        }

        @Override
        public void run() {
            DownloadFileService downloadFileService = DownloadFileService.this;

            HttpGet httpGet = new HttpGet(app.getApplicationContext(), RfcxGuardian.APP_ROLE);

            try {

                if (httpGet.getAsFile(app.installUtils.installVersionUrl, app.getApplicationContext().getFilesDir() + "/" + app.installUtils.apkFileNameDownload)) {

                    Log.d(logTag, "APK download complete. Verifying downloaded checksum...");
                    String downloadFileSha1 = FileUtils.sha1Hash(app.installUtils.apkPathDownload);
                    Log.d(logTag, "APK file SHA1 checksum : " + downloadFileSha1 + " (expected: " + app.installUtils.installVersionSha1);

                    if (downloadFileSha1.equalsIgnoreCase(app.installUtils.installVersionSha1)) {

                        Log.d(logTag, "Checksum passed. Uncompressing APK...");

                        FileUtils.gUnZipFile(app.installUtils.apkPathDownload, app.installUtils.apkPathPostDownload);

                        Log.d(logTag, "APK Uncompresssed. Moving APK file to external storage...");
                        FileUtils.delete(app.installUtils.apkPathDownload);
                        FileUtils.delete(app.installUtils.apkPathExternal);
                        FileUtils.copy(app.installUtils.apkPathPostDownload, app.installUtils.apkPathExternal);
                        FileUtils.chmod(app.installUtils.apkPathExternal, "rw", "rw");

                        if (FileUtils.sha1Hash(app.installUtils.apkPathExternal).equalsIgnoreCase(FileUtils.sha1Hash(app.installUtils.apkPathPostDownload))) {

                            FileUtils.delete(app.installUtils.apkPathPostDownload);
                            app.rfcxSvc.triggerService(InstallAppService.SERVICE_NAME, false);
                        }

                    } else {
                        Log.e(logTag, "Checksum failed. Skipping installation and deleting apk download.");
                        FileUtils.delete(app.installUtils.apkPathDownload);
                    }
                } else {
                    Log.e(logTag, "Download failed.");
                    FileUtils.delete(app.installUtils.apkPathDownload);
                }
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            } finally {
                app.rfcxSvc.setRunState(SERVICE_NAME, false);
                app.rfcxSvc.stopService(SERVICE_NAME);
            }
        }
    }

}
