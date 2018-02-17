package admin.device.android.control;

import java.io.File;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import admin.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DateTimeResetPermissionsJobService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DateTimeResetPermissionsJobService.class);
	
	private static final String SERVICE_NAME = "DateTimeResetPermissions";
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DateTimeResetPermissionsJob dateTimeResetPermissionsJob;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.dateTimeResetPermissionsJob = new DateTimeResetPermissionsJob();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.dateTimeResetPermissionsJob.start();
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
		this.dateTimeResetPermissionsJob.interrupt();
		this.dateTimeResetPermissionsJob = null;
	}
	
	
	private class DateTimeResetPermissionsJob extends Thread {
		
		public DateTimeResetPermissionsJob() {
			super("DateTimeResetPermissionsJobService-DateTimeResetPermissionsJob");
		}
		
		@Override
		public void run() {
			DateTimeResetPermissionsJobService dateTimeResetPermissionsJobInstance = DateTimeResetPermissionsJobService.this;
			
			app = (RfcxGuardian) getApplication();
			
			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
				
				File[] dateTimeAlarmHandlers = new File[] { 
						new File("/dev/alarm")
						};

				StringBuilder resetShellCommand = new StringBuilder();
				
				for (File dateTimeAlarmHandler : dateTimeAlarmHandlers) {
					if (dateTimeAlarmHandler.exists() && (!dateTimeAlarmHandler.canRead() || !dateTimeAlarmHandler.canWrite())) {
						resetShellCommand.append("chmod 666 ").append(dateTimeAlarmHandler.getAbsolutePath()).append("; ");
					}	
				}

				if (resetShellCommand.length() > 0) {
					ShellCommands shellCommands = new ShellCommands(app.getApplicationContext(), RfcxGuardian.APP_ROLE);
					Log.v(logTag, "Resetting Permissions on DateTime Alarm Handlers...");
					shellCommands.executeCommandAsRootAndIgnoreOutput(resetShellCommand.toString());
				}
					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				dateTimeResetPermissionsJobInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

	
}
