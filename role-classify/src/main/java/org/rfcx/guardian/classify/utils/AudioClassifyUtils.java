package org.rfcx.guardian.classify.utils;

import android.content.Context;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.classify.model.AudioClassifier;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioClassifyUtils {


    public static final int CLASSIFY_FAILURE_SKIP_THRESHOLD = 3;
    public static final int DETECTION_SEND_FAILURE_SKIP_THRESHOLD = 10;
    private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils");
    private final RfcxGuardian app;
    private final Map<String, AudioClassifier> classifiers = new HashMap<String, AudioClassifier>();
    private final Map<String, String[]> classifierClassifications = new HashMap<String, String[]>();
    private final Map<String, Integer> classifierSampleRates = new HashMap<String, Integer>();
    private final Map<String, Float> classifierWindowSizes = new HashMap<String, Float>();
    private final Map<String, Float> classifierStepSizes = new HashMap<String, Float>();
    public AudioClassifyUtils(Context context) {
        this.app = (RfcxGuardian) context.getApplicationContext();
        RfcxAudioFileUtils.initializeAudioDirectories(context);
        RfcxClassifierFileUtils.initializeClassifierDirectories(context);
    }

    public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

        ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
        for (String[] queuedRow : queuedForClassification) {
            audioQueuedForClassification.add(queuedRow[6]);
        }

        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(new String[]{RfcxAudioFileUtils.audioClassifyDir(context)}, audioQueuedForClassification, Math.round(maxAgeInMilliseconds / 60000), false, false);
    }

    public static void cleanupClassifierDirectory(Context context, String[] excludeFilePaths, long maxAgeInMilliseconds) {

        ArrayList<String> excludeFilePathList = new ArrayList<String>();
        for (String filePath : excludeFilePaths) {
            excludeFilePathList.add(filePath);
        }

        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(new String[]{RfcxClassifierFileUtils.classifierActiveDir(context)}, excludeFilePathList, Math.round(maxAgeInMilliseconds / 60000), false, false);
    }

    public static void cleanupSnippetDirectory(Context context, List<String[]> audioSnippetsQueued, long maxAgeInMilliseconds) {

        ArrayList<String> audioSnippets = new ArrayList<String>();
        for (String[] queuedRow : audioSnippetsQueued) {
            audioSnippets.add(queuedRow[6]);
        }

        (new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup(new String[]{RfcxAudioFileUtils.audioSnippetDir(context)}, audioSnippets, Math.round(maxAgeInMilliseconds / 60000), false, false);
    }

    public boolean confirmOrLoadClassifier(String classifierId, String tfLiteFilePath, int sampleRate, float windowSize, float stepSize, String classificationsStr) {

        String clsfrId = classifierId.toLowerCase(Locale.US);


        if (!this.classifiers.containsKey(clsfrId)) {

            AudioClassifier audioClassifier = new AudioClassifier(tfLiteFilePath, sampleRate, windowSize, stepSize, ArrayUtils.toList(classificationsStr.split(",")));
            audioClassifier.loadClassifier();

            this.classifierClassifications.put(clsfrId, classificationsStr.split(","));
            this.classifierSampleRates.put(clsfrId, sampleRate);
            this.classifierWindowSizes.put(clsfrId, windowSize);
            this.classifierStepSizes.put(clsfrId, stepSize);
            this.classifiers.put(clsfrId, audioClassifier);

        }

        return this.classifiers.containsKey(clsfrId);
    }

    public AudioClassifier getClassifier(String classifierId) {
        String clsfrId = classifierId.toLowerCase(Locale.US);
        if (this.classifiers.containsKey(clsfrId)) {
            return this.classifiers.get(clsfrId);
        }
        return null;
    }

    private String[] getClassifierClassifications(String classifierId) {
        String clsfrId = classifierId.toLowerCase(Locale.US);
        if (this.classifierClassifications.containsKey(clsfrId)) {
            return this.classifierClassifications.get(clsfrId);
        }
        return null;
    }

    private int getClassifierSampleRate(String classifierId) {
        String clsfrId = classifierId.toLowerCase(Locale.US);
        if (this.classifierSampleRates.containsKey(clsfrId)) {
            return this.classifierSampleRates.get(clsfrId);
        }
        return 0;
    }

    private float getClassifierWindowSize(String classifierId) {
        String clsfrId = classifierId.toLowerCase(Locale.US);
        if (this.classifierWindowSizes.containsKey(clsfrId)) {
            return this.classifierWindowSizes.get(clsfrId);
        }
        return 0;
    }

    private float getClassifierStepSize(String classifierId) {
        String clsfrId = classifierId.toLowerCase(Locale.US);
        if (this.classifierStepSizes.containsKey(clsfrId)) {
            return this.classifierStepSizes.get(clsfrId);
        }
        return 0;
    }

    public JSONObject classifyOutputAsJson(String classifierId, String audioId, long audioStartsAt, List<float[]> classifierOutput) throws JSONException {

        String[] classifierClassifications = app.audioClassifyUtils.getClassifierClassifications(classifierId);
        int classifierSampleRate = app.audioClassifyUtils.getClassifierSampleRate(classifierId);
        float classifierWindowSize = app.audioClassifyUtils.getClassifierWindowSize(classifierId);
        float classifierStepSize = app.audioClassifyUtils.getClassifierStepSize(classifierId);

        JSONObject jsonObj = new JSONObject();

        jsonObj.put("classifier_id", classifierId);
        jsonObj.put("audio_id", audioId);

        jsonObj.put("sample_rate", classifierSampleRate + "");
        jsonObj.put("window_size", String.format(Locale.US, "%.4f", classifierWindowSize));
        jsonObj.put("step_size", String.format(Locale.US, "%.4f", classifierStepSize));

        jsonObj.put("starts_at", audioStartsAt + "");

        JSONObject jsonDetections = new JSONObject();
        for (int j = 0; j < classifierClassifications.length; j++) {
            JSONArray classArr = new JSONArray();
            for (int i = 0; i < classifierOutput.size(); i++) {
                classArr.put(String.format(Locale.US, "%.6f", classifierOutput.get(i)[j]));
            }
            jsonDetections.put(classifierClassifications[j], classArr);
        }

        jsonObj.put("detections", jsonDetections);
        return jsonObj;
    }

    public void sendClassifyOutputToGuardianRole(String jsonObjStr) {

        Cursor sendDetectionsResponse =
                app.getResolver().query(
                        RfcxComm.getUri("guardian", "detections_create", RfcxComm.urlEncode(StringUtils.stringToGZipBase64(jsonObjStr))),
                        RfcxComm.getProjection("guardian", "detections_create"),
                        null, null, null);
        if (sendDetectionsResponse != null) {
            sendDetectionsResponse.close();
        }
    }

}
