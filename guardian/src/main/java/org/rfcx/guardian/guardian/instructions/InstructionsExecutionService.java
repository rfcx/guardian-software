package org.rfcx.guardian.guardian.instructions;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class InstructionsExecutionService extends Service {

	private static final String SERVICE_NAME = "InstructionsExecution";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsExecutionService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstructionsExecutionSvc instructionsExecutionSvc;

	private long instructionsExecutionCycleDuration = 20000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.instructionsExecutionSvc = new InstructionsExecutionSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.instructionsExecutionSvc.start();
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
		this.instructionsExecutionSvc.interrupt();
		this.instructionsExecutionSvc = null;
	}
	
	
	private class InstructionsExecutionSvc extends Thread {
		
		public InstructionsExecutionSvc() { super("InstructionsExecutionService-InstructionsExecutionSvc"); }
		
		@Override
		public void run() {
			InstructionsExecutionService instructionsExecutionInstance = InstructionsExecutionService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			while (instructionsExecutionInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					List<String[]> instructionsQueuedForExecution = app.instructionsDb.dbQueuedInstructions.getRowsInOrderOfExecution();

					for (String[] instructionForExecution : instructionsQueuedForExecution) {

						// only proceed with execution process if there is a valid queued instruction in the database
						if (instructionForExecution[0] != null) {

							long executeAtOrAfter = (long) Long.parseLong(instructionForExecution[3]);
							long rightNow = System.currentTimeMillis();

							if (executeAtOrAfter <= rightNow) {

//								String msgId = smsForDispatch[4];
//								String msgAddress = smsForDispatch[2];
//								String msgBody = smsForDispatch[3];
//
//								DeviceSmsUtils.sendSmsMessage(msgAddress, msgBody);
//
//								app.smsMessageDb.dbSmsSent.insert(rightNow, msgAddress, msgBody, msgId);
//								app.smsMessageDb.dbSmsQueued.deleteSingleRowByMessageId(msgId);
//
//								Log.w(logTag, "SMS Sent (ID " + msgId + "): To " + msgAddress + " at " + DateTimeUtils.getDateTime(rightNow) + ": \"" + msgBody + "\"");
							}
						}
					}

					Thread.sleep(instructionsExecutionCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					instructionsExecutionInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			instructionsExecutionInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
