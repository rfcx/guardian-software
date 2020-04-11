package org.rfcx.guardian.admin.sms;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.device.DeviceSmsUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class SmsDispatchService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SmsDispatchService.class);
	
	private static final String SERVICE_NAME = "SmsDispatch";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SmsDispatch smsDispatch;

	private long smsDispatchCycleDuration = 30000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.smsDispatch = new SmsDispatch();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.smsDispatch.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.smsDispatch.interrupt();
		this.smsDispatch = null;
	}
	
	
	private class SmsDispatch extends Thread {
		
		public SmsDispatch() {
			super("SmsDispatchService-SmsDispatch");
		}
		
		@Override
		public void run() {
			SmsDispatchService smsDispatchInstance = SmsDispatchService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			while (smsDispatchInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					List<String[]> smsQueuedForDispatch = app.smsMessageDb.dbSmsQueued.getRowsInOrderOfTimestamp();

					for (String[] smsForDispatch : smsQueuedForDispatch) {

						// only proceed with dispatch process if there is a valid queued sms message in the database
						if (smsForDispatch[0] != null) {

							long sendAtOrAfter = (long) Long.parseLong(smsForDispatch[1]);
							long rightNow = System.currentTimeMillis();

							if (sendAtOrAfter <= rightNow) {

								String msgId = smsForDispatch[4];
								String msgAddress = smsForDispatch[2];
								String msgBody = smsForDispatch[3];

								DeviceSmsUtils.sendSmsMessage(msgAddress, msgBody);

								app.smsMessageDb.dbSmsSent.insert(rightNow, msgAddress, msgBody, msgId);
								app.smsMessageDb.dbSmsQueued.deleteSingleRowByMessageId(msgId);

								Log.w(logTag, "SMS Sent (ID " + msgId + "): To " + msgAddress + " at " + rightNow + ": \"" + msgBody + "\"");
							}
						}
					}

					Thread.sleep(smsDispatchCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					smsDispatchInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			smsDispatchInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
