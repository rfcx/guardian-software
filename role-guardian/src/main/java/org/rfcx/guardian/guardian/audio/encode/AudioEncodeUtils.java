package org.rfcx.guardian.guardian.audio.encode;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.audio.flac.FLACStreamEncoder;
import org.rfcx.guardian.audio.flac.FLAC_FileEncoder;
import org.rfcx.guardian.audio.opus.OpusAudioEncoder;
import org.rfcx.guardian.utility.audio.RfcxAudioUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class AudioEncodeUtils {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioEncodeUtils");
	public static int bufferSize = 0;

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
					String encStatus = flacStreamEncoder.encode(postEncodeFile, encodeSampleRate, RfcxAudioUtils.AUDIO_CHANNEL_COUNT, RfcxAudioUtils.AUDIO_SAMPLE_SIZE, byteBuffer, bufferSize);

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
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(audio));

		int read;
		byte[] buff = new byte[1024];
		while ((read = in.read(buff)) > 0) {
			out.write(buff, 0, read);
		}
		out.flush();
		return ByteBuffer.wrap(out.toByteArray());
	}

}
