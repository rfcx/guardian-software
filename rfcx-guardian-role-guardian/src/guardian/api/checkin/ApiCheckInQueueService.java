package guardian.api.checkin;

import rfcx.utility.audio.RfcxAudioUtils;
import rfcx.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import guardian.RfcxGuardian;

public class ApiCheckInQueueService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInQueueService.class);
	
	private static final String SERVICE_NAME = "ApiCheckInQueue";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiCheckInQueue apiCheckInQueue;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiCheckInQueue = new ApiCheckInQueue();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.apiCheckInQueue.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.apiCheckInQueue.interrupt();
		this.apiCheckInQueue = null;
	}
	
	private class ApiCheckInQueue extends Thread {

		public ApiCheckInQueue() {
			super("ApiCheckInQueueService-ApiCheckInQueue");
		}
		
		@Override
		public void run() {
			ApiCheckInQueueService apiCheckInQueueInstance = ApiCheckInQueueService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
				
			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {
				
				for (String[] encodedAudio : app.audioEncodeDb.dbEncoded.getAllRows()) {
					
					String[] audioInfo = new String[] {
							encodedAudio[0], // "created_at"
							encodedAudio[1], //"timestamp"
							encodedAudio[2], //"format"
							encodedAudio[3], //"digest"
							encodedAudio[4], //"samplerate"
							encodedAudio[5], //"bitrate"
							encodedAudio[6], //"codec"
							(RfcxAudioUtils.isEncodedWithVbr(encodedAudio[6]) ? "vbr" : "cbr"), //"cbr_or_vbr"
							encodedAudio[8] //"encode_duration"
						};
					
					if (app.apiCheckInUtils.addCheckInToQueue(audioInfo, encodedAudio[9])) {
						app.audioEncodeDb.dbEncoded.deleteSingleRow(encodedAudio[1]);
					}
		
				}
				

				if (app.rfcxPrefs.getPrefAsBoolean("checkin_offline_mode")) { 
					Log.v(logTag, "No CheckIn because offline mode is on");
				} else {
					app.rfcxServiceHandler.triggerService("ApiCheckInJob", false);
				}
				
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				
			} finally {
				apiCheckInQueueInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}

		}
	}

}
