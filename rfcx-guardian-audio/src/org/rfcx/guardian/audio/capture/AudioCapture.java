package org.rfcx.guardian.audio.capture;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.database.AudioDb;
import org.rfcx.guardian.utility.RfcxConstants;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

public class AudioCapture {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioCapture.class.getSimpleName();

	private RfcxGuardian app = null;
	
	public String captureDir = null;
	
	// encode straight to AAC/M4A (lossy, constant bitrate)...
	// ...or don't, and encode asynchronously after
	// (*may* eventually support various lossy formats for post-encoding)
	public static final boolean ENCODE_ON_CAPTURE = true;
	
	public final static int CAPTURE_SAMPLE_RATE_HZ = 8000;
	
	// TO DO: These need to be made dynamic, ideally tied to prefs (cross role)
	public int pauseCaptureIfBatteryPercentageIsBelow = 50;
	
	public void initializeAudioDirectories(RfcxGuardian app) {
		
		this.app = app;
		
		String appFilesDir = app.getApplicationContext().getFilesDir().toString();
		String finalFilesDir = "/cache/download/rfcx";
		(new File(app.audioEncode.sdCardFilesDir)).mkdirs();
		
		if ((new File(app.audioEncode.sdCardFilesDir)).isDirectory()) { finalFilesDir = app.audioEncode.sdCardFilesDir; }
				
		this.captureDir = appFilesDir+"/capture"; (new File(this.captureDir)).mkdirs();
		app.audioEncode.encodeDir = appFilesDir+"/encode"; (new File(app.audioEncode.encodeDir)).mkdirs();
		app.audioEncode.preEncodeDir = finalFilesDir+"/m4a"; (new File(app.audioEncode.preEncodeDir)).mkdirs();
		
	}
	
	public void cleanupCaptureDirectory() {
		for (File file : (new File(this.captureDir)).listFiles()) {
			try { file.delete(); } catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
		}
	}

	public boolean isBatteryChargeSufficientForCapture() {
		int batteryCharge = this.app.deviceBattery.getBatteryChargePercentage(app.getApplicationContext(), null);
		return (batteryCharge >= this.pauseCaptureIfBatteryPercentageIsBelow);
	}

}
