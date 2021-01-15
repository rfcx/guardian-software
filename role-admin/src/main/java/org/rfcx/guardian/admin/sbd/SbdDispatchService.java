package org.rfcx.guardian.admin.sbd;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class SbdDispatchService extends Service {

	public static final String SERVICE_NAME = "SbdDispatch";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SbdDispatchService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private SbdDispatch sbdDispatch;

	private long forcedPauseBetweenEachDispatch = 1333;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.sbdDispatch = new SbdDispatch();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
	//	Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.sbdDispatch.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
		this.sbdDispatch.interrupt();
		this.sbdDispatch = null;
	}
	
	
	private class SbdDispatch extends Thread {
		
		public SbdDispatch() {
			super("SbdDispatchService-SbdDispatch");
		}
		
		@Override
		public void run() {
			SbdDispatchService sbdDispatchInstance = SbdDispatchService.this;
			
			app = (RfcxGuardian) getApplication();

			try {

				app.rfcxSvc.reportAsActive(SERVICE_NAME);

				List<String[]> sbdQueuedForDispatch = app.sbdMessageDb.dbSbdQueued.getRowsInOrderOfTimestamp();

				for (String[] sbdForDispatch : sbdQueuedForDispatch) {

					// only proceed with dispatch process if there is a valid queued sms message in the database
					if (sbdForDispatch[0] != null) {

						long sendAtOrAfter = Long.parseLong(sbdForDispatch[1]);
						long rightNow = System.currentTimeMillis();

						if (sendAtOrAfter <= rightNow) {

							String msgId = sbdForDispatch[4];
							String msgAddress = sbdForDispatch[2];
							String msgBody = sbdForDispatch[3];

					//		DeviceSmsUtils.sendSmsMessage(msgAddress, msgBody);

							app.smsMessageDb.dbSmsQueued.deleteSingleRowByMessageId(msgId);

//							if (!msgAddress.equalsIgnoreCase(apiSmsAddress)) {
//
//								app.smsMessageDb.dbSmsSent.insert(rightNow, msgAddress, msgBody, msgId);
//								Log.w(logTag, "SMS Sent (ID " + msgId + "): To " + msgAddress + " at " + DateTimeUtils.getDateTime(rightNow) + ": \"" + msgBody + "\"");
//
//							} else {
//
//								String concatSegId = msgBody.substring(0,4) + "-" + msgBody.substring(4,7);
//								Log.v(logTag, DateTimeUtils.getDateTime(rightNow)+" - Segment '"+concatSegId + "' sent by SMS ("+msgBody.length()+" chars)");
//								RfcxComm.updateQuery("guardian", "database_set_last_accessed_at", "segments|" + concatSegId, app.getResolver());
//							}

							Thread.sleep(forcedPauseBetweenEachDispatch);
						}
					}
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);

			} finally {
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				app.rfcxSvc.stopService(SERVICE_NAME, false);
				sbdDispatchInstance.runFlag = false;
			}

		}
	}

	
}
