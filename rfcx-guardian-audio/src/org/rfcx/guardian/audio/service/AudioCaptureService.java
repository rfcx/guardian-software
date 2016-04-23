package org.rfcx.guardian.audio.service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.audio.capture.AudioCapture;
import org.rfcx.guardian.audio.capture.ExtAudioRecorderModified;
import org.rfcx.guardian.audio.encode.AudioEncode;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class AudioCaptureService extends Service {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+AudioCaptureService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioCaptureSvc audioCaptureSvc;

	private RfcxGuardian app = null;
	private Context context = null;
	
	MediaRecorder mediaRecorder = null;
    ExtAudioRecorderModified audioRecorder = null;

	private MediaRecorder[] mediaRecorderArray = {null,null};
	private ExtAudioRecorderModified[] audioRecorderArray = {null,null};;
    
    FileUtils fileUtils = new FileUtils();
    
	private long captureLoopPeriod;
	private int captureSampleRate;
	
	private int encodingBitRate;
	private String fileExtension;
	private String captureCodec;
	
	private long[] captureTimeStamps = {0,0};
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCaptureSvc = new AudioCaptureSvc();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		
		app = (RfcxGuardian) getApplication();
		context = app.getApplicationContext();

		app.audioCapture.initializeAudioDirectories(app);
		
		Log.v(TAG, "Starting service: "+TAG);
		
		app.isRunning_AudioCapture = true;
		try {
			this.audioCaptureSvc.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isRunning_AudioCapture = false;
		this.audioCaptureSvc.interrupt();
		this.audioCaptureSvc = null;
	}

	private class AudioCaptureSvc extends Thread {

		public AudioCaptureSvc() {
			super("AudioCaptureService-AudioCaptureSvc");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			app = (RfcxGuardian) getApplication();
			
			AudioCapture audioCapture = app.audioCapture;
			AudioEncode audioEncode = app.audioEncode;
			
			// cleanup directories of old or broken audio files
			app.audioCapture.cleanupCaptureDirectory();
			app.audioEncode.cleanupEncodeDirectory();
			
			captureLoopPeriod = 1000*((int) Integer.parseInt(app.getPref("audio_capture_interval")));
			captureSampleRate = audioCapture.CAPTURE_SAMPLE_RATE_HZ;
			encodingBitRate = audioEncode.ENCODING_BIT_RATE;
			fileExtension = (app.audioEncode.ENCODE_ON_CAPTURE) ? "m4a" : "wav";
			captureCodec = (app.audioEncode.ENCODE_ON_CAPTURE) ? "aac" : "pcm";
			
			try {
				Log.d(TAG, "Capture Loop Period: "+ captureLoopPeriod +"ms");
				while (audioCaptureService.runFlag) {
					try {
						if (app.audioCapture.isBatteryChargeSufficientForCapture()) {
							captureLoopStart();
					        processCompletedCaptureFile();
					        Thread.sleep(captureLoopPeriod);
							captureLoopEnd();
						} else {
							Thread.sleep(2*captureLoopPeriod);
							Log.i(TAG, "AudioCapture disabled due to low battery level"
									+" (current: "+app.deviceBattery.getBatteryChargePercentage(context, null)+"%, required: "+app.audioCapture.pauseCaptureIfBatteryPercentageIsBelow+"%)."
									+" Waiting "+(Math.round(2*captureLoopPeriod/1000))+" seconds before next attempt.");
						}
					} catch (Exception e) {
						Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
						audioCaptureService.runFlag = false;
						app.isRunning_AudioCapture = false;
					}
				}
				Log.v(TAG, "Stopping service: "+TAG);
				captureLoopEnd();
				
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
				audioCaptureService.runFlag = false;
				app.isRunning_AudioCapture = false;
			}
		}
	}
	
	private void captureLoopStart() throws IllegalStateException, IOException {
		long timeStamp = Calendar.getInstance().getTimeInMillis();; 
		String filePath = app.audioCapture.captureDir+"/"+timeStamp+"."+fileExtension;
		try {
			if (app.audioEncode.ENCODE_ON_CAPTURE) {
				mediaRecorder = setAacCaptureRecorder();
				mediaRecorder.setOutputFile(filePath);
		        mediaRecorder.prepare();
		        mediaRecorder.start();
			} else {
				audioRecorder = ExtAudioRecorderModified.getInstance();
				audioRecorder.setOutputFile(filePath);
		        audioRecorder.prepare();
		        audioRecorder.start();
			}
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
        captureTimeStamps[0] = captureTimeStamps[1];
        captureTimeStamps[1] = timeStamp;
	}
	
	private void captureLoopEnd() {
		try {
			if (app.audioEncode.ENCODE_ON_CAPTURE) {
				mediaRecorder.stop();
				mediaRecorder.release();
			} else {
				audioRecorder.stop();
				audioRecorder.release();
			}
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
	}
	
	private MediaRecorder setAacCaptureRecorder() {
		MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioSamplingRate(captureSampleRate);
        mediaRecorder.setAudioEncodingBitRate(encodingBitRate);
        mediaRecorder.setAudioChannels(1);
        return mediaRecorder;
	}
	
	private void processCompletedCaptureFile() {
		File completedCapture = new File(app.audioCapture.captureDir+"/"+captureTimeStamps[0]+"."+fileExtension);
		if (completedCapture.exists()) {
			try {
				File preEncodeFile = new File(app.audioEncode.getAudioFileLocation_PreEncode(captureTimeStamps[0],fileExtension));
				fileUtils.copy(completedCapture, preEncodeFile);
				if (preEncodeFile.exists()) { completedCapture.delete(); }				
			} catch (IOException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
	        app.audioDb.dbCaptured.insert(captureTimeStamps[0]+"", fileExtension, "-", captureSampleRate, 0, captureCodec, captureLoopPeriod, captureLoopPeriod);
			Log.i(TAG, "Capture file created ("+this.captureLoopPeriod+"ms): "+captureTimeStamps[0]+"."+fileExtension);
	        app.audioEncode.triggerAudioEncodeAfterCapture(context);
		}
	}
	
	
	
	
//	private void captureLoopStart() throws IllegalStateException, IOException {
//		long timeStamp = Calendar.getInstance().getTimeInMillis();; 
//		String filePath = app.audioCapture.captureDir+"/"+timeStamp+"."+fileExtension;
//		try {
//			if (app.audioEncode.ENCODE_ON_CAPTURE) {
//				mediaRecorder = setAacCaptureRecorder();
//				mediaRecorder.setOutputFile(filePath);
//		        mediaRecorder.prepare();
//		        mediaRecorder.start();
//			} else {
//				audioRecorder = ExtAudioRecorderModified.getInstance();
//				audioRecorder.setOutputFile(filePath);
//		        audioRecorder.prepare();
//		        audioRecorder.start();
//			}
//		} catch (IllegalThreadStateException e) {
//			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//		}
//        captureTimeStamps[0] = captureTimeStamps[1];
//        captureTimeStamps[1] = timeStamp;
//	}
//	
//	private void captureLoopEnd() {
//		try {
//			if (app.audioEncode.ENCODE_ON_CAPTURE) {
//				mediaRecorder.stop();
//				mediaRecorder.release();
//			} else {
//				audioRecorder.stop();
//				audioRecorder.release();
//			}
//		} catch (IllegalThreadStateException e) {
//			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
//		}
//	}
	
	
	
	
}
