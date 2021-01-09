package org.rfcx.guardian.classify.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.classify.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class ClassifyQueueCycleService extends Service {

	private static final String SERVICE_NAME = "ClassifyQueueCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ClassifyQueueCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ClassifyQueueCycleSvc classifyQueueCycleSvc;

	private long classifyQueueCycleDuration = 10000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.classifyQueueCycleSvc = new ClassifyQueueCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.classifyQueueCycleSvc.start();
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
		this.classifyQueueCycleSvc.interrupt();
		this.classifyQueueCycleSvc = null;
	}
	
	
	private class ClassifyQueueCycleSvc extends Thread {
		
		public ClassifyQueueCycleSvc() { super("ClassifyQueueCycleService-ClassifyQueueCycleSvc"); }
		
		@Override
		public void run() {
			ClassifyQueueCycleService classifyQueueCycleInstance = ClassifyQueueCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (classifyQueueCycleInstance.runFlag) {

				try {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

//					if (app.sbdMessageDb.dbSbdQueued.getCount() > 0) {
//
//						app.rfcxServiceHandler.triggerService("SbdDispatch", false);
//
//					}

					Thread.sleep(classifyQueueCycleDuration);

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
					classifyQueueCycleInstance.runFlag = false;
				}
			}

			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			classifyQueueCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
