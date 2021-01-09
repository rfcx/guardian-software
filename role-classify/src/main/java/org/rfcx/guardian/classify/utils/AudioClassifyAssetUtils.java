package org.rfcx.guardian.classify.utils;

import android.content.Context;

import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class AudioClassifyAssetUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyAssetUtils");

	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[10]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}

}
