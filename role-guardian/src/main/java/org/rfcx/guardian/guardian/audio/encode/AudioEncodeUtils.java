package org.rfcx.guardian.guardian.audio.encode;

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

public class AudioEncodeUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioEncodeUtils");

	public static final SimpleDateFormat vaultStatsDayId = new SimpleDateFormat("yyyy_MM_dd", Locale.US);

	public static final int ENCODE_QUALITY = 10;
	public static final int ENCODE_FAILURE_SKIP_THRESHOLD = 3;

	public static int encodeAudioFile(File preEncodeFile, File postEncodeFile, String encodeCodec, int encodeSampleRate, int encodeBitRate, int encodeQuality) {

		int encodeOutputBitRate = -1;

		if (preEncodeFile.exists()) {
			try {
				if (encodeCodec.equalsIgnoreCase("opus")) {

					OpusAudioEncoder opusEncoder = new OpusAudioEncoder();
					EncodeStatus encStatus = opusEncoder.transcode(preEncodeFile, postEncodeFile, encodeBitRate, encodeQuality);
					if (encStatus == EncodeStatus.OK) { encodeOutputBitRate = encodeBitRate; }
				//	Log.d(logTag, "OPUS Encoding Complete: "+encStatus.name());

				} else if (encodeCodec.equalsIgnoreCase("flac")) {

					FLACStreamEncoder flacStreamEncoder = new FLACStreamEncoder();
					EncodeStatus encStatus = flacStreamEncoder.encode(preEncodeFile, postEncodeFile, encodeSampleRate, RfcxAudioFileUtils.AUDIO_CHANNEL_COUNT, RfcxAudioFileUtils.AUDIO_SAMPLE_SIZE);
					if (encStatus == EncodeStatus.OK) { encodeOutputBitRate = 0; }
				//	Log.d(logTag, "FLAC Encoding Complete: "+encStatus.name());

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

	public static void cleanupEncodeDirectory(Context context, List<String[]> queuedForEncode, long maxAgeInMilliseconds) {

		ArrayList<String> audioQueuedForEncode = new ArrayList<String>();
		for (String[] queuedRow : queuedForEncode) {
			audioQueuedForEncode.add(queuedRow[10]);
		}

		(new RfcxAssetCleanup(RfcxGuardian.APP_ROLE)).runFileSystemAssetCleanup( new String[]{ RfcxAudioFileUtils.audioEncodeDir(context) }, audioQueuedForEncode, Math.round(maxAgeInMilliseconds/60000), false, false );
	}


	public static boolean sendEncodedAudioToVault(String audioTimestamp, File preVaultFile, File vaultFile) throws IOException {

		FileUtils.copy(preVaultFile, vaultFile);
		FileUtils.delete(preVaultFile);
		if (FileUtils.exists(vaultFile)) {
			Log.d(logTag, "Audio saved to Vault: "+audioTimestamp+", "+FileUtils.bytesAsReadableString(FileUtils.getFileSizeInBytes(vaultFile))+", "+vaultFile.getAbsolutePath());
			return true;
		} else {
			Log.e(logTag, "Final encoded file not found: "+vaultFile.getAbsolutePath());
		}
		return false;
	}
}
