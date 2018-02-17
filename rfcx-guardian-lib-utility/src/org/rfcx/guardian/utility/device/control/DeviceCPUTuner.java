package org.rfcx.guardian.utility.device.control;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.util.Log;

public class DeviceCPUTuner {
	
	public DeviceCPUTuner(Context context, String appRole) {
		this.context = context;
		this.logTag = RfcxLog.generateLogTag("Utils", DeviceCPUTuner.class);
		this.shellCommands = new ShellCommands(context, appRole);
	}
	
	private Context context;
	private String logTag = RfcxLog.generateLogTag("Utils", DeviceCPUTuner.class);
	private ShellCommands shellCommands;
	
	public void writeConfiguration(int freq_min, int freq_max, int governor_up, int governor_down) {
		if (isInstalled(this.context)) {
			createOrUpdateDbProfile(this.context, this.logTag, freq_min, freq_max, governor_up, governor_down);
			overWritePrefsXml(this.context, this.logTag, freq_min, freq_max, governor_up, governor_down);
		} else {
			Log.e(this.logTag,"CPUTuner is NOT installed");
		}
	}
	
	private static final int wifiState = 0;
	private static final int gpsState = 0;
	private static final int bluetoothState = 1;
	private static final int mobiledataState = 2;
	private static final int backgroundSyncState = 2;
	private static final int virtualGovernor = 3;
	private static final int mobiledataConnectionState = 1;
	private static final int powersaveBias = 1000;
	private static final int AIRPLANEMODE = 0;
	
	private static String getXmlPrefString(int frequencyMin, int frequencyMax, int governorThresholdUp, int governorThresholdDown) { 
		return (new StringBuilder())
			.append("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>")
			.append("\n<map>")
			.append("\n<long name=\"prefKeyLastBoot\" value=\"1363225391032\" />")
			.append("\n<string name=\"prefKeyConfiguration\">Sophisticated</string>")
			.append("\n<string name=\"prefKeyCalcPowerUsageType\">3</string>")
			.append("\n<string name=\"prefKeyUnitSystem\">1</string>")
			.append("\n<string name=\"prefKeyUserLevel\">3</string>")
			.append("\n<boolean name=\"prefKeyEnableProfiles\" value=\"true\" />")
			.append("\n<boolean name=\"prefKeySaveConfigOnSwitch\" value=\"false\" />")
			.append("\n<string name=\"prefKeyStatusbarAddToChoice\">0</string>")
			.append("\n<boolean name=\"prefKeyUserLevelSet\" value=\"true\" />")
			.append("\n<int name=\"prefKeyMinFreqDefault\" value=\"").append(frequencyMin).append("\" />")
			.append("\n<string name=\"prefKeyMinFreq\">").append(frequencyMin).append("</string>")
			.append("\n<int name=\"prefKeyMaxFreqDefault\" value=\"").append(frequencyMax).append("\" />")
			.append("\n<string name=\"prefKeyMaxFreq\">").append(frequencyMax).append("</string>")
			.append("\n</map>")
			.append("\n").toString();
	}
	
	private static final int sqlRfcxProfilePrimaryKey = 7;
	private static final String sqlCheckIfProfileExists = (new StringBuilder()).append("\"SELECT COUNT(*) FROM cpuProfiles WHERE profileName='RFCx' AND _id=").append(sqlRfcxProfilePrimaryKey).append(";\"").toString();
	private static final String sqlDeleteRfcxProfile = "\"DELETE FROM cpuProfiles WHERE profileName='RFCx';\"";
	private static final String sqlUpdateTriggers = (new StringBuilder()).append("\"UPDATE triggers SET screenOffProfileId=").append(sqlRfcxProfilePrimaryKey).append(", batteryProfileId=").append(sqlRfcxProfilePrimaryKey).append(", powerProfileId=").append(sqlRfcxProfilePrimaryKey).append(", callInProgressProfileId=").append(sqlRfcxProfilePrimaryKey).append(", screenLockedProfileId=").append(sqlRfcxProfilePrimaryKey).append(";\"").toString();
	
	private static String getSqlInsertRfcxProfile(int frequencyMin, int frequencyMax, int governorThresholdUp, int governorThresholdDown) { 
		return (new StringBuilder()).append("\"INSERT INTO cpuProfiles VALUES (").append(sqlRfcxProfilePrimaryKey).append(", 'RFCx', 'conservative', ").append(frequencyMax).append(", ").append(frequencyMin).append(", ").append(wifiState).append(", ").append(gpsState).append(", ").append(bluetoothState).append(", ").append(mobiledataState).append(", ").append(governorThresholdUp).append(", ").append(governorThresholdDown).append(", ").append(backgroundSyncState).append(", ").append(virtualGovernor).append(", ").append(mobiledataConnectionState).append(", '', ").append(powersaveBias).append(", ").append(AIRPLANEMODE).append(", 0);\"").toString();
	}
	
	private static String getSqlUpdateRfcxProfile(int frequencyMin, int frequencyMax, int governorThresholdUp, int governorThresholdDown) { 
		return (new StringBuilder()).append("\"UPDATE cpuProfiles SET frequencyMax=").append(frequencyMax).append(", frequencyMin=").append(frequencyMin).append(", wifiState=").append(wifiState).append(", gpsState=").append(gpsState).append(", bluetoothState=").append(bluetoothState).append(", mobiledataState=").append(mobiledataState).append(", governorThresholdUp=").append(governorThresholdUp).append(", governorThresholdDown=").append(governorThresholdDown).append(", backgroundSyncState=").append(backgroundSyncState).append(", virtualGovernor=").append(virtualGovernor).append(", mobiledataConnectionState=").append(mobiledataConnectionState).append(", powersaveBias=").append(powersaveBias).append(", AIRPLANEMODE=").append(AIRPLANEMODE).append(" WHERE profileName='RFCx';\"").toString();
	}
	
	private void overWritePrefsXml(Context context, String logTag, int freq_min, int freq_max, int governor_up, int governor_down) {
		
		String tmpPrefsFilePath = context.getFilesDir().getAbsolutePath().toString()+"/txt/cputuner.xml";
		File tmpPrefsFile = new File(tmpPrefsFilePath);			
        if (tmpPrefsFile.exists()) { tmpPrefsFile.delete(); }
        
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(tmpPrefsFile));
        	outFile.write(getXmlPrefString(freq_min, freq_max, governor_up, governor_down));
        	outFile.close();
        	FileUtils.chmod(tmpPrefsFile, 0755);
        	if (tmpPrefsFile.exists()) {
        		this.shellCommands.executeCommandAsRoot("cp "+tmpPrefsFilePath+" "+getPrefsPath(context));
        	}
        } catch (IOException e) {
        		RfcxLog.logExc(logTag, e);
        }	
	}
	
	private void createOrUpdateDbProfile(Context context, String logTag, int freq_min, int freq_max, int governor_up, int governor_down) {

		String preQuery = (new StringBuilder()).append("sqlite3 ").append(getDbPath(context)).append(" ").toString();
		if (this.shellCommands.executeCommandAsRootAndSearchOutput(preQuery+sqlCheckIfProfileExists, "0")) {
			Log.d(logTag, "Inserting RFCx profile into CPUTuner database for the first time.");
			this.shellCommands.executeCommandAsRoot(preQuery+sqlDeleteRfcxProfile);
			this.shellCommands.executeCommandAsRoot(preQuery+getSqlInsertRfcxProfile(freq_min, freq_max, governor_up, governor_down));
		} else {
			Log.v(logTag, "Updating RFCx profile in CPUTuner database.");
			this.shellCommands.executeCommandAsRoot(preQuery+getSqlUpdateRfcxProfile(freq_min, freq_max, governor_up, governor_down));
		}
		
		this.shellCommands.executeCommandAsRoot(preQuery+sqlUpdateTriggers);
	}
	
	private static boolean isInstalled(Context context) {
		return (new File(getAppDirPath(context))).exists();
	}
	
	private static String getAppDirPath(Context context) {
		return (new StringBuilder())
				.append(FileUtils.getSystemApplicationDirPath(context))
				.append("ch.amana.android.cputuner")
				.toString();
	}
	
	private static String getPrefsPath(Context context) {
		return (new StringBuilder())
				.append(getAppDirPath(context))
				.append("/shared_prefs/ch.amana.android.cputuner_preferences.xml")
				.toString();
	}
	
	private static String getDbPath(Context context) {
		return (new StringBuilder())
				.append(getAppDirPath(context))
				.append("/databases/cputuner")
				.toString();
	}

	
}
