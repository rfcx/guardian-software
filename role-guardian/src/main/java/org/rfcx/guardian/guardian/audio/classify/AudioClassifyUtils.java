package org.rfcx.guardian.guardian.audio.classify;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.guardian.asset.AudioDetectionFilterJobService;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AudioClassifyUtils {

	public AudioClassifyUtils(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		RfcxClassifierFileUtils.initializeClassifierDirectories(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils");

	private final RfcxGuardian app;

	public static final int CLASSIFY_FAILURE_SKIP_THRESHOLD = 3;

	public void queueClassifyJobAcrossRoles(String audioId, String classifierId, String classifierVersion, int classifierSampleRate, String audioFilePath, String classifierFilePath, String classifierWindowSize, String classifierStepSize, String classifierClasses) {

		try {
			String classifyJobUrlBlob = TextUtils.join("|", new String[]{
					RfcxComm.urlEncode(audioId),
					RfcxComm.urlEncode(classifierId),
					RfcxComm.urlEncode(classifierVersion),
					classifierSampleRate+"",
					RfcxComm.urlEncode(audioFilePath),
					RfcxComm.urlEncode(classifierFilePath),
					RfcxComm.urlEncode(classifierWindowSize),
					RfcxComm.urlEncode(classifierStepSize),
					RfcxComm.urlEncode(classifierClasses)
				});

			Cursor classifyQueueResponse = app.getResolver().query(
							RfcxComm.getUri("classify", "classify_queue", classifyJobUrlBlob),
							RfcxComm.getProjection("classify", "classify_queue"),
							null, null, null);
			if (classifyQueueResponse != null) { classifyQueueResponse.close(); }

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

			Log.d(logTag, "Classify Detections Received: Audio: " + audioId + ", Classifier: " + classifierId );

			String[] classiferLibraryInfo = app.assetLibraryDb.dbClassifier.getSingleRowById(classifierId);
			JSONObject classiferJsonMeta = new JSONObject(classiferLibraryInfo[7]);
			String classifierName = classiferJsonMeta.getString("classifier_name");
			String classifierVersion = classiferJsonMeta.getString("classifier_version");

			for (Iterator<String> classificationNames = jsonObj.getJSONObject("detections").keys(); classificationNames.hasNext(); ) {
				String classificationTag = classificationNames.next();
				JSONArray detections = jsonObj.getJSONObject("detections").getJSONArray(classificationTag);
				app.audioDetectionDb.dbUnfiltered.insert(
						classificationTag, classifierId, classifierName, classifierVersion, "-",
						audioId, audioId, windowSize, stepSize, detections.toString()
				);
			}

			// save classify job stats
			long classifyJobDuration = Long.parseLong(jsonObj.getString("classify_duration"));
			long classifyAudioSize = Long.parseLong(jsonObj.getString("audio_size"));
			app.latencyStatsDb.dbClassifyLatency.insert(classifierName + "-v" + classifierVersion, classifyJobDuration, classifyAudioSize);

			app.rfcxSvc.triggerService( AudioDetectionFilterJobService.SERVICE_NAME, false);

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

					app.audioClassifierDb.dbActive.insert(clsfrId, clsfrName, clsfrVersion, clsfrFormat, clsfrDigest, clsfrActiveFilePath, clsfrSampleRate, clsfrInputGain, clsfrWindowSize, clsfrStepSize, clsfrClassifications);

					FileUtils.delete(clsfrActiveFilePath);
					FileUtils.copy(clsfrLibraryFilePath, clsfrActiveFilePath);

					return FileUtils.sha1Hash(clsfrActiveFilePath).equalsIgnoreCase(clsfrDigest);
				}
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return false;
	}

	public boolean deActivateClassifier(String clsfrId) {

		String clsfrActiveFilePath = RfcxClassifierFileUtils.getClassifierFileLocation_Active(app.getApplicationContext(), Long.parseLong(clsfrId));
		FileUtils.delete(clsfrActiveFilePath);

		app.audioClassifierDb.dbActive.deleteSingleRow(clsfrId);

		return (app.audioClassifierDb.dbActive.getCountByAssetId(clsfrId) == 0);
	}


	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[6]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
