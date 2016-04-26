package org.rfcx.guardian.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class RfcxPrefs {

	private static final String TAG = "Rfcx-Utils-"+RfcxPrefs.class.getSimpleName();
	private static final FileUtils fileUtils = new FileUtils();
	
	private Context context = null;
	private String thisAppRole = null;
	
	public RfcxPrefs init(Context context, String thisAppRole) {
		this.context = context;
		this.thisAppRole = thisAppRole.toLowerCase();
		return this;
	}
	
	public String readPrefFromFile(String targetAppRole, String prefKey) {
		return readFromGuardianTxtFile(this.context, this.thisAppRole, targetAppRole.toLowerCase(), "pref_"+prefKey.toLowerCase());
	}
	
	public void writePrefToFile(String prefKey, String prefValue) {
		writeToGuardianTxtFile(this.context, "pref_"+prefKey.toLowerCase(), prefValue);
	}

	public void writeGuidToFile(String deviceId) {
		writeToGuardianTxtFile(this.context, "guid",deviceId);
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianTxtFile(this.context, "version",versionName);
	}

	private static void writeToGuardianTxtFile(Context context, String fileNameNoExt, String stringContents) {
    	String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt";
    	File fileObj = new File(filePath);
    	fileObj.mkdirs();
    	fileUtils.chmod(new File(context.getFilesDir().toString()+"/txt"), 0755);
    	if (fileObj.exists()) { fileObj.delete(); }
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
        	outFile.write(stringContents);
        	outFile.close();
        	fileUtils.chmod(new File(filePath), 0755);
        } catch (IOException e) {
        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
        }
	}
	
	private static String readFromGuardianTxtFile(Context context, String thisAppRole, String targetAppRole, String fileNameNoExt) {
    	try {
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		File txtFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+thisAppRole).length()))+"."+targetAppRole+"/files/txt",fileNameNoExt+".txt");
    		if (txtFile.exists()) {
				FileInputStream input = new FileInputStream(txtFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[12];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String txtFileContents = fileContent.toString().trim();
	    		input.close();
	    		return txtFileContents;
    		} else {
    			Log.e(TAG, "No file '"+fileNameNoExt+"' saved by org.rfcx.guardian."+targetAppRole+"...");
    		}
    	} catch (FileNotFoundException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
    	} catch (IOException e) {
    		Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
    	return null;
	}
	
}
