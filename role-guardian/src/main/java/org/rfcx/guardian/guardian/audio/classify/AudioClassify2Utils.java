package org.rfcx.guardian.guardian.audio.classify;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.audio.EncodeStatus;
import org.rfcx.guardian.audio.flac.FLACStreamEncoder;
import org.rfcx.guardian.audio.opus.OpusAudioEncoder;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AudioClassify2Utils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioClassifyUtils");

	private MLPredictor predictor = new MLPredictor();

	public int stepSize = 12000;
	public float windowSize = (float) 0.975;
	public int finalStepSize = Math.round(stepSize * windowSize);



//	public void runClassifier() {
//
//
//		predictor.run(audioChunk);
//	}
//
//
//
//	public List<float[]> sliceAudio(int sampleRate, int step, float windowSize) {
//
//		List<float[]> slicedAudio = new ArrayList<>();
//
//		int chunkSize = Math.round(sampleRate * windowSize);
//		int startAt = 0;
//		int endAt = chunkSize;
//
//		int stepSize = (step != 0) ? Math.round(chunkSize * (1f / (2 * step))) : 0;
//
//		while ((startAt + chunkSize) < this.size) {
//			if (startAt != 0) {
//				startAt = endAt - stepSize;
//				endAt = startAt + chunkSize;
//			}
//			slicedAudio.add(this.copyOfRange(startAt, endAt))
//			startAt = endAt;
//		}
//
//		return slicedAudio;
//	}








	public static void cleanupClassifyDirectory(Context context, List<String[]> queuedForClassification, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForClassification = new ArrayList<String>();
		for (String[] queuedRow : queuedForClassification) {
			audioQueuedForClassification.add(queuedRow[10]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioClassifyDir(context) }, audioQueuedForClassification, Math.round(maxAgeInMilliseconds/60000), false, false );
	}


}
