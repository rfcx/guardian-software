package org.rfcx.guardian.encode.utils;

import java.io.File;
import java.util.List;

import org.rfcx.guardian.audio.flac.FLAC_FileEncoder;
import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.audio.AudioFile;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Log;

public class AudioEncodeUtils {

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeUtils.class.getSimpleName();
	
	public static int encodeAudioFile(File preEncodeFile, File postEncodeFile, String encodeCodec, int encodeSampleRate, int encodeBitRate, int encodeQuality) {
		
		int encodeOutputBitRate = -1;
		
		if (preEncodeFile.exists()) {
			try {
				if (encodeCodec.equalsIgnoreCase("opus")) {
					FileUtils.copy(preEncodeFile, postEncodeFile);
					encodeOutputBitRate = 1;
				} else if (encodeCodec.equalsIgnoreCase("mp3")) {
					FileUtils.copy(preEncodeFile, postEncodeFile);
					encodeOutputBitRate = 1;
				} else if (encodeCodec.equalsIgnoreCase("flac")) {
					FLAC_FileEncoder flacEncoder = new FLAC_FileEncoder();
					flacEncoder.adjustAudioConfig(encodeSampleRate, AudioFile.AUDIO_SAMPLE_SIZE, AudioFile.AUDIO_CHANNEL_COUNT);
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
	
	public static void cleanupEncodeDirectory(List<String[]> queuedForEncode) {
		
		for (File file : (new File(AudioFile.encodeDir())).listFiles()) {
			
			boolean isQueuedForEncode = false;
			
			for (String[] queuedRow : queuedForEncode) {
				if (file.getAbsolutePath().equalsIgnoreCase(queuedRow[9])) {
					isQueuedForEncode = true;
				}
			}
			
			if (!isQueuedForEncode) {
				try { 
					file.delete();
					Log.d(logTag, "Deleted "+file.getName()+" from encode directory...");
				} catch (Exception e) { 
					RfcxLog.logExc(logTag, e);
				}
			}
		}
	}
	
}
