package org.rfcx.guardian.admin.service;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class I2cResetPermissionsJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, I2cResetPermissionsJobService.class);
	
	private static final String SERVICE_NAME = "I2cResetPermissions";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private I2cResetPermissionsJob i2cResetPermissionsJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.i2cResetPermissionsJob = new I2cResetPermissionsJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.i2cResetPermissionsJob.start();
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
		this.i2cResetPermissionsJob.interrupt();
		this.i2cResetPermissionsJob = null;
	}
	
	
	private class I2cResetPermissionsJob extends Thread {
		
		public I2cResetPermissionsJob() {
			super("I2cResetPermissionsJobService-I2cResetPermissionsJob");
		}
		
		@Override
		public void run() {
			I2cResetPermissionsJobService i2cResetPermissionsJobInstance = I2cResetPermissionsJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				ShellCommands.executeCommand("chmod 666 /dev/i2c-*", null, true, context);
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				i2cResetPermissionsJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
