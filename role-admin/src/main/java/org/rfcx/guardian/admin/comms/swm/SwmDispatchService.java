package org.rfcx.guardian.admin.comms.swm;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.admin.comms.sbd.SbdDispatchTimeoutService;
import org.rfcx.guardian.admin.comms.swm.data.SwmTDResponse;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class SwmDispatchService extends Service {

	public static final String SERVICE_NAME = "SwmDispatch";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "SwmDispatchService");

	private RfcxGuardian app;

	private boolean runFlag = false;
	private SwmDispatch swmDispatch;

	private long forcedPauseBetweenEachDispatch = 3333;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.swmDispatch = new SwmDispatch();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
	//	Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.swmDispatch.start();
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
		this.swmDispatch.interrupt();
		this.swmDispatch = null;
	}


	private class SwmDispatch extends Thread {

		public SwmDispatch() {
			super("SwmDispatchService-SwmDispatch");
		}

		@Override
		public void run() {
			SwmDispatchService swmDispatchInstance = SwmDispatchService.this;

			app = (RfcxGuardian) getApplication();

			try {

				app.rfcxSvc.reportAsActive(SERVICE_NAME);

				List<String[]> swmQueuedForDispatch = app.swmMessageDb.dbSwmQueued.getRowsInOrderOfTimestamp();

				for (String[] swmForDispatch : swmQueuedForDispatch) {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					// only proceed with dispatch process if there is a valid queued swm message in the database
					if (swmForDispatch[0] != null) {

						long sendAtOrAfter = Long.parseLong(swmForDispatch[1]);
						long rightNow = System.currentTimeMillis();

						if (sendAtOrAfter <= rightNow) {

							String msgId = swmForDispatch[4];
							String msgBody = swmForDispatch[3];

							if (!app.swmUtils.isInFlight) {
								app.swmUtils.isInFlight = true;
								app.rfcxSvc.triggerService(SwmDispatchTimeoutService.SERVICE_NAME, true);
								SwmTDResponse tdResponse = app.swmUtils.getApi().transmitData("\""+msgBody+"\""); // TODO unit test
								if (tdResponse != null) {
									app.rfcxSvc.reportAsActive(SERVICE_NAME);

									app.swmUtils.consecutiveDeliveryFailureCount = 0;
									app.swmMessageDb.dbSwmQueued.deleteSingleRowByMessageId(msgId);

									String concatSegId = msgBody.substring(0, 4) + "-" + msgBody.substring(4, 7);
									Log.v(logTag, DateTimeUtils.getDateTime(rightNow) + " - Segment '" + concatSegId + "' sent by SWM (" + msgBody.length() + " chars)");
									RfcxComm.updateQuery("guardian", "database_set_last_accessed_at", "segments|" + concatSegId, app.getResolver());

								} else {
									app.swmUtils.consecutiveDeliveryFailureCount++;
									Log.e(logTag, "SWM Send Failure (Consecutive Failures: " + app.swmUtils.consecutiveDeliveryFailureCount + ")...");
									if (app.swmUtils.consecutiveDeliveryFailureCount >= SwmUtils.powerCycleAfterThisManyConsecutiveDeliveryFailures) {
										//app.swmUtils.setPower(false);
										app.swmUtils.setPower(true);
										app.swmUtils.consecutiveDeliveryFailureCount = 0;
										break;
									}
								}

								app.swmUtils.isInFlight = false;
							}

							Thread.sleep(forcedPauseBetweenEachDispatch);
						}
					}
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);

			} finally {
				app.rfcxSvc.setRunState(SERVICE_NAME, false);
				app.rfcxSvc.stopService(SERVICE_NAME, false);
				swmDispatchInstance.runFlag = false;
			}

		}
	}


}
