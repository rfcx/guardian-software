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
import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;

public class RfcxPrefs {

	public RfcxPrefs(Context context, String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxPrefs");
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
		this.prefsDirPath = setOrCreatePrefsDirectory(context);
	}
	
	private String logTag;
	
	private Context context = null;
	private String thisAppRole = null;
	private String prefsDirPath = null;
	
	private Map<String, String> cachedPrefs = new HashMap<String, String>();
	
	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		
		String newPrefValue = null;
		
		if (this.cachedPrefs.containsKey(prefKey)) {
			return this.cachedPrefs.get(prefKey);
		} else if ((newPrefValue = readPrefFromContentProvider(prefKey)) != null) {
			this.cachedPrefs.put(prefKey, newPrefValue);
			writePrefToFile(prefKey, newPrefValue);
			return this.cachedPrefs.get(prefKey);
		} else if ((newPrefValue = readPrefFromFile(prefKey)) != null) {
			this.cachedPrefs.put(prefKey, newPrefValue);
			return this.cachedPrefs.get(prefKey);
		} else if (defaultPrefs.containsKey(prefKey)) {
			Log.e(logTag, "Unable to read pref '"+prefKey+"'. Reverting to default value '"+ defaultPrefs.get(prefKey)+"'.");
			return defaultPrefs.get(prefKey);
		} else {
			return null;
		}
	}

	public int getPrefAsInt(String prefKey) {
		return Integer.parseInt(getPrefAsString(prefKey));
	}

	public long getPrefAsLong(String prefKey) {
		return Long.parseLong(getPrefAsString(prefKey));
	}

	public boolean getPrefAsBoolean(String prefKey) { return getPrefAsString(prefKey).equalsIgnoreCase("true"); }

	//
	// Set Pref Values
	//

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

    public void setPref(String prefKey, long prefValue) {
        setPref(prefKey, ""+prefValue);
    }

    public void setPref(String prefKey, boolean prefValue) { setPref(prefKey, prefValue ? "true" : "false"); }

    //
    // [Re]Sync Pref Values
    //

	private void reSyncSinglePref(String prefKey) {
		this.cachedPrefs.remove(prefKey);
		String prefValue = getPrefAsString(prefKey);
	}

	public void reSyncPrefs(String prefKey) {
		Log.v(logTag, "Pref ReSync Triggered: '"+prefKey+"'");
		for (String thisKey : listPrefsKeys()) {
			if (	(	prefKey.equalsIgnoreCase("all")
					||	prefKey.equalsIgnoreCase(thisKey)
					)
				&&	this.cachedPrefs.containsKey(thisKey)
				) {
				reSyncSinglePref(thisKey);
			}
		}
	}

	public void reSyncPrefInExternalRoleViaContentProvider(String targetAppRole, String prefKey, Context context) {
		try {
			Cursor targetAppRoleResponse =
					context.getContentResolver().query(
					RfcxComm.getUri(targetAppRole, "prefs_resync", prefKey),
					RfcxComm.getProjection(targetAppRole, "prefs_resync"),
					null, null, null);
			if (targetAppRoleResponse != null) {
			//	Log.v(logTag, targetAppRoleResponse.toString());
				targetAppRoleResponse.close();
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

    //
    // Read/Write pref values to txt files
    //
	
	private String readPrefFromFile(String prefKey) {
		try {
			
	    		String filePath = prefsDirPath + "/" + prefKey.toLowerCase(Locale.US);
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
	
	private boolean writePrefToFile(String prefKey, String prefValue) {
		
		boolean writeSuccess = false;
		
		String filePath = prefsDirPath+"/"+prefKey.toLowerCase(Locale.US);
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

	public static void writeToGuardianRoleTxtFile(Context context, String logTag, String fileNameNoExt, String stringContents) {
		writeToGuardianRoleTxtFile(context, logTag, fileNameNoExt, stringContents, false);
	}

	public static void writeToGuardianRoleTxtFile(Context context, String logTag, String fileNameNoExt, String stringContents, boolean isPrivate) {
		String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt;
		File fileObj = new File(filePath);
		fileObj.mkdirs();
		FileUtils.chmod(context.getFilesDir().toString()+"/txt", "rw", isPrivate ? "" : "rw");
		FileUtils.delete(fileObj);
        try {
			BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
			outFile.write(stringContents);
			outFile.close();
			FileUtils.chmod(filePath, "rw", isPrivate ? "" : "rw");
        } catch (IOException e) {
			RfcxLog.logExc(logTag, e);
        }
	}
	
	public static String readFromGuardianRoleTxtFile(Context context, String logTag, String thisAppRole, String targetAppRole, String fileNameNoExt) {
	    	try {
	    		String appFilesDir = context.getFilesDir().getAbsolutePath();
	    		File txtFileObj = new File(appFilesDir+"/txt/"+fileNameNoExt);
	    		if (!thisAppRole.equalsIgnoreCase(targetAppRole)) {
					txtFileObj = new File(appFilesDir.substring(0,appFilesDir.lastIndexOf("/files")-(("."+thisAppRole.toLowerCase(Locale.US)).length()))+"."+targetAppRole.toLowerCase(Locale.US)+"/files/txt/"+fileNameNoExt);
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
		return (new File(context.getFilesDir().toString()+"/txt/"+fileNameNoExt)).exists();
	}
	
	private static String setOrCreatePrefsDirectory(Context context) {
		
		String prefsDir = context.getFilesDir().toString()+"/prefs";

		(new File(prefsDir)).mkdirs();
		FileUtils.chmod(prefsDir, "rw", "rw");
		
		return prefsDir;
		
	}

    //
    // Retrieve pref value from guardian role via content provider
    //

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

    //
    // Get formatted prefs
    //

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
		List<String> prefsKeysVals = new ArrayList<String>();
		for (String prefKey : prefsKeys) {
			prefsKeysVals.add(TextUtils.join("*", new String[] { prefKey, getPrefAsString(prefKey) } ));
		}
		String prefsBlob = TextUtils.join("|", prefsKeysVals);
		return StringUtils.getSha1HashOfString(prefsBlob);
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
		Log.e(logTag, prefsBlob.toString());
		return prefsBlob;
	}

	public JSONObject getPrefsAsJsonObj(boolean includeAllKeys, String[] includeTheseKeys) {

		List<String> prefsKeys = listPrefsKeys();
		Collections.sort(prefsKeys);

		JSONObject prefsObj = new JSONObject();

		for (String prefKey : prefsKeys) {
			if (includeAllKeys || ArrayUtils.doesStringArrayContainString(includeTheseKeys, prefKey)) {
				try {
					prefsObj.put(prefKey, getPrefAsString(prefKey));
				} catch (JSONException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}

		return prefsObj;
	}

	public JSONObject getPrefsAsJsonObj() {
		return getPrefsAsJsonObj(true, new String[] {} );
	}

	public JSONObject getPrefsAsJsonObj(String[] includeTheseKeys) {
		return getPrefsAsJsonObj(false, includeTheseKeys );
	}


    //
    // Prefs default/fallback values. Should be kept in sync with the prefs.xml file in guardian role resources
    //
	
	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
	    new HashMap<String, String>() {{

			put("enable_audio_capture", "true");
			put("enable_checkin_publish", "true");

			put("enable_cutoffs_battery", "true");
			put("enable_cutoffs_schedule_off_hours", "false");
			put("enable_cutoffs_sampling_ratio", "false");

			put("api_checkin_host", "checkins.rfcx.org");
	        put("api_checkin_protocol", "ssl");
	        put("api_checkin_port", "8883");
			put("enable_checkin_auth", "true");
			put("api_checkin_auth_creds", "[guid],[token]");

            put("api_rest_host", "api.rfcx.org");
            put("api_rest_protocol", "https");

	        put("api_ntp_host", "time.apple.com");
			put("api_sms_address", "+14154803657");
	        
			put("reboot_forced_daily_at", "23:54:00");
			
			put("audio_cycle_duration", "90");
			
			put("audio_schedule_off_hours", "23:56-23:58,23:58-00:00");
			put("audio_sampling_ratio", "1:2");

			put("checkin_cutoff_battery", "95");
			put("audio_cutoff_battery", "90");

			put("enable_cutoffs_sentinel_battery", "true");
			put("audio_cutoff_sentinel_battery", "15");
			put("checkin_cutoff_sentinel_battery", "15");
			
			put("audio_encode_codec", "opus");
			put("audio_encode_bitrate", "28672");
			put("audio_sample_rate", "24000");

			put("checkin_failure_thresholds", "15,30,50,70,90");
			put("checkin_failure_limit", "3");

			put("checkin_queue_filesize_limit", "80");
			put("checkin_sent_filesize_buffer", "80");
			put("checkin_stash_filesize_buffer", "160");
			put("checkin_archive_filesize_target", "32");

			put("checkin_requeue_bounds_hours", "10-14");

			put("checkin_meta_bundle_limit", "8");

			put("admin_enable_wifi", "true");
			put("admin_enable_tcp_adb", "true");
			put("admin_enable_wifi_socket", "true");
			put("admin_enable_ssh_server", "false");

			put("api_sntp_cycle_duration", "30");
			put("api_ping_cycle_duration", "30");

			put("admin_log_capture_cycle", "30");
			put("admin_log_capture_level", "Warn");
			put("admin_enable_log_capture", "false");

			put("admin_enable_geoposition_capture", "true");
			put("admin_geoposition_capture_cycle", "15");
			
			put("admin_screenshot_capture_cycle", "180");
			put("admin_enable_screenshot_capture", "false");

			put("admin_enable_sentinel_power", "true");
			put("admin_enable_sentinel_sensor", "true");

			put("admin_system_timezone", "[ Not Set ]");

			
			
	    }}
	);
	

	
}
