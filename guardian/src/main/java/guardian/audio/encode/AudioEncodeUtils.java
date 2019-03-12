package guardian.audio.encode;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rfcx.audio.flac.FLAC_FileEncoder;
import rfcx.audio.opus.OpusAudioEncoder;
import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.misc.FileUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.content.Context;
import android.util.Log;
import guardian.RfcxGuardian;

public class AudioEncodeUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AudioEncodeUtils.class);
	
	public static int encodeAudioFile(File preEncodeFile, File postEncodeFile, String encodeCodec, int encodeSampleRate, int encodeBitRate, int encodeQuality) {
		
		int encodeOutputBitRate = -1;
		
		if (preEncodeFile.exists()) {
			try {
				if (encodeCodec.equalsIgnoreCase("opus")) {
					
					OpusAudioEncoder opusEncoder = new OpusAudioEncoder();
					String encStatus = opusEncoder.transcode(preEncodeFile, postEncodeFile, encodeBitRate, encodeQuality);
					if (encStatus.equalsIgnoreCase("OK")) { encodeOutputBitRate = encodeBitRate; }
					Log.d(logTag, "OPUS Encoding Complete: "+encStatus);
					
				} else if (encodeCodec.equalsIgnoreCase("flac")) {
					
					FLAC_FileEncoder flacEncoder = new FLAC_FileEncoder();
					flacEncoder.adjustAudioConfig(encodeSampleRate, RfcxAudioUtils.AUDIO_SAMPLE_SIZE, RfcxAudioUtils.AUDIO_CHANNEL_COUNT);
					FLAC_FileEncoder.Status encStatus = flacEncoder.encode(preEncodeFile, postEncodeFile);
					if (encStatus == FLAC_FileEncoder.Status.FULL_ENCODE) { encodeOutputBitRate = 0; }
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
		
		FileUtils.deleteDirectoryContents(RfcxAudioUtils.encodeDir(context), audioQueuedForEncode);
	}
	
}
