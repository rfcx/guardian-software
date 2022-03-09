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

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiCheckInArchiveService");
    private static final SimpleDateFormat dirDateFormat = new SimpleDateFormat("yyyy-MM", Locale.US);
    private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.US);
    private static final SimpleDateFormat metaDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US);
    private static final String[] tsvMetaColumns = new String[]{"measured_at", "queued_at", "filename", "format", "sha1checksum", "samplerate", "bitrate", "encode_duration"};
    private RfcxGuardian app;
    private String rfcxDeviceId;
    private long archiveTimestamp = System.currentTimeMillis();
    private String archiveSdCardDir;
    private String archiveTitle;
    private String archiveWorkDir;
    private String archiveTar;
    private String archiveTarFilePath;
    private String archiveFinalFilePath;
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
//		Log.v(logTag, "Starting service: "+logTag);
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
//		Log.v(logTag, "Stopping service: "+logTag);
    }

    private void setAndInitializeCheckInArchiveDirectories(Context context) {

        archiveTitle = "archive_" + rfcxDeviceId + "_" + fileDateTimeFormat.format(new Date(archiveTimestamp));
        archiveWorkDir = context.getFilesDir().toString() + "/archive/" + archiveTitle;
        archiveTar = "archive/" + archiveTitle + ".tar";
        archiveTarFilePath = context.getFilesDir().toString() + "/" + archiveTar;
        archiveSdCardDir = Environment.getExternalStorageDirectory().toString() + "/rfcx/archive/audio/" + dirDateFormat.format(new Date(archiveTimestamp));
        archiveFinalFilePath = archiveSdCardDir + "/" + archiveTitle + ".tar";

        FileUtils.initializeDirectoryRecursively(archiveSdCardDir, true);
        FileUtils.initializeDirectoryRecursively(archiveWorkDir + "/audio", false);
    }

    private class ApiCheckInArchive extends Thread {

        public ApiCheckInArchive() {
            super("ApiCheckInArchiveService-ApiCheckInArchive");
        }

        @Override
        public void run() {
            ApiCheckInArchiveService apiCheckInArchiveInstance = ApiCheckInArchiveService.this;

            app = (RfcxGuardian) getApplication();
            Context context = app.getApplicationContext();

            rfcxDeviceId = app.rfcxGuardianIdentity.getGuid();
            archiveTimestamp = System.currentTimeMillis();

            setAndInitializeCheckInArchiveDirectories(context); // best to run this before AND after the cleanup
            String archiveAppFilesDir = context.getFilesDir().toString() + "/archive";
            for (File fileToRemove : FileUtils.getDirectoryContents(archiveAppFilesDir, true)) {
                FileUtils.delete(fileToRemove);
            }
            for (File fileToRemove : FileUtils.getEmptyDirectories(archiveAppFilesDir)) {
                FileUtils.delete(fileToRemove);
            }
            FileUtils.deleteDirectoryContents(archiveAppFilesDir);
            setAndInitializeCheckInArchiveDirectories(context); // best to run this before AND after the cleanup

            long archiveFileSizeTarget = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_ARCHIVE_FILESIZE_TARGET);
            long archiveFileSizeTargetInBytes = archiveFileSizeTarget * 1024 * 1024;

            long stashFileSizeBuffer = app.rfcxPrefs.getPrefAsLong(RfcxPrefs.Pref.CHECKIN_STASH_FILESIZE_BUFFER);
            long stashFileSizeBufferInBytes = stashFileSizeBuffer * 1024 * 1024;

            long stashedCumulativeFileSizeInBytes = app.apiCheckInDb.dbStashed.getCumulativeFileSizeForAllRows();

            if (!(new File(archiveSdCardDir)).isDirectory()) {
                Log.e(logTag, "CheckIn Archive job cancelled because SD card directory could not be located: " + archiveSdCardDir);

            } else if (stashedCumulativeFileSizeInBytes < (stashFileSizeBufferInBytes + archiveFileSizeTargetInBytes)) {
                Log.e(logTag, "CheckIn Archive job cancelled because archive threshold (" + archiveFileSizeTarget + " MB) has not been reached.");

            } else {

                try {

                    List<String[]> stashedCheckInsBeyondBuffer = new ArrayList<String[]>();
                    List<String[]> allStashedCheckIns = app.apiCheckInDb.dbStashed.getAllRows();

                    long fileSizeBufferTracker = 0;

                    for (int i = allStashedCheckIns.size() - 1; i >= 0; i--) {
                        fileSizeBufferTracker += Long.parseLong(allStashedCheckIns.get(i)[6]);
                        if (fileSizeBufferTracker > stashFileSizeBufferInBytes) {
                            stashedCheckInsBeyondBuffer.add(allStashedCheckIns.get(i));
                        }
                    }

                    if (!app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_CHECKIN_ARCHIVE)) {

                        Log.d(logTag, "CheckIn Archive disabled due to preference 'enable_checkin_archive' being explicitly set to false.");

                    } else {

                        Log.i(logTag, "Preparing CheckIn Archive Process...");

                        Log.i(logTag, "Archiving " + stashedCheckInsBeyondBuffer.size() + " Stashed CheckIns.");

                        // Create Archive File List
                        List<String> archiveFileList = new ArrayList<String>();

                        StringBuilder tsvRows = new StringBuilder();
                        tsvRows.append(TextUtils.join("\t", tsvMetaColumns)).append("\n");

                        long oldestCheckInTimestamp = System.currentTimeMillis();
                        long newestCheckInTimestamp = 0;

                        for (String[] checkIn : stashedCheckInsBeyondBuffer) {

                            // Create TSV contents row
                            JSONObject audioJson = new JSONObject(checkIn[2]);
                            String[] audioMeta = audioJson.getString("audio").split("\\*");

                            long measuredAt = Long.parseLong(audioMeta[1]);
                            int audioDuration = Integer.parseInt(audioMeta[10]);
                            int sampleRate = Integer.parseInt(audioMeta[4]);

                            String archivedAudioFileName = RfcxAudioFileUtils.getAudioFileName(rfcxDeviceId, measuredAt, audioMeta[2], audioDuration, sampleRate);
                            String archivedAudioTmpFilePath = archiveWorkDir + "/audio/" + archivedAudioFileName;

                            String tsvRow = ""
                                    /* measured_at */ + metaDateTimeFormat.format(new Date(measuredAt)) + "\t"
                                    /* queued_at */ + metaDateTimeFormat.format(new Date(Long.parseLong(audioJson.getString("queued_at")))) + "\t"
                                    /* filename */ + archivedAudioFileName + "\t"
                                    /* format */ + audioMeta[2] + "\t"
                                    /* sha1checksum */ + audioMeta[3] + "\t"
                                    /* samplerate */ + sampleRate + "\t"
                                    /* bitrate */ + audioMeta[5] + "\t"
                                    /* encode_duration */ + audioMeta[8]
                                    + "\n";

                            // UnGZip audio files into position
                            FileUtils.gUnZipFile(checkIn[4], archivedAudioTmpFilePath);

                            if (FileUtils.exists(archivedAudioTmpFilePath)) {
                                FileUtils.chmod(archivedAudioTmpFilePath, "rw", "rw");
                                tsvRows.append(tsvRow);
                                archiveFileList.add(archivedAudioTmpFilePath);
                            }

                            if (measuredAt < oldestCheckInTimestamp) {
                                oldestCheckInTimestamp = measuredAt;
                            }
                            if (measuredAt > newestCheckInTimestamp) {
                                newestCheckInTimestamp = measuredAt;
                            }
                        }

                        StringUtils.saveStringToFile(tsvRows.toString(), archiveWorkDir + "/_metadata_audio.tsv");
                        archiveFileList.add(archiveWorkDir + "/_metadata_audio.tsv");
                        FileUtils.chmod(archiveWorkDir + "/_metadata_audio.tsv", "rw", "rw");

                        Log.i(logTag, "Creating CheckIn Archive: " + archiveTitle);
                        FileUtils.createTarArchiveFromFileList(archiveFileList, archiveTarFilePath);
                        FileUtils.chmod(archiveTarFilePath, "rw", "rw");
                        long archiveFileSize = FileUtils.getFileSizeInBytes(archiveTarFilePath);

                        if (DeviceStorage.isExternalStorageWritable()) {

                            Log.i(logTag, "Transferring CheckIn Archive (" + FileUtils.bytesAsReadableString(archiveFileSize) + ") to External Storage: " + archiveFinalFilePath);
                            FileUtils.copy(archiveTarFilePath, archiveFinalFilePath);
                            FileUtils.chmod(archiveFinalFilePath, "rw", "rw");

                            app.apiCheckInArchiveDb.dbArchive.insert(
                                    new Date(archiveTimestamp),        // archived_at
                                    new Date(oldestCheckInTimestamp),    // archive_begins_at
                                    new Date(newestCheckInTimestamp),    // archive_ends_at
                                    stashedCheckInsBeyondBuffer.size(),  // record_count
                                    archiveFileSize,                    // filesize in bytes
                                    archiveFinalFilePath                // filepath
                            );

                            Log.i(logTag, "CheckIn Archive Job Complete: "
                                    + stashedCheckInsBeyondBuffer.size() + " audio files, "
                                    + FileUtils.bytesAsReadableString(archiveFileSize) + ", "
                                    + archiveFinalFilePath);
                        }
                    }

                    // Clean up and remove archived originals
                    for (String[] checkIn : stashedCheckInsBeyondBuffer) {
                        FileUtils.delete(checkIn[4]);
                        app.apiCheckInDb.dbStashed.deleteSingleRowByAudioAttachmentId(checkIn[1]);
                    }

                    FileUtils.delete(archiveWorkDir);
                    FileUtils.delete(archiveTarFilePath);

                    Log.d(logTag, stashedCheckInsBeyondBuffer.size() + " CheckIns have been deleted from stash.");


                } catch (Exception e) {
                    RfcxLog.logExc(logTag, e);

                }
            }

            apiCheckInArchiveInstance.runFlag = false;
            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            app.rfcxSvc.stopService(SERVICE_NAME, false);
        }
    }

}
