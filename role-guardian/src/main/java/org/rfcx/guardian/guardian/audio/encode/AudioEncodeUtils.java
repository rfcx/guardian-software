package org.rfcx.guardian.guardian.audio.encode;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.audio.flac.FLACStreamEncoder;
import org.rfcx.guardian.audio.opus.OpusAudioEncoder;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
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
					String encStatus = opusEncoder.transcode(preEncodeFile, postEncodeFile, encodeBitRate, encodeQuality);
					if (encStatus.equalsIgnoreCase("OK")) { encodeOutputBitRate = encodeBitRate; }
					Log.d(logTag, "OPUS Encoding Complete: "+encStatus);

				} else if (encodeCodec.equalsIgnoreCase("flac")) {

					FLACStreamEncoder flacStreamEncoder = new FLACStreamEncoder();
					ByteBuffer byteBuffer = audioToByteBuffer(preEncodeFile);
					int buffersize = byteBuffer.position();
					String encStatus = flacStreamEncoder.encode(postEncodeFile, encodeSampleRate, RfcxAudioUtils.AUDIO_CHANNEL_COUNT, RfcxAudioUtils.AUDIO_SAMPLE_SIZE, byteBuffer, buffersize);

					if (encStatus.equalsIgnoreCase("OK")) { encodeOutputBitRate = 0; }
					Log.d(logTag, "FLAC Encoding Complete: "+encStatus);

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

	private static ByteBuffer audioToByteBuffer(File audio) throws IOException {
		FileInputStream in = new FileInputStream(audio);

		byte[] buff = new byte[(int)audio.length()];
		in.read(buff, 0, buff.length);
		Log.d("FLAC_ENCODER", buff.length+"");
		ByteBuffer byteBuffer = ByteBuffer.allocateDirect(buff.length);
		byteBuffer.put(buff);
		Log.d("FLAC_ENCODER", byteBuffer.limit()+"");
		return byteBuffer;
	}

}
