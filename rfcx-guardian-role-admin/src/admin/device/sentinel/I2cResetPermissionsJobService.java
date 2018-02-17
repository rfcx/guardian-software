package admin.device.sentinel;

import java.io.File;

import rfcx.utility.misc.ShellCommands;
import rfcx.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
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
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				File[] i2cHandlers = new File[] { 
						new File("/dev/i2c-0"), 
						new File("/dev/i2c-1"), 
						new File("/dev/i2c-2")
						};

				StringBuilder resetShellCommand = new StringBuilder();
				
				for (File i2cHandler : i2cHandlers) {
					if (i2cHandler.exists() && (!i2cHandler.canRead() || !i2cHandler.canWrite())) {
						resetShellCommand.append("chmod 666 ").append(i2cHandler.getAbsolutePath()).append("; ");
					}	
				}

				if (resetShellCommand.length() > 0) {
					ShellCommands shellCommands = new ShellCommands(app.getApplicationContext(), RfcxGuardian.APP_ROLE);
					Log.v(logTag, "Resetting Permissions on I2C Handlers...");
					shellCommands.executeCommandAsRootAndIgnoreOutput(resetShellCommand.toString());
				}
					
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
