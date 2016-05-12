package org.rfcx.guardian.utility.rfcx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.utility.FileUtils;

import android.content.Context;
import android.util.Log;

public class RfcxPrefs {

	public RfcxPrefs(Context context, String appRole) {
		this.logTag = "Rfcx-"+appRole+"-"+RfcxPrefs.class.getSimpleName();
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
	}
	
	private String logTag = "Rfcx-Utils-"+RfcxPrefs.class.getSimpleName();
	
	private Context context = null;
	private String thisAppRole = null;
	
	private Map<String, String> cachedPrefs = new HashMap<String, String>();
	
	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		if (this.cachedPrefs.containsKey(prefKey)) {
			return this.cachedPrefs.get(prefKey);
		} else if (readPrefFromFile("installer", prefKey) != null) {
			this.cachedPrefs.put(prefKey, readPrefFromFile("installer", prefKey));
			return this.cachedPrefs.get(prefKey);
		} else {
			return null;
		}
	}

	public int getPrefAsInt(String prefKey) {
		return (int) Integer.parseInt(getPrefAsString(prefKey));
	}
	
	public void setPref(String targetAppRole, String prefKey, String prefValue) {
		this.cachedPrefs.remove(prefKey);
		this.cachedPrefs.put(prefKey, prefValue);
		if ( 	(targetAppRole.toLowerCase(Locale.US).equals(this.thisAppRole.toLowerCase(Locale.US)))
			&&	!prefValue.equals(readPrefFromFile(targetAppRole, prefKey))
			) {
				writePrefToFile(prefKey, prefValue);
			}
	}

	public void setPref(String targetAppRole, String prefKey, int prefValue) {
		setPref(targetAppRole, prefKey, ""+prefValue);
	}
	
	// Reading and Writing to preference text files
	
	public String readPrefFromFile(String targetAppRole, String prefKey) {
		return readFromGuardianTxtFile(this.context, this.thisAppRole, targetAppRole.toLowerCase(Locale.US), "pref_"+prefKey.toLowerCase(Locale.US));
	}
	
	public void writePrefToFile(String prefKey, String prefValue) {
		writeToGuardianTxtFile(this.context, "pref_"+prefKey.toLowerCase(Locale.US), prefValue);
	}

	public void writeGuidToFile(String deviceId) {
		writeToGuardianTxtFile(this.context, "guid",deviceId);
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianTxtFile(this.context, "version",versionName);
	}
	
	public String getVersionFromFile(String targetAppRole) {
		return readFromGuardianTxtFile(this.context, this.thisAppRole, targetAppRole.toLowerCase(Locale.US), "version");
	}

	private void writeToGuardianTxtFile(Context context, String fileNameNoExt, String stringContents) {
    	String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt";
    	File fileObj = new File(filePath);
    	fileObj.mkdirs();
    	FileUtils.chmod(context.getFilesDir().toString()+"/txt", 0755);
    	if (fileObj.exists()) { fileObj.delete(); }
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
        	outFile.write(stringContents);
        	outFile.close();
        	FileUtils.chmod(filePath, 0755);
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
	}
	
	private String readFromGuardianTxtFile(Context context, String thisAppRole, String targetAppRole, String fileNameNoExt) {
    	try {
    		String mainAppPath = context.getFilesDir().getAbsolutePath();
    		File txtFile = new File(mainAppPath.substring(0,mainAppPath.lastIndexOf("/files")-(("."+thisAppRole).length()))+"."+targetAppRole+"/files/txt",fileNameNoExt+".txt");
    		if (txtFile.exists()) {
				FileInputStream input = new FileInputStream(txtFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[256];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String txtFileContents = fileContent.toString().trim();
	    		input.close();
	    		return txtFileContents;
    		} else {
    			Log.e(logTag, "No file '"+fileNameNoExt+"' saved by org.rfcx.guardian."+targetAppRole+"...");
    		}
    	} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
    	} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
    	return null;
	}
	
}
