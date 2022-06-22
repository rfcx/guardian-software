package org.rfcx.guardian.guardian.asset.detections;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioDetectionFilterJobService extends Service {

    public static final String SERVICE_NAME = "AudioDetectionFilterJob";

    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioDetectionFilterJobService");

    private RfcxGuardian app;

    private boolean runFlag = false;
    private AudioDetectionFilterJob audioDetectionFilterJob;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.audioDetectionFilterJob = new AudioDetectionFilterJob();
        app = (RfcxGuardian) getApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
        this.runFlag = true;
        app.rfcxSvc.setRunState(SERVICE_NAME, true);
        try {
            this.audioDetectionFilterJob.start();
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
        this.audioDetectionFilterJob.interrupt();
        this.audioDetectionFilterJob = null;
    }

    private class AudioDetectionFilterJob extends Thread {

        public AudioDetectionFilterJob() {
            super("AudioDetectionFilterJobService-AudioDetectionFilterJob");
        }

        @Override
        public void run() {
            AudioDetectionFilterJobService audioDetectionFilterJobInstance = AudioDetectionFilterJobService.this;

            app = (RfcxGuardian) getApplication();

            app.rfcxSvc.reportAsActive(SERVICE_NAME);

            try {

                Map<String, String[]> filters = new HashMap<String, String[]>();

                List<String[]> latestUnfilteredDetections = app.audioDetectionDb.dbUnfiltered.getAllRows();

                for (String[] latestUnfilteredRow : latestUnfilteredDetections) {

                    app.rfcxSvc.reportAsActive(SERVICE_NAME);

                    if (latestUnfilteredRow[0] != null) {

                        String classTag = latestUnfilteredRow[1];
                        String clsfrId = latestUnfilteredRow[2];
                        String clsfrName = latestUnfilteredRow[3];
                        String clsfrVersion = latestUnfilteredRow[4];
                        String filterId = latestUnfilteredRow[5];
                        String audioId = latestUnfilteredRow[6];
                        long beginsAtMs = Long.parseLong(latestUnfilteredRow[7]);
                        long windowSizeMs = Math.round(Float.parseFloat(latestUnfilteredRow[8]) * 1000);
                        long stepSizeMs = Math.round(Float.parseFloat(latestUnfilteredRow[9]) * 1000);
                        JSONArray confidences = new JSONArray(latestUnfilteredRow[10]);
                        String threshold = latestUnfilteredRow[11];

                        String[] clsfrProfile = app.assetLibraryDb.dbClassifier.getSingleRowById(clsfrId);
                        JSONObject clsfrProfileJson = new JSONObject(clsfrProfile[7]);

                        String[] filterRow = new String[]{};
//						if (!filters.containsKey(filterId)) {
//							filterRow = app.assetLibraryDb.dbClassifier.getSingleRowById(filterId);
//						}
//						filterRow = filters.get(filterId);

                        String[] allowedClassifications = new String[]{"chainsaw", "elephant"};
                        double filterConfidenceMinThreshold = Double.parseDouble(threshold);
                        double filterConfidenceMinCountPerMinute = 4;
                        int detectionSigFigs = 2;


                        if (ArrayUtils.doesStringArrayContainString(allowedClassifications, classTag)) {

                            List<String> filteredConfidences = new ArrayList<>();
                            List<String> unFilteredConfidences = new ArrayList<>();
                            //					long[] timeAtFirstAndLastDetections = new long[] { beginsAtMs, beginsAtMs };
                            int eligibleDetectionsCount = 0;

                            for (int i = 0; i < confidences.length(); i++) {
                                double confVal = Double.parseDouble(confidences.getString(i));
                                if (filterConfidenceMinThreshold <= confVal) {
                                    filteredConfidences.add(String.format(Locale.US, "%." + detectionSigFigs + "f", confVal));
//									if (eligibleDetectionsCount == 0) { timeAtFirstAndLastDetections[0] += stepSizeMs; }
//									timeAtFirstAndLastDetections[1] += stepSizeMs;
                                    eligibleDetectionsCount++;
                                } else {
                                    filteredConfidences.add("");
                                    unFilteredConfidences.add(String.format(Locale.US, "%." + detectionSigFigs + "f", confVal));
                                }
                            }

                            if (eligibleDetectionsCount >= (filterConfidenceMinCountPerMinute * app.rfcxPrefs.getPrefAsFloat(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION) / 60)) {
                                app.audioDetectionDb.dbFiltered.insert(
                                        classTag, clsfrId, clsfrName, clsfrVersion, filterId,
                                        audioId, beginsAtMs, windowSizeMs, stepSizeMs, TextUtils.join(",", filteredConfidences), threshold);
                                Log.v(logTag, eligibleDetectionsCount + " detection windows (" + Math.round((Double.parseDouble("" + eligibleDetectionsCount) / Double.parseDouble("" + confidences.length())) * 100) + "%) for classification '" + classTag + "' are above the " + filterConfidenceMinThreshold + " confidence threshold and have been preserved (required per minute: " + Math.round(filterConfidenceMinCountPerMinute) + ").");
                                // only allow sms guardian to force send detection
                                if (app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PROTOCOL_ESCALATION_ORDER).contains("sms")) {
                                    app.apiPingUtils.sendPing(false, new String[] { "detections" }, true);
                                }
                            } else {
                                Log.w(logTag, eligibleDetectionsCount + " detection windows (" + Math.round((Double.parseDouble("" + eligibleDetectionsCount) / Double.parseDouble("" + confidences.length())) * 100) + "%) for classification '" + classTag + "' are above the " + filterConfidenceMinThreshold + " confidence threshold (required per minute: " + Math.round(filterConfidenceMinCountPerMinute) + "). None will be preserved.");
                            }
                        }

                        app.audioDetectionDb.dbUnfiltered.deleteSingleRow(classTag, audioId);

                    } else {
                        Log.d(logTag, "Queued detections row entry in database is invalid.");

                    }

                }

            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
                app.rfcxSvc.setRunState(SERVICE_NAME, false);
                audioDetectionFilterJobInstance.runFlag = false;
            }

            app.rfcxSvc.setRunState(SERVICE_NAME, false);
            audioDetectionFilterJobInstance.runFlag = false;

        }
    }


}
