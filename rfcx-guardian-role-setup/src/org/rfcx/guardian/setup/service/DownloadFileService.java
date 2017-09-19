package org.rfcx.guardian.setup.service;

import java.io.File;

import org.rfcx.guardian.setup.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.http.HttpGet;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class DownloadFileService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DownloadFileService.class);
	
	private static final String SERVICE_NAME = "DownloadFile";

	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DownloadFile downloadFile;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.downloadFile = new DownloadFile();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.downloadFile.start();
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
		this.downloadFile.interrupt();
		this.downloadFile = null;
	}
	
	private class DownloadFile extends Thread {

		public DownloadFile() {
			super("DownloadFileService-DownloadFile");
		}

		@Override
		public void run() {
			DownloadFileService downloadFileService = DownloadFileService.this;
			Context context = app.getApplicationContext();
			HttpGet httpGet = new HttpGet(context, RfcxGuardian.APP_ROLE);
			try {
				String fileName = app.apiCore.installRole+"-"+app.apiCore.installVersion+".apk";
				String url = app.apiCore.installVersionUrl;
				String sha1 = app.apiCore.installVersionSha1;
				
				if (httpGet.getAsFile(url, fileName)) {
					Log.d(logTag, "Download Complete. Verifying Checksum...");
					String filePath = context.getFilesDir().toString()+"/"+fileName;
					String fileSha1 = FileUtils.sha1Hash(filePath);
					Log.d(logTag, "sha1 (apicheck): "+sha1);
					Log.d(logTag, "sha1 (download): "+fileSha1);
					if (fileSha1.equals(sha1)) {
						app.rfcxServiceHandler.triggerService("InstallApp", false);
					} else {
						Log.e(logTag, "Checksum mismatch");
						(new File(filePath)).delete();
					}					
				} else {
					Log.e(logTag, "Download failed");
				}
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}

}
