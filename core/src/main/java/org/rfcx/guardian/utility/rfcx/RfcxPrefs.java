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
		this.logTag = RfcxLog.generateLogTag(appRole, RfcxPrefs.class);
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
		this.prefsDirPath = setOrCreatePrefsDirectory(context, appRole);
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", RfcxPrefs.class);
	
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
		} else if (this.defaultPrefs.containsKey(prefKey)) {
			Log.e(logTag, "Unable to read pref '"+prefKey+"', falling back to default value '"+this.defaultPrefs.get(prefKey)+"'...");
			return this.defaultPrefs.get(prefKey);
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
	
	public void clearPrefsCache() {
		this.cachedPrefs = new HashMap<String, String>();
	}
	
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
	    		} else {
	    			Log.e(logTag, "No preference file '"+prefKey+"' exists...");
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
			
			if (!this.thisAppRole.equalsIgnoreCase("org.rfcx.guardian.guardian")) {
			
				Cursor prefsCursor = this.context.getContentResolver().query(
						RfcxComm.getUri("org.rfcx.guardian.guardian", "prefs", prefKey),
						RfcxComm.getProjection("org.rfcx.guardian.guardian", "prefs"),
						null, null, null);
				
				if ((prefsCursor != null) && (prefsCursor.getCount() > 0)) { if (prefsCursor.moveToFirst()) { try { do {
					if (prefsCursor.getString(prefsCursor.getColumnIndex("pref_key")).equalsIgnoreCase(prefKey)) {
						Log.v(logTag, "Receiving pref '"+prefKey+"' via content provider...");
						return prefsCursor.getString(prefsCursor.getColumnIndex("pref_value"));
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
	        	FileUtils.chmod(prefsDirPath, 0777);
	    	} else {
	    		fileObj.delete();
	    	}
	    	
        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
	        	outFile.write(prefValue);
	        	outFile.close();
	        	FileUtils.chmod(filePath, 0777);
	        	writeSuccess = fileObj.exists();
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
        return writeSuccess;
	}
	
	public void writeVersionToFile(String versionName) {
		writeToGuardianRoleTxtFile(this.context, this.logTag, "version",versionName);
	}
	
	public String getVersionFromFile(String targetAppRole) {
		return readFromGuardianRoleTxtFile(this.context, this.logTag, this.thisAppRole, targetAppRole.toLowerCase(Locale.US), "version");
	}
	
	public void writeGuidToFile(String deviceGuid) {
		writeToGuardianRoleTxtFile(this.context, this.logTag, "guid", deviceGuid);
	}

	public static void writeGuidToFile(Context context, String logTag, String deviceGuid) {
		writeToGuardianRoleTxtFile(context, logTag, "guid", deviceGuid);
	}
	
	public static String getGuidFromFile(Context context, String logTag, String thisAppRole, String targetAppRole) {
		return readFromGuardianRoleTxtFile(context, logTag, thisAppRole, targetAppRole.toLowerCase(Locale.US), "guid");
	}

	private static void writeToGuardianRoleTxtFile(Context context, String logTag, String fileNameNoExt, String stringContents) {
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
	
	public static String readFromGuardianRoleTxtFile(Context context, String logTag, String thisAppRole, String targetAppRole, String fileNameNoExt) {
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
		    		return txtFileContents.isEmpty() ? null : txtFileContents;
	    		} else {
//	    			Log.e(logTag, "No file '"+fileNameNoExt+"' saved by org.rfcx.org.rfcx.guardian.guardian."+targetAppRole+"...");
	    		}
	    	} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
	    	} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	    	return null;
	}

	
	private static String setOrCreatePrefsDirectory(Context context, String appRole) {
		
		String roleFilesDir = context.getFilesDir().toString();
		String prefsDir = (new StringBuilder()).append(roleFilesDir.substring(0, roleFilesDir.lastIndexOf("/files")-("."+appRole).length())).append(".org.rfcx.guardian.guardian/files/prefs").toString();
		
		if (appRole.equalsIgnoreCase("org.rfcx.guardian.guardian")) {
			(new File(prefsDir)).mkdirs(); FileUtils.chmod(prefsDir, 0777);
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
		
		return StringUtils.getSha1HashOfString(prefsBlob.toString());
	
	}
	
	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
	    new HashMap<String, String>() {{

			put("verbose_logging", "true");

			put("enable_audio_capture", "true");
			put("enable_checkin_publish", "true");
			put("enable_cutoffs_battery", "true");
			put("enable_cutoffs_schedule_off_hours", "true");
			
	        put("api_checkin_host", "checkin.rfcx.org");
	        put("api_checkin_protocol", "tcp");
	        put("api_checkin_port", "1883");
	        put("api_ntp_host", "time.apple.com");
	        
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
			put("checkin_archive_threshold", "240");

			put("admin_enable_bluetooth", "true");	
			
			put("admin_log_capture_cycle", "30");
			put("admin_log_capture_level", "warn");
			put("admin_enable_log_capture", "true");
			
			put("admin_screenshot_capture_cycle", "180");
			put("admin_enable_screenshot_capture", "true");
			
			
			
	    }}
	);
	

	
}
