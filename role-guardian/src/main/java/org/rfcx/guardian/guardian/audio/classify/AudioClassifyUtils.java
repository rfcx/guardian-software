package org.rfcx.guardian.guardian.audio.classify;

import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
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


	public void createDummyRow() {

		long classifierId = System.currentTimeMillis();

		app.audioClassifierDb.dbActive.insert(
				""+classifierId,
				"1",
				"tflite",
				"asdfasdf",
				RfcxClassifierFileUtils.getClassifierFileLocation_Active(app.getApplicationContext(), classifierId, "threat", "1"),
				12000,
				"0.975",
				"1",
				"chainsaw,gunshot,vehicle"
				);
	}




	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[5]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
