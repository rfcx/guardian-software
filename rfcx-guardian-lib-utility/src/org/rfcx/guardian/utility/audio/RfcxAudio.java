package org.rfcx.guardian.utility.audio;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.rfcx.guardian.utility.FileUtils;

import android.content.Context;
import android.os.Environment;

public class RfcxAudio {

	private static final String logTag = "Rfcx-Utils-"+RfcxAudio.class.getSimpleName();
	
	public static final int AUDIO_SAMPLE_SIZE = 16;
	public static final int AUDIO_CHANNEL_COUNT = 1;
	
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM/dd-a", Locale.US);
	
	public static void initializeAudioDirectories(Context context) {
		(new File(captureDir(context))).mkdirs(); FileUtils.chmod(captureDir(context), 0777);
		(new File(encodeDir(context))).mkdirs(); FileUtils.chmod(encodeDir(context), 0777);
		(new File(sdCardFilesDir())).mkdirs(); FileUtils.chmod(sdCardFilesDir(), 0777);
		(new File(finalFilesDir(context))).mkdirs(); FileUtils.chmod(finalFilesDir(context), 0777);
		(new File(postZipDir(context))).mkdirs(); FileUtils.chmod(postZipDir(context), 0777);
	}
	
	private static String sdCardFilesDir() {
		return (new StringBuilder()).append(Environment.getExternalStorageDirectory().toString()).append("/rfcx/audio").toString(); 
	}
	
	private static String finalFilesDir(Context context) {
		if ((new File(sdCardFilesDir())).isDirectory()) {
			return sdCardFilesDir();
		} else {
			return (new StringBuilder()).append(context.getFilesDir().toString()).append("/audio/final").toString();
		}
	}

	private static String postZipDir(Context context) {
		return finalFilesDir(context);
//		return (new StringBuilder()).append(finalFilesDir()).append("/audio").toString(); 
	}
	
	public static String captureDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/audio/capture").toString(); 
	}

	public static String encodeDir(Context context) {
		return (new StringBuilder()).append(context.getFilesDir().toString()).append("/audio/encode").toString(); 
	}
	
	public static String getAudioFileLocation_Capture(Context context, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(captureDir(context)).append("/").append(timestamp).append(".").append(fileExtension).toString(); 
	}
	
	public static String getAudioFileLocation_PreEncode(Context context, long timestamp, String fileExtension) {
		return (new StringBuilder()).append(encodeDir(context)).append("/").append(timestamp).append(".").append(fileExtension).toString(); 
	}
	
	public static String getAudioFileLocation_PostEncode(Context context, long timestamp, String audioCodec) {
		return (new StringBuilder()).append(encodeDir(context)).append("/_").append(timestamp).append(".").append(getFileExtension(audioCodec)).toString(); 
	}

	public static String getAudioFileLocation_Complete_PostZip(Context context, long timestamp, String audioCodec) {
		return (new StringBuilder()).append(postZipDir(context)).append("/").append(dateFormat.format(new Date(timestamp))).append("/").append(timestamp).append(".").append(getFileExtension(audioCodec)).append(".gz").toString(); 
	}
	
	public static String getFileExtension(String audioCodecOrFileExtension) {

		if 	(		audioCodecOrFileExtension.equalsIgnoreCase("opus")
				||	audioCodecOrFileExtension.equalsIgnoreCase("flac")
				||	audioCodecOrFileExtension.equalsIgnoreCase("mp3")
			) {
			
			return audioCodecOrFileExtension;
			
		} else {
			
			return "wav";
			
		}
	}
	
	public static boolean isEncodedWithVbr(String audioCodecOrFileExtension) {
		return (	audioCodecOrFileExtension.equalsIgnoreCase("opus")
				||	audioCodecOrFileExtension.equalsIgnoreCase("flac")
				||	audioCodecOrFileExtension.equalsIgnoreCase("mp3")
				);
	}
	
}
