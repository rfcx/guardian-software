package audio;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import org.rfcx.src_android.RfcxSource;

import utility.ExtAudioRecorderModified;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class AudioCaptureService extends Service {

	private static final String TAG = AudioCaptureService.class.getSimpleName();

	private boolean runFlag = false;
	private AudioCapture audioCapture;

	private RfcxSource app = null;
    ExtAudioRecorderModified audioRecorder = null;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.audioCapture = new AudioCapture();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		this.runFlag = true;
		app = (RfcxSource) getApplication();
		if (app.audioCore.cacheDir == null) { 
			app.audioCore.cacheDir = app.getApplicationContext().getFilesDir().getPath()+"/audio";
			(new File(app.audioCore.cacheDir)).mkdirs();
		}
		if (app.verboseLogging) Log.d(TAG, "Starting service: "+TAG);
		app.isServiceRunning_AudioCapture = true;
		this.audioCapture.start();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.isServiceRunning_AudioCapture = false;
		this.audioCapture.interrupt();
		this.audioCapture = null;
	}

	private class AudioCapture extends Thread {

		public AudioCapture() {
			super("AudioCaptureService-AudioCapture");
		}

		@Override
		public void run() {
			AudioCaptureService audioCaptureService = AudioCaptureService.this;
			app = (RfcxSource) getApplicationContext();
			AudioCore audioCore = app.audioCore;
			try {
//				int bufferSize = 12 * AudioRecord.getMinBufferSize(
//					AudioCore.CAPTURE_SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
//				AudioRecord audioRecord = new AudioRecord(
//					MediaRecorder.AudioSource.MIC, AudioCore.CAPTURE_SAMPLE_RATE,
//					AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			
//				short[] audioBuffer = new short[AudioCore.BUFFER_LENGTH];
				
				
//				StreamConfiguration streamConfiguration = new StreamConfiguration();
//		        streamConfiguration.setSampleRate(AudioCore.CAPTURE_SAMPLE_RATE);
//		        streamConfiguration.setBitsPerSample(16);
//		        streamConfiguration.setChannelCount(1);
//
//				ByteBuffer byteBuffer = ByteBuffer.allocateDirect(AudioCore.BUFFER_LENGTH*10);
//				
//				audioRecord.startRecording();
				
//				Thread.sleep(15000);
				
				
				while (audioCaptureService.runFlag) {
					try {
						
						audioRecorder = ExtAudioRecorderModified.getInstance();
				        audioRecorder.setOutputFile(app.audioCore.cacheDir+"/"+Calendar.getInstance().getTimeInMillis()+".wav");
				        audioRecorder.prepare();
				        audioRecorder.start();
						Thread.sleep(60000);
						audioRecorder.stop();
						audioRecorder.release();
						
//						audioRecord.read(byteBuffer, AudioCore.BUFFER_LENGTH*10);
//						byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
//						File outputFile = new File(fileDir,Calendar.getInstance().getTimeInMillis()+".flac");
//				        FLACFileOutputStream flacOutputStream = new FLACFileOutputStream(outputFile);
//				        FLACEncoder flacEncoder = new FLACEncoder();
//				        flacEncoder.setStreamConfiguration(streamConfiguration);
//				        flacEncoder.setOutputStream(flacOutputStream);
//						flacEncoder.openFLACStream();
//						int[] asInt = new int[byteBuffer.asIntBuffer().remaining()];
//						byteBuffer.asIntBuffer().get(asInt);
//						flacEncoder.addSamples(asInt, asInt.length);
//			   //         flacEncoder.encodeSamples(asInt.length, false);
//			            flacEncoder.encodeSamples(flacEncoder.samplesAvailableToEncode(), true);
//			            flacOutputStream.close();
////						audioCore.cachePcmBuffer(audioBuffer);
						
					} catch (Exception e) {
						e.printStackTrace();
						audioCaptureService.runFlag = false;
						app.isServiceRunning_AudioCapture = false;
					}
				}
				if (app.verboseLogging) Log.d(TAG, "Stopping service: "+TAG);
				audioRecorder.stop();
				audioRecorder.release();
//			} catch (InterruptedException e) {
//				Log.e(TAG, "InterruptedException");
//				audioCaptureService.runFlag = false;
//				app.isServiceRunning_AudioCapture = false;
			} catch (Exception e) {
				e.printStackTrace();
				audioCaptureService.runFlag = false;
				app.isServiceRunning_AudioCapture = false;
			}
		}
	}

}
