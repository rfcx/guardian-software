package org.rfcx.guardian.updater.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxGarbageCollection;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class RebootTriggerService extends Service {

	public static final String SERVICE_NAME = "RebootTrigger";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "RebootTriggerService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private RebootTrigger rebootTrigger;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.rebootTrigger = new RebootTrigger();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.rebootTrigger.start();
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
		this.rebootTrigger.interrupt();
		this.rebootTrigger = null;
	}
	
	
	private class RebootTrigger extends Thread {
		
		public RebootTrigger() {
			super("RebootTriggerService-RebootTrigger");
		}
		
		@Override
		public void run() {
			RebootTriggerService rebootTriggerInstance = RebootTriggerService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// Halting the Guardian role services
				Log.e(logTag, "Reboot: Requesting that Guardian role stop all services...");
				try {
					Cursor killGuardianSvcs = app.getResolver().query(
						RfcxComm.getUri("guardian", "control", "kill"),
						RfcxComm.getProjection("guardian", "control"),
						null, null, null);
					Log.v(logTag, killGuardianSvcs.toString());
					killGuardianSvcs.close();
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}

				// Halting the Admin role services
				Log.e(logTag, "Reboot: Requesting that Admin role stop all services...");
				try {
					Cursor killAdminSvcs = app.getResolver().query(
							RfcxComm.getUri("admin", "control", "kill"),
							RfcxComm.getProjection("admin", "control"),
							null, null, null);
					Log.v(logTag, killAdminSvcs.toString());
					killAdminSvcs.close();
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}

				// Garbage Collection
				RfcxGarbageCollection.runAndroidGarbageCollection();

				// Triggering reboot request
				Log.e(logTag, "Reboot: Broadcasting ACTION_REBOOT Intent...");
				Intent actionReboot = new Intent(Intent.ACTION_REBOOT);
				actionReboot.putExtra("nowait", 1);
				actionReboot.putExtra("window", 1);
				sendBroadcast(actionReboot);

				Log.e(logTag, "System should be shutting down now...");
				app.rfcxServiceHandler.stopAllServices();


			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				rebootTriggerInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
