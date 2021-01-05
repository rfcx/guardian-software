package org.rfcx.guardian.guardian.instructions;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.json.JSONObject;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class InstructionsExecutionService extends Service {

	private static final String SERVICE_NAME = "InstructionsExecution";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "InstructionsExecutionService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private InstructionsExecutionSvc instructionsExecutionSvc;

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
//		Log.v(logTag, "Starting service: "+logTag);
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

			try {

				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				for (String[] queuedRow : app.instructionsDb.dbQueued.getRowsInOrderOfExecution()) {

					// only proceed with execution process if there is a valid queued instruction in the local database
					if (queuedRow[0] != null) {

						long executeAtOrAfter = Long.parseLong(queuedRow[4]);
						long rightNow = System.currentTimeMillis();

						if (executeAtOrAfter <= rightNow) {

							String instrId = queuedRow[1];
							long receivedAt = Long.parseLong(queuedRow[0]);
							String type = queuedRow[2];
							String command = queuedRow[3];
							JSONObject metaJson = new JSONObject(queuedRow[5]);

							if (app.instructionsDb.dbExecuted.getCountById(instrId) == 0) {
								app.instructionsDb.dbQueued.incrementSingleRowAttemptsById(instrId);
								int execAttempts = (Integer.parseInt(queuedRow[6])) + 1;

								// Execute the instruction
								String responseJsonStr = app.instructionsUtils.executeInstruction(type, command, metaJson);

								app.instructionsDb.dbExecuted.findByIdOrCreate(instrId, queuedRow[2], queuedRow[3], System.currentTimeMillis(), responseJsonStr, execAttempts, receivedAt);
								app.instructionsDb.dbQueued.deleteSingleRowById(instrId);
								Log.w(logTag, "Instruction "+instrId+" executed: Attempts: " + execAttempts + ", " + type + ", " + command + ", " + metaJson.toString());
							} else {
								Log.w(logTag, "Instruction "+instrId+" has already been executed. It will be skipped, and removed from the queue, if applicable.");
								app.instructionsDb.dbQueued.deleteSingleRowById(instrId);
							}

							// send execution receipt
							String[] pingFields = new String[] { "instructions" };
							if (type.equalsIgnoreCase("set") && command.equalsIgnoreCase("prefs")) { pingFields = new String[] { "instructions", "prefs" }; }
							app.apiPingUtils.sendPing(false, pingFields);

						}
					}
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);

			} finally {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME, false);
				instructionsExecutionInstance.runFlag = false;
			}

		}
	}

	
}
