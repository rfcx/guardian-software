package org.rfcx.guardian.utility.rfcx;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;

public class RfcxPrefs {

	public RfcxPrefs(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxPrefs");
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
		this.prefsDirPath = setOrCreatePrefsDirectory(context, appRole);
	}
	
	private String logTag;
	
	private Context context = null;
	private String thisAppRole = null;
	private String prefsDirPath = null;
	
	private Map<String, String> cachedPrefs = new HashMap<String, String>();
	
	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		
		String tmpPrefValue = null;
		
		if (this.cachedPrefs.containsKey(prefKey)) {
			return this.cachedPrefs.get(prefKey);
		} else if ((tmpPrefValue = readPrefFromFile(prefKey)) != null) {
			this.cachedPrefs.put(prefKey, tmpPrefValue);
			return this.cachedPrefs.get(prefKey);
		} else if ((tmpPrefValue = readPrefFromContentProvider(prefKey)) != null) {
			this.cachedPrefs.put(prefKey, tmpPrefValue);
			return this.cachedPrefs.get(prefKey);
		} else if (defaultPrefs.containsKey(prefKey)) {
			Log.e(logTag, "Unable to read pref '"+prefKey+"', falling back to default value '"+ defaultPrefs.get(prefKey)+"'...");
			return defaultPrefs.get(prefKey);
		} else {
			return null;
		}
	}

	public int getPrefAsInt(String prefKey) {
		return (int) Integer.parseInt(getPrefAsString(prefKey));
	}

	public long getPrefAsLong(String prefKey) {
		return (long) Long.parseLong(getPrefAsString(prefKey));
	}

	public boolean getPrefAsBoolean(String prefKey) {
		return getPrefAsString(prefKey).equalsIgnoreCase("true");
	}
	
	public void setPref(String prefKey, String prefValue) {
		this.cachedPrefs.remove(prefKey);
		this.cachedPrefs.put(prefKey, prefValue);
		if ( !prefValue.equals(readPrefFromFile(prefKey)) ) {
			writePrefToFile(prefKey, prefValue);
		}
	}

	public void setPref(String prefKey, int prefValue) {
		setPref(prefKey, ""+prefValue);
	}

	public void reSyncPref(String prefKey) {
		this.cachedPrefs.remove(prefKey);
		String prefValue = getPrefAsString(prefKey);
		Log.i(logTag, "Pref ReSynced: "+prefKey+" = "+prefValue);
	}

	public void reSyncPrefInExternalRoleViaContentProvider(String targetAppRole, String prefKey, Context context) {
		try {
			Cursor targetAppRoleResponse =
					context.getContentResolver().query(
					RfcxComm.getUri(targetAppRole, "prefs_resync", prefKey),
					RfcxComm.getProjection(targetAppRole, "prefs_resync"),
					null, null, null);
			if (targetAppRoleResponse != null) {
				Log.v(logTag, targetAppRoleResponse.toString());
				targetAppRoleResponse.close();
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

//	public void clearPrefsCache() {
//		this.cachedPrefs = new HashMap<String, String>();
//		Log.i(logTag, "Prefs cache cleared.");
//		for (Map.Entry<String, ?> pref : this.sharedPrefs.getAll().entrySet()) {
//			this.rfcxPrefs.setPref(pref.getKey(), pref.getValue().toString());
//		}
//	}
	
	// Reading and Writing to preference text files
	
	private String readPrefFromFile(String prefKey) {
		try {
			
	    		String filePath = (new StringBuilder()).append(prefsDirPath).append("/").append(prefKey.toLowerCase(Locale.US)).append(".txt").toString();
	        	File fileObj = new File(filePath);
	        	
	    		if (fileObj.exists()) {
					FileInputStream input = new FileInputStream(fileObj);
					StringBuffer fileContent = new StringBuffer("");
					byte[] buffer = new byte[256];
					while (input.read(buffer) != -1) {
					    fileContent.append(new String(buffer));
					}
		    		String txtFileContents = fileContent.toString().trim();
		    		input.close();
		    		return txtFileContents;
	    		}
	    	} catch (FileNotFoundException e) {
				RfcxLog.logExc(logTag, e);
	    	} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
		}
    	return null;
	}
	
	private String readPrefFromContentProvider(String prefKey) {
		try {
			
			if (!this.thisAppRole.equalsIgnoreCase("guardian")) {
			
				Cursor prefsCursor = this.context.getContentResolver().query(
						RfcxComm.getUri("guardian", "prefs", prefKey),
						RfcxComm.getProjection("guardian", "prefs"),
						null, null, null);
				
				if ((prefsCursor != null) && (prefsCursor.getCount() > 0)) { if (prefsCursor.moveToFirst()) { try { do {
					if (prefsCursor.getString(prefsCursor.getColumnIndex("pref_key")).equalsIgnoreCase(prefKey)) {
						String prefValue = prefsCursor.getString(prefsCursor.getColumnIndex("pref_value"));
						Log.v(logTag, "Pref retrieved via Content Provider: '"+prefKey+"' = '"+prefValue+"'");
						return prefValue;
					}
				} while (prefsCursor.moveToNext()); } finally { prefsCursor.close(); } } }
			}
			
	    	} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
	    	}
    		return null;
	}
	
	
	private boolean writePrefToFile(String prefKey, String prefValue) {
		
		boolean writeSuccess = false;
		
	    	String filePath = prefsDirPath+"/"+prefKey.toLowerCase(Locale.US)+".txt";
	    	File fileObj = new File(filePath);
	    	
	    	if (!fileObj.exists()) {
	        	(new File(prefsDirPath)).mkdirs();
	        	FileUtils.chmod(prefsDirPath, "rw", "rw");
	    	} else {
	    		fileObj.delete();
	    	}
	    	
        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(prefValue);
	        	outFile.close();
	        	FileUtils.chmod(filePath, "rw", "rw");
	        	writeSuccess = fileObj.exists();
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
        return writeSuccess;
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianRoleTxtFile(this.context, this.logTag, "version", versionName);
	}


	public static void writeToGuardianRoleTxtFile(Context context, String logTag, String fileNameNoExt, String stringContents) {
		String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt";
		File fileObj = new File(filePath);
		fileObj.mkdirs();
		FileUtils.chmod(context.getFilesDir().toString()+"/txt", "rw", "rw");
		if (fileObj.exists()) { fileObj.delete(); }
        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(stringContents);
	        	outFile.close();
	        	FileUtils.chmod(filePath, "rw", "rw");
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
	}
	
	public static String readFromGuardianRoleTxtFile(Context context, String logTag, String thisAppRole, String targetAppRole, String fileNameNoExt) {
	    	try {
	    		String appFilesDir = context.getFilesDir().getAbsolutePath();
	    		File txtFileObj = new File(appFilesDir+"/txt/"+fileNameNoExt+".txt");
	    		if (!thisAppRole.equalsIgnoreCase(targetAppRole)) {
					txtFileObj = new File(appFilesDir.substring(0,appFilesDir.lastIndexOf("/files")-(("."+thisAppRole.toLowerCase(Locale.US)).length()))+"."+targetAppRole.toLowerCase(Locale.US)+"/files/txt/"+fileNameNoExt+".txt");
	    		}
	    		if (txtFileObj.exists()) {
					FileInputStream input = new FileInputStream(txtFileObj);
					StringBuilder fileContent = new StringBuilder();
					byte[] buffer = new byte[256];
					while (input.read(buffer) != -1) {
					    fileContent.append(new String(buffer));
					}
		    		String txtFileContents = fileContent.toString().trim();
		    		input.close();
		    		return txtFileContents.isEmpty() ? null : txtFileContents;
	    		} else {
	    		//	Log.e(logTag, "No file '"+fileNameNoExt+"' saved at "+txtFileObj.getAbsolutePath());
	    		}
	    	} catch (FileNotFoundException e) {
				RfcxLog.logExc(logTag, e);
	    	} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
	    	return null;
	}

	public static boolean doesGuardianRoleTxtFileExist(Context context, String fileNameNoExt) {
		return (new File(context.getFilesDir().toString()+"/txt/"+fileNameNoExt+".txt")).exists();
	}

	
	private static String setOrCreatePrefsDirectory(Context context, String appRole) {
		
		String roleFilesDir = context.getFilesDir().toString();
		String prefsDir = roleFilesDir.substring(0, roleFilesDir.lastIndexOf("/files") - ("." + appRole).length()) + ".guardian/files/prefs";
		
		if (appRole.equalsIgnoreCase("guardian")) {
			(new File(prefsDir)).mkdirs(); FileUtils.chmod(prefsDir, "rw", "rw");
		}
		
		return prefsDir;
		
	}
	
	public static List<String> listPrefsKeys() {
		List<String> prefsKeys = new ArrayList<String>();
		for (Entry prefKeyEntry : defaultPrefs.entrySet()) {
			prefsKeys.add(prefKeyEntry.getKey().toString());
		}
		return prefsKeys;
	}

	public String getPrefsChecksum() {
		
		return StringUtils.getSha1HashOfString(getPrefsAsJsonArray().toString());
	}

	public JSONArray getPrefsAsJsonArray() {

		List<String> prefsKeys = listPrefsKeys();
		Collections.sort(prefsKeys);

		JSONArray prefsBlob = new JSONArray();

		for (String prefKey : prefsKeys) {
			try {
				JSONObject thisPref = new JSONObject();
				thisPref.put(prefKey, getPrefAsString(prefKey));
				prefsBlob.put(thisPref);
			} catch (JSONException e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		return prefsBlob;
	}
	
	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
	    new HashMap<String, String>() {{

			put("enable_audio_capture", "true");
			put("enable_checkin_publish", "true");
			put("enable_cutoffs_battery", "true");
			put("enable_cutoffs_schedule_off_hours", "true");

			put("api_rest_host", "api.rfcx.org");
			put("api_rest_protocol", "https");
	        put("api_checkin_host", "checkin.rfcx.org");
	        put("api_checkin_protocol", "tcp");
	        put("api_checkin_port", "1883");
	        put("api_ntp_host", "time.apple.com");
			put("api_sms_address", "14154803657");
	        
			put("reboot_forced_daily_at", "23:54:00");
			
			put("audio_cycle_duration", "90");
			
			put("audio_schedule_off_hours", "00:10-00:15,00:15-00:20");

			put("checkin_battery_cutoff", "90");
			put("audio_battery_cutoff", "80");
			
			put("audio_encode_codec", "opus");
			put("audio_encode_bitrate", "16384");
			put("audio_sample_rate", "12000");

			put("checkin_failure_thresholds", "10,20,30,40,50,60,70,80,90");
			
			put("checkin_skip_threshold", "5");
			put("checkin_stash_threshold", "240");
			put("checkin_archive_threshold", "160");

			put("admin_enable_bluetooth", "false");
			put("admin_enable_wifi", "true");
			put("admin_enable_tcp_adb", "true");

			put("admin_log_capture_cycle", "30");
			put("admin_log_capture_level", "warn");
			put("admin_enable_log_capture", "false");
			
			put("admin_screenshot_capture_cycle", "180");
			put("admin_enable_screenshot_capture", "true");

			put("admin_enable_sentinel_capture", "true");

			put("admin_system_timezone", "[ Not Set ]");
			
			
	    }}
	);
	

	
}
