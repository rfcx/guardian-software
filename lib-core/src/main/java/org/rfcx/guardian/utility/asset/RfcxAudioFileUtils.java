package org.rfcx.guardian.utility.asset;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;

import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class RfcxAudioFileUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", "RfcxAudioFileUtils");

	private static final SimpleDateFormat dirDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd/HH", Locale.US);
	private static final SimpleDateFormat dirDateTimeFormat_DayOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static final SimpleDateFormat fileDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss.SSSZZZ", Locale.US);

	public static final int AUDIO_SAMPLE_SIZE = 16;
	public static final int AUDIO_CHANNEL_COUNT = 1;
	
	public static void initializeAudioDirectories(Context context) {

		FileUtils.initializeDirectoryRecursively(audioSdCardDir(), true);
		FileUtils.initializeDirectoryRecursively(audioVaultDir(), true);

		FileUtils.initializeDirectoryRecursively(audioCaptureDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioEncodeDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioFinalDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioQueueDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioStashDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioSnippetDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioClassifyDir(context), false);
		FileUtils.initializeDirectoryRecursively(audioLibraryDir(context), false);
	}

	private static String audioSdCardDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/audio";
	}

	private static String audioVaultDir() {
		return Environment.getExternalStorageDirectory().toString() + "/rfcx/vault/audio";
	}

	public static String audioCacheDir(Context context) {
		return context.getFilesDir().toString() + "/audio/cache";
	}

	public static String audioLibraryDir(Context context) {
		return context.getFilesDir().toString() + "/audio/library";
	}

	public static String audioClassifyDir(Context context) {
		return context.getFilesDir().toString() + "/audio/classify";
	}
	
	public static String audioCaptureDir(Context context) {
		return context.getFilesDir().toString() + "/audio/capture";
	}

	public static String audioEncodeDir(Context context) {
		return context.getFilesDir().toString() + "/audio/encode";
	}

	public static String audioFinalDir(Context context) {
		return context.getFilesDir().toString() + "/audio/final";
	}

	public static String audioQueueDir(Context context) {
		return context.getFilesDir().toString() + "/audio/queue";
	}

	public static String audioStashDir(Context context) {
		return context.getFilesDir().toString() + "/audio/stash";
	}

	public static String audioSnippetDir(Context context) {
		return context.getFilesDir().toString() + "/audio/snippet";
	}
	
//	public static String getAudioFileLocation_Capture(Context context, long timestamp, String fileExtension) {
//		return audioCaptureDir(context) + "/" + timestamp + "." + fileExtension;
//	}

	public static String getAudioFileName(String originTag, long timestamp, String audioCodec, int audioLength, int sampleRate) {
		return 	originTag
				+ "_" + fileDateTimeFormat.format(new Date(timestamp))
				+ sampleRateTag(sampleRate)
				+ audioLengthTag(audioLength, true)
				+ "." + getFileExt(audioCodec);
	}

	public static String getAudioFileLocation_PreClassify(Context context, long timestamp, String fileExtension, int sampleRate, String extraTagLabel) {
		return audioClassifyDir(context) + "/" + timestamp + sampleRateTag(sampleRate) + miscTag(extraTagLabel) + "." + fileExtension;
	}

	public static String getAudioFileLocation_PreEncode(Context context, long timestamp, String fileExtension, int sampleRate, String extraTagLabel) {
		return audioEncodeDir(context) + "/" + timestamp + sampleRateTag(sampleRate) + miscTag(extraTagLabel) + "." + fileExtension;
	}
	
	public static String getAudioFileLocation_PostEncode(Context context, long timestamp, String audioCodec, int sampleRate, String extraTagLabel) {
		return audioEncodeDir(context) + "/_" + timestamp + sampleRateTag(sampleRate) + miscTag(extraTagLabel) + "." + getFileExt(audioCodec);
	}

	public static String getAudioFileLocation_GZip(String rfcxDeviceId, Context context, long timestamp, String audioCodec) {
		return audioFinalDir(context) + "/" + dirDateTimeFormat.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, 0,0) + ".gz";
	}

	public static String getAudioFileLocation_Queue(String rfcxDeviceId, Context context, long timestamp, String audioCodec) {
		return audioQueueDir(context) + "/" + dirDateTimeFormat.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, 0,0) + ".gz";
	}

	public static String getAudioFileLocation_Stash(String rfcxDeviceId, Context context, long timestamp, String audioCodec) {
		return audioStashDir(context) + "/" + dirDateTimeFormat.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, 0,0) + ".gz";
	}

	public static String getAudioFileLocation_Snippet(String rfcxDeviceId, Context context, long timestamp, String audioCodec) {
		return audioSnippetDir(context) + "/" + dirDateTimeFormat.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, 0,0);
	}

	public static String getAudioFileLocation_Cache(Context context, long timestamp, String audioCodec) {
		return audioCacheDir(context) + "/" + timestamp + "." + getFileExt(audioCodec);
	}

	public static String getAudioFileLocation_Library(Context context, long timestamp, String audioCodec) {
		return audioLibraryDir(context) + "/" + dirDateTimeFormat_DayOnly.format(new Date(timestamp)) + "/" + getAudioFileName("library", timestamp, audioCodec, 0, 0);
	}

	public static String getAudioFileLocation_Vault(String rfcxDeviceId, long timestamp, String audioCodec, int audioLength, int sampleRate) {
		return audioVaultDir() + "/" + dirDateTimeFormat_DayOnly.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, audioLength, sampleRate);
	}

	public static String getAudioFileLocation_ExternalStorage(String rfcxDeviceId, long timestamp, String audioCodec) {
		return audioSdCardDir() + "/" + dirDateTimeFormat.format(new Date(timestamp)) + "/" + getAudioFileName(rfcxDeviceId, timestamp, audioCodec, 0,0) + ".gz";
	}
	
	public static String getFileExt(String audioCodecOrFileExtension) {
		if 	(audioCodecOrFileExtension.equalsIgnoreCase("opus") || audioCodecOrFileExtension.equalsIgnoreCase("flac")) {
			return audioCodecOrFileExtension;
		} else {
			return "wav";
		}
	}
	
	public static boolean isEncodedWithVbr(String audioCodecOrFileExtension) {
		return (audioCodecOrFileExtension.equalsIgnoreCase("opus") || audioCodecOrFileExtension.equalsIgnoreCase("flac"));
	}

	private static String sampleRateTag(int sampleRate) {
		return ((sampleRate == 0) ? "" : "_" + (Math.round(((double) sampleRate)/1000) + "kHz"));
	}

	private static String audioLengthTag(int audioLength, boolean useFractionalTime) {
		return ((audioLength == 0) ? "" : "_" + (
				((useFractionalTime) ? String.format(Locale.US, "%.3f", (((double) audioLength)/1000)) : Math.round(((double) audioLength)/1000))
				+ "secs")
		);
	}

	private static String miscTag(String tagLabel) {
		return (((tagLabel == null) || (tagLabel.length() == 0)) ? "" : "_" + tagLabel);
	}
	
}
