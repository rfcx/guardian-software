package org.rfcx.guardian.audio.capture;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class AudioCapture {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioCapture.class.getSimpleName();

	private RfcxGuardian app = null;
	
	public String captureDir = null;
	
	public void initializeAudioDirectories(RfcxGuardian app) {
		
		this.app = app;
		
		String appFilesDir = app.getApplicationContext().getFilesDir().toString();

		(new File(app.audioEncode.sdCardFilesDir)).mkdirs();
		(new File(app.audioEncode.finalFilesDir)).mkdirs();
		
		if ((new File(app.audioEncode.sdCardFilesDir)).isDirectory()) { app.audioEncode.finalFilesDir = app.audioEncode.sdCardFilesDir; }
				
		this.captureDir = appFilesDir+"/capture"; (new File(this.captureDir)).mkdirs();
		app.audioEncode.encodeDir = appFilesDir+"/encode"; (new File(app.audioEncode.encodeDir)).mkdirs();
		app.audioEncode.postZipDir = app.audioEncode.finalFilesDir+"/audio"; (new File(app.audioEncode.postZipDir)).mkdirs();
		
	}
	
	public void cleanupCaptureDirectory() {
		for (File file : (new File(this.captureDir)).listFiles()) {
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
