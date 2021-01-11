package org.rfcx.guardian.guardian.api.methods.download;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.network.HttpGet;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.List;

public class AssetDownloadJobService extends Service {

	public static final String SERVICE_NAME = "AssetDownloadJob";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetDownloadJobService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private AssetDownloadJob assetDownloadJob;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.assetDownloadJob = new AssetDownloadJob();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
//		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.assetDownloadJob.start();
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
		this.assetDownloadJob.interrupt();
		this.assetDownloadJob = null;
	}
	
	private class AssetDownloadJob extends Thread {

		public AssetDownloadJob() {
			super("AssetDownloadJobService-AssetDownloadJob");
		}
		
		@Override
		public void run() {
			AssetDownloadJobService assetDownloadJobInstance = AssetDownloadJobService.this;
			
			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);
			
			try {

				List<String[]> queuedAssetsForDownload = app.assetDownloadDb.dbQueued.getAllRows();
				if (queuedAssetsForDownload.size() == 0) { Log.d(logTag, "No asset download jobs are currently queued."); }
				app.assetDownloadUtils.cleanupDownloadDirectory( queuedAssetsForDownload, Math.round( 1.0 * 3 * 60 * 60 * 1000 ) );

				for (String[] queuedDownloads : queuedAssetsForDownload) {

					app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

					if (queuedDownloads[0] != null) {

						String queuedAt = queuedDownloads[0];
						String assetType = queuedDownloads[1];
						String assetId = queuedDownloads[2];
						String checksum = queuedDownloads[3];
						String protocol = queuedDownloads[4];
						String uriStr = queuedDownloads[5];
						long fileSize = Long.parseLong(queuedDownloads[6]);
						String fileType = queuedDownloads[7];
						int downloadAttempts = Integer.parseInt(queuedDownloads[8]);
						long lastAccessedAt = Long.parseLong(queuedDownloads[9]);
/*
						if (!FileUtils.exists(audioFilePath)) {

							Log.d(logTag, "Skipping Audio Playback Job for " + assetId + " because input audio file could not be found.");

							app.audioPlaybackDb.dbQueued.deleteSingleRowByCreatedAt(queuedAt);

						} else*/ if (downloadAttempts >= AssetDownloadUtils.DOWNLOAD_FAILURE_SKIP_THRESHOLD) {

							Log.d(logTag, "Skipping Asset Download Job for " + assetType + ", " + assetId + " after " + AssetDownloadUtils.DOWNLOAD_FAILURE_SKIP_THRESHOLD + " failed attempts.");

							app.assetDownloadDb.dbSkipped.insert(assetType, assetId, checksum, protocol, uriStr, fileSize, fileType, downloadAttempts, System.currentTimeMillis());
							app.assetDownloadDb.dbQueued.deleteSingleRow(assetType, assetId);

						} else {


							Log.i(logTag, "Beginning Asset Download Job: " + assetType + ", " + assetId);

							app.assetDownloadDb.dbQueued.incrementSingleRowAttempts(assetType, assetId);
							app.assetDownloadDb.dbQueued.updateLastAccessedAt(assetType, assetId);

							String downloadTmpFilePath = app.assetDownloadUtils.getTmpAssetFilePath(assetType, assetId);
							String postDownloadFilePath = app.assetDownloadUtils.getPostDownloadAssetFilePath(assetType, assetId, fileType);
							String finalGalleryFilePath = app.assetGalleryUtils.getGalleryAssetFilePath(assetType, assetId, fileType);

							if (protocol.equalsIgnoreCase("http")) {

								if (FileUtils.sha1Hash(finalGalleryFilePath).equalsIgnoreCase(checksum)) {

									Log.e(logTag, "Asset Download will be skipped. An existing copy of queued asset was found with correct checksum at " + RfcxAssetCleanup.conciseFilePath(finalGalleryFilePath, RfcxGuardian.APP_ROLE));
									app.assetDownloadDb.dbQueued.deleteSingleRow(assetType, assetId);

								} else {

									HttpGet httpGet = new HttpGet(context, RfcxGuardian.APP_ROLE);
									httpGet.setTimeOuts(30000, 300000);

									long downloadStartTime = System.currentTimeMillis();
									httpGet.getAsFile(uriStr, downloadTmpFilePath);
									long downloadDuration = (System.currentTimeMillis() - downloadStartTime);

									FileUtils.delete(postDownloadFilePath);
									FileUtils.gUnZipFile(downloadTmpFilePath, postDownloadFilePath);
									String downloadChecksum = FileUtils.sha1Hash(postDownloadFilePath);

									if (downloadChecksum.equalsIgnoreCase(checksum)) {
										FileUtils.chmod(postDownloadFilePath, "rw", "rw");

										// log successful completion
										app.assetDownloadDb.dbCompleted.insert(assetType, assetId, checksum, protocol, uriStr, fileSize, fileType, downloadAttempts + 1, downloadDuration);
										app.assetDownloadDb.dbQueued.deleteSingleRow(assetType, assetId);

										Log.i(logTag, "Asset Download Successful. File will be placed in the Asset Gallery at " + RfcxAssetCleanup.conciseFilePath(finalGalleryFilePath, RfcxGuardian.APP_ROLE));

										app.assetDownloadUtils.followUpOnSuccessfulDownload( assetType, assetId, fileType, checksum, fileSize );

									} else {
										Log.e(logTag, "Asset Download Failure: Rejected due to checksum mis-match on decompressed asset file.");
										Log.e(logTag, downloadChecksum+" - "+checksum);
										FileUtils.delete(postDownloadFilePath);

									}
								}

								FileUtils.delete(downloadTmpFilePath);

							}
						}

					} else {
						Log.d(logTag, "Queued asset download entry in database is invalid.");

					}

				}


					
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				assetDownloadJobInstance.runFlag = false;
			}
			
			app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
			assetDownloadJobInstance.runFlag = false;

		}
	}
	

}
