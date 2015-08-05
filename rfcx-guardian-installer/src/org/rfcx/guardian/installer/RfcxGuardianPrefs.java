package org.rfcx.guardian.installer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rfcx.guardian.utility.FileUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardianPrefs {

	private static final String TAG = "Rfcx-Installer-"+RfcxGuardianPrefs.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private RfcxGuardian app = null;
	
	public SharedPreferences createPrefs(RfcxGuardian rfcxApp) {
		app = rfcxApp;
		SharedPreferences sharedPreferences = null;
		return sharedPreferences;
	}
	
	public void initializePrefs() {
		PreferenceManager.setDefaultValues(app, R.xml.prefs, false);
		app.sharedPrefs = PreferenceManager.getDefaultSharedPreferences(app);
		app.sharedPrefs.registerOnSharedPreferenceChangeListener(app);
	}
	
	public boolean setPref(String name, String value, String type) {
		Editor editor = app.sharedPrefs.edit();
		if (type.equals("boolean")) editor.putBoolean(name, Boolean.parseBoolean(value));
		else if (type.equals("int")) editor.putString(name, ""+value); // still not saving ints as ints 
		else if (type.equals("float")) editor.putString(name, ""+value); // still not saving floats as floats 
		else if (type.equals("long")) editor.putString(name, ""+value);  // still not saving longs as longs 
		else editor.putString(name, value);
		return editor.commit();
	}
	
	public void checkAndSet(RfcxGuardian rfcxApp) {
		app = rfcxApp;
		app.verboseLog = app.sharedPrefs.getBoolean("verbose_logging", app.verboseLog);
		app.apiCore.targetAppRoleApiEndpoint = app.targetAppRoleApiEndpoint;
		app.apiCore.setApiCheckVersionEndpoint(app.getDeviceId());
	}
	
	private void writeToGuardianTxtFile(String fileNameNoExt, String stringContents) {
		if (app != null) {
			FileUtils fileUtils = new FileUtils();
	    	String filePath = app.getApplicationContext().getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt";
	    	File fileObj = new File(filePath);
	    	fileObj.mkdirs();
	    	fileUtils.chmod(new File(app.getApplicationContext().getFilesDir().toString()+"/txt"), 0755);
	    	if (fileObj.exists()) { fileObj.delete(); }
	        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(stringContents);
	        	outFile.close();
	        	fileUtils.chmod(new File(filePath), 0755);
	        } catch (IOException e) {
	        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
		}
	}
	
	public void writeGuidToFile(String deviceId) {
		writeToGuardianTxtFile("guid",deviceId);
	}

	public void writeTokenToFile(String deviceToken) {
		writeToGuardianTxtFile("token",deviceToken);
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianTxtFile("version",versionName);
	}
	
}
