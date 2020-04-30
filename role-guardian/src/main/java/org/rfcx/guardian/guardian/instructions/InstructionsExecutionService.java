package org.rfcx.guardian.guardian.instructions;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class InstructionsExecutionService extends Service {

	private static final String SERVICE_NAME = "InstructionsExecution";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsExecutionService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstructionsExecutionSvc instructionsExecutionSvc;

	private long instructionsExecutionCycleDuration = 15000;

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

					for (String[] queuedRow : app.instructionsDb.dbQueuedInstructions.getRowsInOrderOfExecution()) {

						// only proceed with execution process if there is a valid queued instruction in the local database
						if (queuedRow[0] != null) {

							long executeAtOrAfter = (long) Long.parseLong(queuedRow[4]);
							long rightNow = System.currentTimeMillis();

							if (executeAtOrAfter <= rightNow) {

								String guid = queuedRow[1];
								long receivedAt = (long) Long.parseLong(queuedRow[0]);
								String type = queuedRow[2];
								String command = queuedRow[3];
								JSONObject metaJson = new JSONObject(queuedRow[5]);
								JSONObject responseJson = new JSONObject();

								if (app.instructionsDb.dbExecutedInstructions.getCountByGuid(guid) == 0) {
									app.instructionsDb.dbQueuedInstructions.incrementSingleRowAttemptsByGuid(guid);
									int execAttempts = ((int) Integer.parseInt(queuedRow[6])) + 1;

									// Execute the instruction

									app.instructionsDb.dbExecutedInstructions.findByGuidOrCreate(guid, queuedRow[2], queuedRow[3], System.currentTimeMillis(), responseJson.toString(), execAttempts, receivedAt);
									app.instructionsDb.dbQueuedInstructions.deleteSingleRowByGuid(guid);
									Log.w(logTag, "Instruction "+guid+" executed: Attempts: " + execAttempts + ", " + type + ", " + command + ", " + metaJson.toString());
								} else {
									Log.w(logTag, "Instruction "+guid+" has already been executed. It will be skipped, and removed from the queue, if applicable.");
									app.instructionsDb.dbQueuedInstructions.deleteSingleRowByGuid(guid);
								}
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
