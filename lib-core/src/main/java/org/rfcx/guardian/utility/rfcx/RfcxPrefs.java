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
	
	private final String logTag;
	
	private final Context context;
	private final String thisAppRole;
	private final String prefsDirPath;
	
	private final Map<String, String> cachedPrefs = new HashMap<String, String>();

	public String prefsSha1FullApiSync = null;
	public long prefsTimestampLastFullApiSync = 0;

	public static final class Pref {

		public static final String ENABLE_AUDIO_CAPTURE = "enable_audio_capture";
		public static final String ENABLE_AUDIO_STREAM = "enable_audio_stream";
		public static final String ENABLE_AUDIO_VAULT = "enable_audio_vault";
		public static final String ENABLE_AUDIO_CLASSIFY = "enable_audio_classify";

		public static final String ENABLE_CHECKIN_PUBLISH = "enable_checkin_publish";
		public static final String ENABLE_CHECKIN_ARCHIVE = "enable_checkin_archive";

		public static final String API_MQTT_HOST = "api_mqtt_host";
		public static final String API_MQTT_PROTOCOL = "api_mqtt_protocol";
		public static final String API_MQTT_PORT = "api_mqtt_port";
		public static final String ENABLE_MQTT_AUTH = "enable_mqtt_auth";
		public static final String API_MQTT_AUTH_CREDS = "api_mqtt_auth_creds";

		public static final String API_REST_HOST = "api_rest_host";
		public static final String API_REST_PROTOCOL = "api_rest_protocol";

		public static final String API_NTP_HOST = "api_ntp_host";
		public static final String API_SMS_ADDRESS = "api_sms_address";

		public static final String API_PROTOCOL_ESCALATION_ORDER = "api_protocol_escalation_order";

		public static final String REBOOT_FORCED_DAILY_AT = "reboot_forced_daily_at";

		public static final String AUDIO_CYCLE_DURATION = "audio_cycle_duration";

		public static final String ENABLE_CUTOFFS_SCHEDULE_OFF_HOURS = "enable_cutoffs_schedule_off_hours";
		public static final String AUDIO_SCHEDULE_OFF_HOURS = "audio_schedule_off_hours";
		public static final String API_PING_SCHEDULE_OFF_HOURS = "api_ping_schedule_off_hours";
		public static final String AUDIO_CLASSIFY_SCHEDULE_OFF_HOURS = "audio_classify_schedule_off_hours";

		public static final String ENABLE_CUTOFFS_SAMPLING_RATIO = "enable_cutoffs_sampling_ratio";
		public static final String AUDIO_SAMPLING_RATIO = "audio_sampling_ratio";

		public static final String ENABLE_CUTOFFS_INTERNAL_BATTERY = "enable_cutoffs_internal_battery";
		public static final String CHECKIN_CUTOFF_INTERNAL_BATTERY = "checkin_cutoff_internal_battery";
		public static final String AUDIO_CUTOFF_INTERNAL_BATTERY = "audio_cutoff_internal_battery";
		public static final String INSTALL_CUTOFF_INTERNAL_BATTERY = "install_cutoff_internal_battery";

		public static final String ENABLE_CUTOFFS_SENTINEL_BATTERY = "enable_cutoffs_sentinel_battery";
		public static final String CHECKIN_CUTOFF_SENTINEL_BATTERY = "checkin_cutoff_sentinel_battery";
		public static final String AUDIO_CUTOFF_SENTINEL_BATTERY = "audio_cutoff_sentinel_battery";

		public static final String AUDIO_CAPTURE_SAMPLE_RATE = "audio_capture_sample_rate";
		public static final String AUDIO_CAPTURE_GAIN = "audio_capture_gain";

		public static final String AUDIO_STREAM_SAMPLE_RATE = "audio_stream_sample_rate";
		public static final String AUDIO_STREAM_CODEC = "audio_stream_codec";
		public static final String AUDIO_STREAM_BITRATE = "audio_stream_bitrate";

		public static final String AUDIO_VAULT_SAMPLE_RATE = "audio_vault_sample_rate";
		public static final String AUDIO_VAULT_CODEC = "audio_vault_codec";
		public static final String AUDIO_VAULT_BITRATE = "audio_vault_bitrate";

		public static final String CHECKIN_FAILURE_THRESHOLDS = "checkin_failure_thresholds";
		public static final String CHECKIN_FAILURE_LIMIT = "checkin_failure_limit";

		public static final String CHECKIN_QUEUE_FILESIZE_LIMIT = "checkin_queue_filesize_limit";
		public static final String CHECKIN_SENT_FILESIZE_BUFFER = "checkin_sent_filesize_buffer";
		public static final String CHECKIN_STASH_FILESIZE_BUFFER = "checkin_stash_filesize_buffer";
		public static final String CHECKIN_ARCHIVE_FILESIZE_TARGET = "checkin_archive_filesize_target";

		public static final String CHECKIN_REQUEUE_BOUNDS_HOURS = "checkin_requeue_bounds_hours";

		public static final String CHECKIN_META_SEND_BUNDLE_LIMIT = "checkin_meta_send_bundle_limit";
		public static final String CHECKIN_META_QUEUE_FILESIZE_LIMIT = "checkin_meta_queue_filesize_limit";

		public static final String ADMIN_ENABLE_TCP_ADB = "admin_enable_tcp_adb";
		public static final String ADMIN_ENABLE_WIFI_SOCKET = "admin_enable_wifi_socket";
		public static final String ADMIN_ENABLE_SSH_SERVER = "admin_enable_ssh_server";

		public static final String API_CLOCK_SYNC_CYCLE_DURATION = "api_clock_sync_cycle_duration";
		public static final String API_PING_CYCLE_DURATION = "api_ping_cycle_duration";
		public static final String API_PING_CYCLE_FIELDS = "api_ping_cycle_fields";

		public static final String ADMIN_ENABLE_LOG_CAPTURE = "admin_enable_log_capture";
		public static final String ADMIN_LOG_CAPTURE_CYCLE = "admin_log_capture_cycle";
		public static final String ADMIN_LOG_CAPTURE_LEVEL = "admin_log_capture_level";

		public static final String ADMIN_ENABLE_GEOPOSITION_CAPTURE = "admin_enable_geoposition_capture";
		public static final String ADMIN_GEOPOSITION_CAPTURE_CYCLE = "admin_geoposition_capture_cycle";

		public static final String ADMIN_ENABLE_SCREENSHOT_CAPTURE = "admin_enable_screenshot_capture";
		public static final String ADMIN_SCREENSHOT_CAPTURE_CYCLE = "admin_screenshot_capture_cycle";

		public static final String ADMIN_ENABLE_CAMERA_CAPTURE = "admin_enable_camera_capture";
		public static final String ADMIN_CAMERA_CAPTURE_CYCLE = "admin_camera_capture_cycle";

		public static final String ADMIN_ENABLE_SENTINEL_POWER = "admin_enable_sentinel_power";
		public static final String ADMIN_ENABLE_SENTINEL_SENSOR = "admin_enable_sentinel_sensor";
		public static final String ADMIN_VERBOSE_SENTINEL = "admin_verbose_sentinel";

		public static final String ADMIN_SYSTEM_TIMEZONE = "admin_system_timezone";
		public static final String ADMIN_SYSTEM_SETTINGS_OVERRIDE = "admin_system_settings_override";

		public static final String ADMIN_ENABLE_AIRPLANE_MODE = "admin_enable_airplane_mode";
		public static final String ADMIN_ENABLE_WIFI_CONNECTION = "admin_enable_wifi_connection";
		public static final String ADMIN_ENABLE_WIFI_HOTSPOT = "admin_enable_wifi_hotspot";
		public static final String ADMIN_WIFI_HOTSPOT_PASSWORD = "admin_wifi_hotspot_password";

		public static final String API_SATELLITE_PROTOCOL = "api_satellite_protocol";

	}


	//
	// Prefs default/fallback values. Should be kept in sync with the prefs.xml file in guardian role resources
	//

	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
			new HashMap<String, String>() {{

			put(Pref.ENABLE_AUDIO_CAPTURE, "true");
			put(Pref.ENABLE_AUDIO_STREAM, "true");
			put(Pref.ENABLE_AUDIO_VAULT, "false");
			put(Pref.ENABLE_AUDIO_CLASSIFY, "false");

			put(Pref.ENABLE_CHECKIN_PUBLISH, "true");
			put(Pref.ENABLE_CHECKIN_ARCHIVE, "true");

			put(Pref.API_MQTT_HOST, "api-mqtt.rfcx.org");
			put(Pref.API_MQTT_PROTOCOL, "ssl");
			put(Pref.API_MQTT_PORT, "8883");
			put(Pref.ENABLE_MQTT_AUTH, "true");
			put(Pref.API_MQTT_AUTH_CREDS, "[guid],[token]");

			put(Pref.API_REST_HOST, "api.rfcx.org");
			put(Pref.API_REST_PROTOCOL, "https");

			put(Pref.API_NTP_HOST, "time.apple.com");
			put(Pref.API_SMS_ADDRESS, "+14154803657");

			put(Pref.API_PROTOCOL_ESCALATION_ORDER, "mqtt,rest,sms,sat");
			
			put(Pref.API_SATELLITE_PROTOCOL, "off");

			put(Pref.REBOOT_FORCED_DAILY_AT, "23:55:00");

			put(Pref.AUDIO_CYCLE_DURATION, "90");

			put(Pref.ENABLE_CUTOFFS_SCHEDULE_OFF_HOURS, "false");
			put(Pref.AUDIO_SCHEDULE_OFF_HOURS, "23:55-23:56,23:57-23:59");
			put(Pref.API_PING_SCHEDULE_OFF_HOURS, "23:55-23:56,23:57-23:59");
			put(Pref.AUDIO_CLASSIFY_SCHEDULE_OFF_HOURS, "23:55-23:56,23:57-23:59");

			put(Pref.ENABLE_CUTOFFS_SAMPLING_RATIO, "false");
			put(Pref.AUDIO_SAMPLING_RATIO, "1:2");

			put(Pref.ENABLE_CUTOFFS_INTERNAL_BATTERY, "false");
			put(Pref.CHECKIN_CUTOFF_INTERNAL_BATTERY, "100");
			put(Pref.AUDIO_CUTOFF_INTERNAL_BATTERY, "100");
			put(Pref.INSTALL_CUTOFF_INTERNAL_BATTERY, "10");

			put(Pref.ENABLE_CUTOFFS_SENTINEL_BATTERY, "true");
			put(Pref.CHECKIN_CUTOFF_SENTINEL_BATTERY, "30");
			put(Pref.AUDIO_CUTOFF_SENTINEL_BATTERY, "30");

			put(Pref.AUDIO_CAPTURE_SAMPLE_RATE, "12000");
			put(Pref.AUDIO_CAPTURE_GAIN, "1.0");

			put(Pref.AUDIO_STREAM_SAMPLE_RATE, "12000");
			put(Pref.AUDIO_STREAM_CODEC, "opus");
			put(Pref.AUDIO_STREAM_BITRATE, "16384");

			put(Pref.AUDIO_VAULT_SAMPLE_RATE, "12000");
			put(Pref.AUDIO_VAULT_CODEC, "flac");
			put(Pref.AUDIO_VAULT_BITRATE, "16384");

			put(Pref.CHECKIN_FAILURE_THRESHOLDS, "15,30,50,70,90");
			put(Pref.CHECKIN_FAILURE_LIMIT, "3");

			put(Pref.CHECKIN_QUEUE_FILESIZE_LIMIT, "80");
			put(Pref.CHECKIN_SENT_FILESIZE_BUFFER, "80");
			put(Pref.CHECKIN_STASH_FILESIZE_BUFFER, "160");
			put(Pref.CHECKIN_ARCHIVE_FILESIZE_TARGET, "32");

			put(Pref.CHECKIN_REQUEUE_BOUNDS_HOURS, "10-14");

			put(Pref.CHECKIN_META_SEND_BUNDLE_LIMIT, "16");
			put(Pref.CHECKIN_META_QUEUE_FILESIZE_LIMIT, "8");

			put(Pref.ADMIN_ENABLE_AIRPLANE_MODE, "false");
			put(Pref.ADMIN_ENABLE_WIFI_CONNECTION, "false");
			put(Pref.ADMIN_ENABLE_WIFI_HOTSPOT, "true");
			put(Pref.ADMIN_ENABLE_TCP_ADB, "true");
			put(Pref.ADMIN_ENABLE_WIFI_SOCKET, "true");
			put(Pref.ADMIN_ENABLE_SSH_SERVER, "false");

			put(Pref.API_CLOCK_SYNC_CYCLE_DURATION, "30");

			put(Pref.API_PING_CYCLE_DURATION, "30");
			put(Pref.API_PING_CYCLE_FIELDS, "all");

			put(Pref.ADMIN_ENABLE_LOG_CAPTURE, "false");
			put(Pref.ADMIN_LOG_CAPTURE_CYCLE, "30");
			put(Pref.ADMIN_LOG_CAPTURE_LEVEL, "Warn");

			put(Pref.ADMIN_ENABLE_GEOPOSITION_CAPTURE, "false");
			put(Pref.ADMIN_GEOPOSITION_CAPTURE_CYCLE, "20");

			put(Pref.ADMIN_ENABLE_SCREENSHOT_CAPTURE, "false");
			put(Pref.ADMIN_SCREENSHOT_CAPTURE_CYCLE, "180");

			put(Pref.ADMIN_ENABLE_CAMERA_CAPTURE, "false");
			put(Pref.ADMIN_CAMERA_CAPTURE_CYCLE, "60");

			put(Pref.ADMIN_ENABLE_SENTINEL_POWER, "true");
			put(Pref.ADMIN_ENABLE_SENTINEL_SENSOR, "false");
			put(Pref.ADMIN_VERBOSE_SENTINEL, "false");

			put(Pref.ADMIN_SYSTEM_TIMEZONE, "[ Not Set ]");
			put(Pref.ADMIN_SYSTEM_SETTINGS_OVERRIDE, "auto_time_zone:system,i,0;");
			put(Pref.ADMIN_WIFI_HOTSPOT_PASSWORD, "rfcxrfcx");

		}}
	);



	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		
		String newPrefValue;
		
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
	public float getPrefAsFloat(String prefKey) {
		return Float.parseFloat(getPrefAsString(prefKey));
	}
	public boolean getPrefAsBoolean(String prefKey) { return getPrefAsString(prefKey).equalsIgnoreCase("true"); }

	//
	// Get Default Pref Values
	//

	public String getDefaultPrefValueAsString(String prefKey) {
		if (defaultPrefs.containsKey(prefKey)) {
			return defaultPrefs.get(prefKey);
		} else {
			return null;
		}
	}

	public int getDefaultPrefValueAsInt(String prefKey) { return Integer.parseInt(getDefaultPrefValueAsString(prefKey)); }
	public long getDefaultPrefValueAsLong(String prefKey) { return Long.parseLong(getDefaultPrefValueAsString(prefKey)); }
	public float getDefaultPrefValueAsFloat(String prefKey) { return Float.parseFloat(getDefaultPrefValueAsString(prefKey)); }
	public boolean getDefaultPrefValueAsBoolean(String prefKey) { return getDefaultPrefValueAsString(prefKey).equalsIgnoreCase("true"); }

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

	public static void deleteGuardianRoleTxtFile(Context context, String fileNameNoExt) {
		String filePath = context.getFilesDir().toString()+"/txt/"+fileNameNoExt;
		File fileObj = new File(filePath);
		FileUtils.delete(fileObj);
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






}
