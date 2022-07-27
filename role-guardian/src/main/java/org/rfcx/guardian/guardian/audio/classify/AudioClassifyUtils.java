package org.rfcx.guardian.guardian.audio.classify;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.asset.detections.AudioDetectionFilterJobService;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class AudioClassifyUtils {

    public static final int CLASSIFY_FAILURE_SKIP_THRESHOLD = 3;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils");

    private final RfcxGuardian app;

    public AudioClassifyUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        RfcxClassifierFileUtils.initializeClassifierDirectories(context);
    }

    public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

        ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
        for (String[] queuedRow : queuedForClassification) {
            audioQueuedForClassification.add(queuedRow[6]);
        }

        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(new String[]{RfcxAudioFileUtils.audioClassifyDir(context)}, audioQueuedForClassification, Math.round(maxAgeInMilliseconds / 60000), false, false);
    }

    public void queueClassifyJobAcrossRoles(String audioId, String classifierId, String classifierVersion, int classifierSampleRate, String audioFilePath, String classifierFilePath, String classifierWindowSize, String classifierStepSize, String classifierClasses, String classifierThreshold) {

        try {
            String classifyJobUrlBlob = TextUtils.join("|", new String[]{
                    RfcxComm.urlEncode(audioId),
                    RfcxComm.urlEncode(classifierId),
                    RfcxComm.urlEncode(classifierVersion),
                    classifierSampleRate + "",
                    RfcxComm.urlEncode(audioFilePath),
                    RfcxComm.urlEncode(classifierFilePath),
                    RfcxComm.urlEncode(classifierWindowSize),
                    RfcxComm.urlEncode(classifierStepSize),
                    RfcxComm.urlEncode(classifierClasses),
                    RfcxComm.urlEncode(classifierThreshold)
            });

            Cursor classifyQueueResponse = app.getResolver().query(
                    RfcxComm.getUri("classify", "classify_queue", classifyJobUrlBlob),
                    RfcxComm.getProjection("classify", "classify_queue"),
                    null, null, null);
            if (classifyQueueResponse != null) {
                classifyQueueResponse.close();
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public void parseIncomingClassificationPayloadAndSave(String classificationPayload) {

        try {
            JSONObject jsonObj = new JSONObject(StringUtils.gZipBase64ToUnGZipString(classificationPayload));

            String classifierId = jsonObj.getString("classifier_id");
            String audioId = jsonObj.getString("audio_id");
            String stepSize = jsonObj.getString("step_size");
            String windowSize = jsonObj.getString("window_size");

            Log.d(logTag, "Classify Detections Received: Audio: " + audioId + ", Classifier: " + classifierId);

            String[] classiferLibraryInfo = app.assetLibraryDb.dbClassifier.getSingleRowById(classifierId);
            JSONObject classiferJsonMeta = new JSONObject(classiferLibraryInfo[7]);
            String classifierName = classiferJsonMeta.getString("classifier_name");
            String classifierVersion = classiferJsonMeta.getString("classifier_version");

            JSONArray vaultJsonArr = new JSONArray();
            boolean isVaultEnabled = app.rfcxPrefs.getPrefAsBoolean(RfcxPrefs.Pref.ENABLE_AUDIO_VAULT);

            int thresholdIndex = 0;
            for (Iterator<String> classificationNames = jsonObj.getJSONObject("detections").keys(); classificationNames.hasNext(); ) {
                String classificationTag = classificationNames.next();
                JSONArray detections = jsonObj.getJSONObject("detections").getJSONArray(classificationTag);
                String threshold = jsonObj.getString("threshold");
                app.audioDetectionDb.dbUnfiltered.insert(
                        classificationTag, classifierId, classifierName, classifierVersion, "-",
                        audioId, audioId, windowSize, stepSize, detections.toString(), threshold.split(",")[thresholdIndex++]
                );

                if (isVaultEnabled) {
                    JSONObject vaultJsonObj = new JSONObject();
                    vaultJsonObj.put("classifier_name", classifierName + "-v" + classifierVersion);
                    vaultJsonObj.put("classifier_id", classifierId);
                    vaultJsonObj.put("classification_tag", classificationTag);
                    vaultJsonObj.put("audio_id", audioId);
                    vaultJsonObj.put("measured_at", (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US)).format(new Date(Long.parseLong(audioId))));
                    vaultJsonObj.put("detections", detections);
                    vaultJsonObj.put("step_size", stepSize);
                    vaultJsonObj.put("window_size", windowSize);
                    vaultJsonArr.put(vaultJsonObj);
                }
            }

            if (isVaultEnabled) {
                saveClassificationPayloadSnapshotToVault(audioId, classifierName, classifierVersion, vaultJsonArr);
            }

            // save classify job stats
            long classifyJobDuration = Long.parseLong(jsonObj.getString("classify_duration"));
            long classifyAudioSize = Long.parseLong(jsonObj.getString("audio_size"));
            app.latencyStatsDb.dbClassifyLatency.insert(classifierName + "-v" + classifierVersion, classifyJobDuration, classifyAudioSize);

            app.rfcxSvc.triggerService(AudioDetectionFilterJobService.SERVICE_NAME, false);

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private void saveClassificationPayloadSnapshotToVault(String audioId, String classifierName, String classifierVersion, JSONArray detectionsArr) {
        try {
            Long audioTimeStamp = Long.parseLong(audioId);
            String detectionJsobBlobDir = Environment.getExternalStorageDirectory().toString() + "/rfcx/vault/detections/" + (new SimpleDateFormat("yyyy-MM-dd", Locale.US)).format(new Date(audioTimeStamp));
            FileUtils.initializeDirectoryRecursively(detectionJsobBlobDir, true);
            String detectionJsobBlobFilePath = detectionJsobBlobDir + "/" + classifierName + "-v" + classifierVersion + "_" + (new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US)).format(new Date(audioTimeStamp)) + ".json";
            StringUtils.saveStringToFile(detectionsArr.toString(), detectionJsobBlobFilePath);
            FileUtils.gZipFile(detectionJsobBlobFilePath, detectionJsobBlobFilePath + ".gz");
            FileUtils.delete(detectionJsobBlobFilePath);
            if (FileUtils.exists(detectionJsobBlobFilePath + ".gz")) {
                Log.i(logTag, "Detection blob has been archived to " + detectionJsobBlobFilePath);
            } else {
                Log.e(logTag, "Detection blob archive process failed...");
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public boolean activateClassifier(String clsfrId) {

        try {

            if (app.assetLibraryDb.dbClassifier.getCountByAssetId(clsfrId) == 0) {
                Log.e(logTag, "Classifier could not be activated because it does not exist in the Asset Library.");

            } else {

                String[] libraryEntry = app.assetLibraryDb.dbClassifier.getSingleRowById(clsfrId);

                if (libraryEntry[0] != null) {

                    String clsfrFormat = libraryEntry[3];
                    String clsfrDigest = libraryEntry[4];
                    String clsfrLibraryFilePath = libraryEntry[5];
                    String clsfrActiveFilePath = RfcxClassifierFileUtils.getClassifierFileLocation_Active(app.getApplicationContext(), Long.parseLong(clsfrId));

                    JSONObject jsonMeta = new JSONObject(libraryEntry[7]);

                    String clsfrName = jsonMeta.getString("classifier_name");
                    String clsfrVersion = jsonMeta.getString("classifier_version");
                    int clsfrSampleRate = Integer.parseInt(jsonMeta.getString("sample_rate"));
                    double clsfrInputGain = Double.parseDouble(jsonMeta.getString("input_gain"));
                    String clsfrWindowSize = jsonMeta.getString("window_size");
                    String clsfrStepSize = jsonMeta.getString("step_size");
                    String clsfrClassifications = jsonMeta.getString("classifications");
                    String clsfrThreshold = jsonMeta.getString("classifications_filter_threshold");

                    app.audioClassifierDb.dbActive.insert(clsfrId, clsfrName, clsfrVersion, clsfrFormat, clsfrDigest, clsfrActiveFilePath, clsfrSampleRate, clsfrInputGain, clsfrWindowSize, clsfrStepSize, clsfrClassifications, clsfrThreshold);

                    FileUtils.delete(clsfrActiveFilePath);
                    FileUtils.copy(clsfrLibraryFilePath, clsfrActiveFilePath);

                    boolean result = FileUtils.sha1Hash(clsfrActiveFilePath).equalsIgnoreCase(clsfrDigest);
                    if (result) {
                        addClassificationToPrefs(clsfrId);
                    }
                    return result;
                }
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
        return false;
    }

    public void addClassificationToPrefs(String classifierId) {
        String[] currentClasses = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CLASSIFY_CLASS).split(",");
        ArrayList<String> currentClassesList = new ArrayList<>(Arrays.asList(currentClasses));
        if (currentClassesList.size() == 1 && currentClassesList.get(0).equalsIgnoreCase("")) {
            currentClassesList.clear();
        }
        String[] active = app.audioClassifierDb.dbActive.getSingleRowById(classifierId);
        if (active != null) {
            String classification = active[11].split(",")[0];
            if (!currentClassesList.contains(classification)) {
                currentClassesList.add(classification);
            }
            app.setSharedPref(RfcxPrefs.Pref.AUDIO_CLASSIFY_CLASS, TextUtils.join(",", currentClassesList));
        }
    }

    public void removeClassificationFromPrefs(String classifierId) {
        String[] active = app.audioClassifierDb.dbActive.getSingleRowById(classifierId);
        String[] currentClasses = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CLASSIFY_CLASS).split(",");
        ArrayList<String> currentClassesList = new ArrayList<>(Arrays.asList(currentClasses));
        if (active != null) {
            String classification = active[11].split(",")[0];
            if (currentClassesList.contains(classification)) {
                int indexOfClass = currentClassesList.indexOf(classification);
                if (indexOfClass != -1) {
                    currentClassesList.remove(indexOfClass);
                }
                app.setSharedPref(RfcxPrefs.Pref.AUDIO_CLASSIFY_CLASS, TextUtils.join(",", currentClassesList));
            }
        }
    }

    public boolean deActivateClassifier(String clsfrId) {

        String clsfrActiveFilePath = RfcxClassifierFileUtils.getClassifierFileLocation_Active(app.getApplicationContext(), Long.parseLong(clsfrId));
        FileUtils.delete(clsfrActiveFilePath);

        removeClassificationFromPrefs(clsfrId);
        app.audioClassifierDb.dbActive.deleteSingleRow(clsfrId);

        return (app.audioClassifierDb.dbActive.getCountByAssetId(clsfrId) == 0);
    }

    public boolean isClassifyAllowedAtThisTimeOfDay(String classifierId) {
        for (String offHoursRange : TextUtils.split(app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.AUDIO_CLASSIFY_SCHEDULE_OFF_HOURS), ",")) {
            String[] offHours = TextUtils.split(offHoursRange, "-");
            if (DateTimeUtils.isTimeStampWithinTimeRange(new Date(), offHours[0], offHours[1])) {
                Log.d(logTag, "Audio Classifier (" + classifierId + ") not allowed at this time of day...");
                return false;
            }
        }
        return true;
    }

}
