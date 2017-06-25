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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class RfcxPrefs {

	public RfcxPrefs(Context context, String appRole) {
		this.logTag = (new StringBuilder()).append("Rfcx-").append(appRole).append("-").append(RfcxPrefs.class.getSimpleName()).toString();
		this.thisAppRole = appRole.toLowerCase(Locale.US);
		this.context = context;
//		setDefaultPrefs();
		detectOrCreatePrefsDirectory(context);
	}
	
	private String logTag = (new StringBuilder()).append("Rfcx-Utils-").append(RfcxPrefs.class.getSimpleName()).toString();
	
	private Context context = null;
	private String thisAppRole = null;
	private static final String prefsParentDirPath = (new StringBuilder()).append(Environment.getDownloadCacheDirectory().getAbsolutePath()).append("/rfcx").toString();
	private static final String prefsDirPath = (new StringBuilder()).append(prefsParentDirPath).append("/prefs").toString();
	
	private Map<String, String> cachedPrefs = new HashMap<String, String>();
//	private Map<String, String> defaultPrefs = new HashMap<String, String>();
	
	// Getters and Setters
	
	public String getPrefAsString(String prefKey) {
		if (this.cachedPrefs.containsKey(prefKey)) {
			return this.cachedPrefs.get(prefKey);
		} else if (readPrefFromFile(prefKey) != null) {
			this.cachedPrefs.put(prefKey, readPrefFromFile(prefKey));
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
		writeToGuardianRoleTxtFile(this.context, "version",versionName);
	}
	
	public String getVersionFromFile(String targetAppRole) {
		return readFromGuardianRoleTxtFile(this.context, this.thisAppRole, targetAppRole.toLowerCase(Locale.US), "version");
	}

	private void writeToGuardianRoleTxtFile(Context context, String fileNameNoExt, String stringContents) {
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
	
	private String readFromGuardianRoleTxtFile(Context context, String thisAppRole, String targetAppRole, String fileNameNoExt) {
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

	
	// this may require root...
	private void detectOrCreatePrefsDirectory(Context context) {
		if (!(new File(prefsParentDirPath)).exists()) {
			ShellCommands.executeCommand("mkdir "+prefsParentDirPath+"; chmod a+rw "+prefsParentDirPath+";", null, true, context);
		}
	}
	

	private static final Map<String, String> defaultPrefs = Collections.unmodifiableMap(
	    new HashMap<String, String>() {{
	    	
	        put("api_url_base", "https://api.rfcx.org");
	        
	        put("service_monitor_cycle_duration", "600000");
	        
			put("reboot_forced_daily_at", "23:55:00");

			put("install_api_registration_token", "");
			put("install_cycle_duration", "3600000");
			put("install_offline_toggle_threshold", "900000");
			
			put("cputuner_freq_min", "30720");
			put("cputuner_freq_max", "122880"); // options: 30720, 49152, 61440, 122880, 245760, 320000, 480000, 
			put("cputuner_governor_up", "98");
			put("cputuner_governor_down", "95");
			
			put("audio_cycle_duration", "90000");
			put("audio_schedule_off_hours", "21:00-22:30,00:15-04:45");
			
			put("audio_encode_codec", "opus");
			put("audio_encode_bitrate", "16384");
			put("audio_sample_rate", "12000");
			put("audio_encode_quality", "7");
			put("audio_encode_skip_threshold", "3");
			put("audio_encode_cycle_pause", "5000");

			put("checkin_battery_cutoff", "90");
			put("audio_battery_cutoff", "60");
			put("install_battery_cutoff", "30");
			
			put("checkin_cycle_pause", "5000");
			put("checkin_skip_threshold", "5");
			put("checkin_stash_threshold", "120");
			put("checkin_archive_threshold", "960");
			
	    }}
	);
	
	public static String[] listPrefsKeys() {
		List<String> prefsKeys = new ArrayList<String>();
		
		Iterator prefsIterator = defaultPrefs.entrySet().iterator();
		while (prefsIterator.hasNext()) {
	        Map.Entry prefsOption = (Map.Entry)prefsIterator.next();
	        prefsKeys.add(prefsOption.getKey().toString());
	        prefsIterator.remove(); // avoids a ConcurrentModificationException
		}
		
		return prefsKeys.toArray(new String[prefsKeys.size()]);
	}
	
//	private void setDefaultPrefs() {
//		
//		defaultPrefs.put("api_url_base", "https://api.rfcx.org");
//
//		defaultPrefs.put("service_monitor_cycle_duration", "600000");
//
//		defaultPrefs.put("reboot_forced_daily_at", "23:55:00");
//		
//		defaultPrefs.put("install_cycle_duration", "3600000");
//		defaultPrefs.put("install_offline_toggle_threshold", "900000");
//		defaultPrefs.put("install_api_registration_token", "ABCDEFGH");
//		
//		defaultPrefs.put("cputuner_freq_min", "30720");
//		defaultPrefs.put("cputuner_freq_max", "245760"); // options: 30720, 49152, 61440, 122880, 245760, 320000, 480000, 
//		defaultPrefs.put("cputuner_governor_up", "98");
//		defaultPrefs.put("cputuner_governor_down", "95");
//		
//		defaultPrefs.put("audio_cycle_duration", "90000");
//		defaultPrefs.put("audio_schedule_off_hours", "00:15-03:00,03:00-04:45");
//		
//		defaultPrefs.put("audio_encode_codec", "opus");
//		defaultPrefs.put("audio_encode_bitrate", "16384");
//		defaultPrefs.put("audio_sample_rate", "12000");
//		defaultPrefs.put("audio_encode_quality", "7");
//		defaultPrefs.put("audio_encode_skip_threshold", "3");
//		defaultPrefs.put("audio_encode_cycle_pause", "5000");
//
//		defaultPrefs.put("checkin_battery_cutoff", "90");
//		defaultPrefs.put("audio_battery_cutoff", "60");
//		defaultPrefs.put("install_battery_cutoff", "30");
//		
//		defaultPrefs.put("checkin_cycle_pause", "5000");
//		defaultPrefs.put("checkin_skip_threshold", "5");
//		defaultPrefs.put("checkin_stash_threshold", "320");
//		defaultPrefs.put("checkin_archive_threshold", "960");
//		
//	}
	
}
