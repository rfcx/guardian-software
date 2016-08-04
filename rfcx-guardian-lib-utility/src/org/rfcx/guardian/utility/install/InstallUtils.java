package org.rfcx.guardian.utility.install;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.http.HttpGet;

import android.content.Context;
import android.util.Log;

public class InstallUtils {

	private Context context;
	private String appRole = "Utils";
	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(InstallUtils.class.getSimpleName()).toString();
	
	public InstallUtils (Context context, String appRole) {
		this.context = context;
		this.appRole = appRole;
		this.logTag = (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(InstallUtils.class.getSimpleName()).toString();
	}
	
	private Map<String, String[]> downloadQueue = new HashMap<String, String[]>();
	private Map<String, String[]> installQueue = new HashMap<String, String[]>();
	
	
	public void scanThroughDownloadEntries() {
	   for (Map.Entry<String, String[]> queueEntry : downloadQueue.entrySet()) { 
	    	Log.i(this.logTag, "Key = " + queueEntry.getKey() + ", Value = " + queueEntry.getValue()[0]);
	    }   
	}
	
	public boolean downloadAndVerify(String fileUrl, String fileName, String fileCheckSum) {
		
		if ((new HttpGet(this.context, this.appRole)).getAsFile(fileUrl, fileName)) {
			Log.d(this.logTag, "Download Complete. Verifying CheckSum...");
			return verifyFileCheckSum(fileName, fileCheckSum);
		} else {
			Log.e(this.logTag, "Download of file failed...");
		}
		return false;
	}
	
	private boolean verifyFileCheckSum(String fileName, String fileCheckSum) {
		
		String filePath = (new StringBuilder()).append(this.context.getFilesDir().toString()).append("/").append(fileName).toString();
		String fileSha1 = FileUtils.sha1Hash(filePath);
		Log.d(this.logTag, "Checksum (expected): "+fileCheckSum);
		Log.d(this.logTag, "Checksum (download): "+fileSha1);
		if (!fileSha1.equals(fileCheckSum)) { (new File(filePath)).delete(); }
		return fileSha1.equals(fileCheckSum);
	}
	
//	private static boolean installApk(Context context, String apkFileName, boolean forceReInstallFlag) {
//		
////		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
//		File apkFile = new File(context.getFilesDir().getPath(), apkFileName);
//		String apkFilePath = apkFile.getAbsolutePath();
//		String reInstallFlag = (app.apiCore.installVersion == null) ? "" : " -r";
//		if (forceReInstallFlag) reInstallFlag = " -r";
//		Log.d(TAG, "installing "+apkFilePath);
//		try {
//			boolean isInstalled = ShellCommands.executeCommand(
//					"pm install"+reInstallFlag+" "+apkFilePath,
//					"Success",true,context);
//			if (apkFile.exists()) { apkFile.delete(); }
//			return isInstalled;
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//			if (apkFile.exists()) { apkFile.delete(); }
//		} finally {
//		}
//		return false;
//		
//	}
	
	
	public void addToDownloadQueue(String roleName, String[] roleMeta) {
		removeFromDownloadQueue(roleName);
		this.downloadQueue.put(roleName, roleMeta);
	}

	public void removeFromDownloadQueue(String roleName) {
		if (this.downloadQueue.containsKey(roleName)) {
			this.downloadQueue.remove(roleName);
		}
	}
	
	public void addToInstallQueue(String roleName, String[] roleMeta) {
		removeFromInstallQueue(roleName);
		this.installQueue.put(roleName, roleMeta);
	}

	public void removeFromInstallQueue(String roleName) {
		if (this.installQueue.containsKey(roleName)) {
			this.installQueue.remove(roleName);
		}
	}
	
}
