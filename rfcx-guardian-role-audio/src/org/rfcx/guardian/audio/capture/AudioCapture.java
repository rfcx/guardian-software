package org.rfcx.guardian.audio.capture;

import java.io.File;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.encode.AudioEncode;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

import android.content.Context;

public class AudioCapture {

	public AudioCapture(Context context) {
		this.app = (RfcxGuardian) context.getApplicationContext();
		initializeAudioDirectories(context);
	}
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioCapture.class.getSimpleName();

	private RfcxGuardian app = null;

	public final static int AUDIO_SAMPLE_RATE = 8000;
	
	private static void initializeAudioDirectories(Context context) {
		(new File(captureDir(context))).mkdirs();
		(new File(AudioEncode.encodeDir(context))).mkdirs();
		(new File(AudioEncode.sdCardFilesDir())).mkdirs();
		(new File(AudioEncode.finalFilesDir())).mkdirs();
		(new File(AudioEncode.postZipDir())).mkdirs();
	}
	
	public static String captureDir(Context context) {
		return context.getFilesDir().toString()+"/capture"; 
	}
	
	public static void cleanupCaptureDirectory(Context context) {
		for (File file : (new File(captureDir(context))).listFiles()) {
			try { 
				file.delete();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e);
			}
		}
	}

	public boolean isBatteryChargeSufficientForCapture() {
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.app.rfcxPrefs.getPrefAsInt("audio_battery_cutoff"));
	}

}
