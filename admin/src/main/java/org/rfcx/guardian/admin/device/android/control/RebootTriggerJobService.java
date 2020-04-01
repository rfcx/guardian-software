package org.rfcx.guardian.admin.device.android.control;

import org.rfcx.guardian.utility.device.AppProcessInfo;
import org.rfcx.guardian.utility.device.root.DeviceReboot;
import org.rfcx.guardian.utility.misc.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import org.rfcx.guardian.admin.RfcxGuardian;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class RebootTriggerJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, RebootTriggerJobService.class);
	
	private static final String SERVICE_NAME = "RebootTrigger";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private RebootTriggerJob rebootTriggerJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.rebootTriggerJob = new RebootTriggerJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.rebootTriggerJob.start();
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
		this.rebootTriggerJob.interrupt();
		this.rebootTriggerJob = null;
	}
	
	
	private class RebootTriggerJob extends Thread {
		
		public RebootTriggerJob() {
			super("RebootTriggerJobService-RebootTriggerJob");
		}
		
		@Override
		public void run() {
			RebootTriggerJobService rebootTriggerJobInstance = RebootTriggerJobService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				// Halting the Guardian role
				// This should be rewritten to use a content provider request to the guardian role
//				int[] guardianProcessInfo = AppProcessInfo.getProcessInfoFromRole("guardian", app.getApplicationContext());
//				Log.e(logTag, "Reboot: Forcibly killing Guardian role process (PID "+guardianProcessInfo[0]+")...");
//				ShellCommands.executeCommandAsRootAndIgnoreOutput( "kill -9 "+guardianProcessInfo[0], app.getApplicationContext());

				// Unmounting external storage (optional)
//				String externalStorage = Environment.getExternalStorageDirectory().toString();
//				Log.e(logTag, "Reboot: Forcibly un-mounting external storage ("+externalStorage+")...");
//				ShellCommands.executeCommandAsRootAndIgnoreOutput("umount "+externalStorage, app.getApplicationContext());

				// Send a broadcast for device shutdown...
				// Is this actually good or helpful in combination with the reboot request?
//				Log.e(logTag, "Reboot: Broadcasting ACTION_SHUTDOWN Intent...");
//				Intent requestShutdownAction = new Intent(Intent.ACTION_SHUTDOWN);
//				sendBroadcast(requestShutdownAction);

				// Triggering reboot request
				Log.e(logTag, "Reboot: Broadcasting ACTION_REBOOT Intent...");
				Intent requestReboot = new Intent(Intent.ACTION_REBOOT);
				requestReboot.putExtra("nowait", 1);
				requestReboot.putExtra("interval", 1);
				requestReboot.putExtra("window", 0);
				sendBroadcast(requestReboot);

				Log.e(logTag, "System should reboot any moment now...");
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				rebootTriggerJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
