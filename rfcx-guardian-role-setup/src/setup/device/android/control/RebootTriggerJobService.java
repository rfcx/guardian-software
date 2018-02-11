package setup.device.android.control;

import java.io.File;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import setup.RfcxGuardian;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
		return START_STICKY;
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

				(new ShellCommands(app.APP_ROLE, app.getApplicationContext())).triggerRebootAsRoot();
					
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
