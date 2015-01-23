package org.rfcx.guardian;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.rfcx.guardian.utility.FileUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardianPrefs {

	private static final String TAG = RfcxGuardianPrefs.class.getSimpleName();
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
		if (app == null) { app = rfcxApp; }
		
		app.verboseLog = app.sharedPrefs.getBoolean("verbose_logging", app.verboseLog);
		app.audioCore.setEncodeOnCapture(app.sharedPrefs.getBoolean("capture_as_aac", app.audioCore.mayEncodeOnCapture()));
		app.airplaneMode.setAllowWifi(app.sharedPrefs.getBoolean("allow_wifi", app.airplaneMode.getAllowWifi()));

//		app.ignoreOffHours = app.sharedPrefs.getBoolean("ignore_off_hours", app.ignoreOffHours);
//		app.monitorIntentServiceInterval = Integer.parseInt(app.sharedPrefs.getString("monitor_intentservice_interval", ""+app.monitorIntentServiceInterval));
//		app.apiCore.setConnectivityInterval(Integer.parseInt(app.sharedPrefs.getString("api_interval", ""+app.apiCore.getConnectivityInterval())));
//		app.apiCore.setApiDomain(app.sharedPrefs.getString("api_domain", "api.rfcx.org"));

//		app.isEnabled_AudioCapture = app.sharedPrefs.getBoolean("enable_service_audiocapture", app.isEnabled_AudioCapture);
//		app.isEnabled_DeviceState = app.sharedPrefs.getBoolean("enable_service_devicestate", app.isEnabled_DeviceState);
//		app.isEnabled_ApiComm = app.sharedPrefs.getBoolean("enable_service_apicomm", app.isEnabled_ApiComm);


//		app.dayBeginsAt = Integer.parseInt(app.sharedPrefs.getString("day_begins_at_hour", ""+app.dayBeginsAt));
//		app.dayEndsAt = Integer.parseInt(app.sharedPrefs.getString("day_ends_at_hour", ""+app.dayEndsAt));
	}


	public void loadPrefsOverride() {
		/***
		TO DO: if it exists, load some JSON
			that will define these preferences override
		 ***/
		ArrayList<String[]> prefProfileArray = new ArrayList<String[]>();
//		for ( ... ) {
//			prefProfileArray.add((String[] { nameFromJson, valueFromJson, typeFromJson });
//		}
//		Log.d(TAG, "Overriding Pref: "+thisPref[0]+" > "+thisPref[1]+" > "+ (setPreference(thisPref[0],thisPref[1],thisPref[2]) ? "Success" : "Failure" ));
	}

	public void writeGuidToFile(String deviceId) {
		if (app != null) {
			FileUtils fileUtils = new FileUtils();
	    	String filePath = app.getApplicationContext().getFilesDir().toString()+"/txt/guid.txt";
	    	File fileObj = new File(filePath);
	    	fileObj.mkdirs();
	    	fileUtils.chmod(new File(app.getApplicationContext().getFilesDir().toString()+"/txt"), 0755);
	    	if (fileObj.exists()) { fileObj.delete(); }
	        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(deviceId);
	        	outFile.close();
	        	fileUtils.chmod(new File(filePath), 0755);
	        } catch (IOException e) {
	        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
		}
	}

	public void writeVersionToFile(String versionName) {
		if (app != null) {
			FileUtils fileUtils = new FileUtils();
	    	String filePath = app.getApplicationContext().getFilesDir().toString()+"/txt/version.txt";
	    	File fileObj = new File(filePath);
	    	fileObj.mkdirs();
	    	fileUtils.chmod(new File(app.getApplicationContext().getFilesDir().toString()+"/txt"), 0755);
	    	if (fileObj.exists()) { fileObj.delete(); }
	        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(versionName);
	        	outFile.close();
	        	fileUtils.chmod(new File(filePath), 0755);
	        } catch (IOException e) {
	        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
		}
	}
	
	public void writeShellCmdToFile(String shellCmd) {
		if (app != null) {
	    	String filePath = app.getApplicationContext().getFilesDir().toString()+"/device_shell_cmd.sh";
	    	File fileObj = new File(filePath);
	    	if (fileObj.exists()) { fileObj.delete(); }
	        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(shellCmd);
	        	outFile.close();
	        	(new FileUtils()).chmod(new File(filePath), 0775);
	        } catch (IOException e) {
	        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
		}
	}

    public void writeShellCmds(String shellCmd) {
        if (app != null) {
	        // create a shell script file from a passed in string
	        String filePath = app.getFilesDir().toString()+"/device_shell_cmd.sh";
	        File fileObj = new File(filePath);
	        // remove old file if exists
	        if (fileObj.exists()) { fileObj.delete(); }
	        try {
	                BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	                outFile.write(shellCmd);
	                outFile.close();
	                (new FileUtils()).chmod(new File(filePath), 0755);
	        }
	        catch (IOException e) {
	                Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
	    }	
    }

}
