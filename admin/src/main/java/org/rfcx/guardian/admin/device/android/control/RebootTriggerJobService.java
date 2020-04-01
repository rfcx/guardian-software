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

//				ShellCommands.executeCommandAndIgnoreOutput("su -c \"umount "+ Environment.getExternalStorageDirectory().toString()+"\"");

				Log.e(logTag, "Running ACTION_SHUTDOWN");
				Intent requestShutdownAction = new Intent(Intent.ACTION_SHUTDOWN);
				sendBroadcast(requestShutdownAction);

				int pid = AppProcessInfo.getAppProcessId();
				int uid = AppProcessInfo.getAppUserId();

				int[] guardianIds = AppProcessInfo.getProcessInfoFromRole("guardian", app.getApplicationContext());

//				ShellCommands.executeCommandAsRootAndIgnoreOutput("kill -9 "+AppProcessInfo.getAppProcessId(), app.getApplicationContext());

//				Log.e(logTag, "Running ACTION_REBOOT");
//				Intent requestReboot = new Intent(Intent.ACTION_REBOOT);
//				requestReboot.putExtra("nowait", 1);
//				requestReboot.putExtra("interval", 1);
//				requestReboot.putExtra("window", 0);
//				sendBroadcast(requestReboot);

				Log.e(logTag, "Rebooting now...");
					
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
