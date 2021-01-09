package org.rfcx.guardian.classify.utils;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.classify.model.AudioClassifier;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AudioClassifyClassicUtils {


	public AudioClassifyClassicUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		RfcxAudioFileUtils.initializeAudioDirectories(context);
		RfcxClassifierFileUtils.initializeClassifierDirectories(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyAssetUtils");

	private final RfcxGuardian app;


	private Map<String, AudioClassifier> classifiers = new HashMap<String, AudioClassifier>();

	private Map<String, String[]> classifierClasses = new HashMap<String, String[]>();
	private Map<String, Integer> classifierSampleRates = new HashMap<String, Integer>();
	private Map<String, Float> classifierWindowSizes = new HashMap<String, Float>();
	private Map<String, Float> classifierSteps = new HashMap<String, Float>();


	public boolean confirmOrLoadClassifier(String classifierId, String tfLiteFilePath, int sampleRate, float windowSize, float step, String outputClassesStr) {

		String clsfrId = classifierId.toLowerCase(Locale.US);


		if (!this.classifiers.containsKey(clsfrId)) {

			AudioClassifier audioClassifier = new AudioClassifier(tfLiteFilePath, sampleRate, windowSize, step, ArrayUtils.toList(outputClassesStr.split(",")));
			audioClassifier.loadClassifier();

			this.classifierClasses.put( clsfrId, outputClassesStr.split(",") );
			this.classifierSampleRates.put( clsfrId, sampleRate);
			this.classifierWindowSizes.put( clsfrId, windowSize);
			this.classifierSteps.put( clsfrId, step);
			this.classifiers.put( clsfrId, audioClassifier );

		}
//			setRunState(svcName, false);
//			setAbsoluteRunState(svcName, false);

		return this.classifiers.containsKey(clsfrId);
	}

	public AudioClassifier getClassifier(String classifierId) {
		String clsfrId = classifierId.toLowerCase(Locale.US);
		if (this.classifiers.containsKey(clsfrId)) {
			return this.classifiers.get(clsfrId);
		}
		return null;
	}

	public String[] getClassifierClasses(String classifierId) {
		String clsfrId = classifierId.toLowerCase(Locale.US);
		if (this.classifierClasses.containsKey(clsfrId)) {
			return this.classifierClasses.get(clsfrId);
		}
		return null;
	}


	public JSONObject classifierOutputAsJson(String classifierId, String audioId, long audioStartsAt, List<float[]> classifierOutput) throws JSONException {

		String[] classifierClasses = app.audioClassifyClassicUtils.getClassifierClasses(classifierId);

		JSONObject jsonObj = new JSONObject();

		jsonObj.put("classifier_id", classifierId);
		jsonObj.put("audio_id", audioId);

		jsonObj.put("starts_at", audioStartsAt);

		JSONObject jsonDetections = new JSONObject();
		for (int j = 0; j < classifierClasses.length; j++) {
			JSONArray classArr = new JSONArray();
			for (int i = 0; i < classifierOutput.size(); i++) {
				classArr.put( Double.parseDouble( String.format("%.3f", classifierOutput.get(i)[j]) ) );
			}
			jsonDetections.put(classifierClasses[j], classArr);
		}
		jsonObj.put("detections", jsonDetections);

		return jsonObj;
	}




	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[10]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
