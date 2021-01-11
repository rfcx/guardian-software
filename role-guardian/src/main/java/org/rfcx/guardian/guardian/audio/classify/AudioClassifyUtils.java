package org.rfcx.guardian.guardian.audio.classify;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
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

			Cursor classifyQueueContentProviderResponse =
					app.getResolver().query(
							RfcxComm.getUri("classify", "classify_queue", classifyJobUrlBlob),
							RfcxComm.getProjection("classify", "classify_queue"),
							null, null, null);
			classifyQueueContentProviderResponse.close();

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}


	public void parseIncomingClassificationPayloadAndSave(String classificationPayload) {

		try {
			JSONObject jsonObj = new JSONObject(StringUtils.gZipBase64ToUnGZipString(classificationPayload));

			Log.d(logTag, "Classifications Blob Receieved: " + jsonObj.toString() );

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}


	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[6]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
