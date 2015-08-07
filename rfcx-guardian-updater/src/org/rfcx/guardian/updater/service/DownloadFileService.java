package org.rfcx.guardian.updater.service;

import java.io.File;

import org.rfcx.guardian.updater.R;
import org.rfcx.guardian.updater.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.HttpGet;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

public class DownloadFileService extends Service {

	private static final String TAG = "Rfcx-"+R.string.log_name+"-"+DownloadFileService.class.getSimpleName();

	private DownloadFile downloadFile;

	private RfcxGuardian app = null;
	private Context context = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.downloadFile = new DownloadFile();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		app = (RfcxGuardian) getApplication();
		if (context == null) context = app.getApplicationContext();
		
		if (app.verboseLog) Log.d(TAG, "Starting service: "+TAG);
		
		app.isRunning_DownloadFile = true;
		try {
			this.downloadFile.start();
		} catch (IllegalThreadStateException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : ""+R.string.null_exc);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		app.isRunning_DownloadFile = false;
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
			HttpGet httpGet = new HttpGet();
			try {
				String fileName = app.apiCore.installRole+"-"+app.apiCore.installVersion+".apk";
				String url = app.apiCore.installVersionUrl;
				String sha1 = app.apiCore.installVersionSha1;
				Context context = app.getApplicationContext();
				
				if (httpGet.getAsFile(url, fileName, context)) {
					if (app.verboseLog) Log.d(TAG, "Download Complete. Verifying Checksum...");
					String filePath = context.getFilesDir().toString()+"/"+fileName;
					String fileSha1 = (new FileUtils()).sha1Hash(filePath);
					if (app.verboseLog) {
						Log.d(TAG, "sha1 (apicheck): "+sha1);
						Log.d(TAG, "sha1 (download): "+fileSha1);
					}
					if (fileSha1.equals(sha1)) {
						app.triggerService("InstallApp", false);
					} else {
						Log.e(TAG, "Checksum mismatch");
						(new File(filePath)).delete();
					}					
				} else {
					Log.e(TAG, "Download failed");
				}
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : ""+R.string.null_exc);
			} finally {
				app.isRunning_DownloadFile = false;
				app.stopService("DownloadFile");
			}
		}
	}

}
