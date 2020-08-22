package org.rfcx.guardian.guardian.audio.encode;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.audio.EncodeStatus;
import org.rfcx.guardian.audio.flac.FLACStreamEncoder;
import org.rfcx.guardian.audio.opus.OpusAudioEncoder;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AudioEncodeUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioEncodeUtils");

	public static int encodeAudioFile(File preEncodeFile, File postEncodeFile, String encodeCodec, int encodeSampleRate, int encodeBitRate, int encodeQuality) {

		int encodeOutputBitRate = -1;

		if (preEncodeFile.exists()) {
			try {
				if (encodeCodec.equalsIgnoreCase("opus")) {

					OpusAudioEncoder opusEncoder = new OpusAudioEncoder();
					EncodeStatus encStatus = opusEncoder.transcode(preEncodeFile, postEncodeFile, encodeBitRate, encodeQuality);
					if (encStatus == EncodeStatus.OK) { encodeOutputBitRate = encodeBitRate; }
					Log.d(logTag, "OPUS Encoding Complete: "+encStatus.name());

				} else if (encodeCodec.equalsIgnoreCase("flac")) {

					FLACStreamEncoder flacStreamEncoder = new FLACStreamEncoder();
					EncodeStatus encStatus = flacStreamEncoder.encode(preEncodeFile, postEncodeFile, encodeSampleRate, RfcxAudioUtils.AUDIO_CHANNEL_COUNT, RfcxAudioUtils.AUDIO_SAMPLE_SIZE);
					if (encStatus == EncodeStatus.OK) { encodeOutputBitRate = 0; }
					Log.d(logTag, "FLAC Encoding Complete: "+encStatus.name());

				} else {

					FileUtils.copy(preEncodeFile, postEncodeFile);
					encodeOutputBitRate = encodeBitRate;

				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return encodeOutputBitRate;
	}

	public static void cleanupEncodeDirectory(Context context, List<String[]> queuedForEncode) {

		ArrayList<String> audioQueuedForEncode = new ArrayList<String>();
		for (String[] queuedRow : queuedForEncode) {
			audioQueuedForEncode.add(queuedRow[9]);
		}

		FileUtils.deleteDirectoryContents(RfcxAudioUtils.audioEncodeDir(context), audioQueuedForEncode);
	}
}
