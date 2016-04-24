package org.rfcx.guardian.installer.device;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rfcx.guardian.installer.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class DeviceCPUTuner {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+DeviceCPUTuner.class.getSimpleName();
	
	private static final String cpuTunerAppName = "ch.amana.android.cputuner";
	
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
		return
			"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
			+"\n<map>"
				+"\n<long name=\"prefKeyLastBoot\" value=\"1363225391032\" />"
				+"\n<string name=\"prefKeyConfiguration\">Sophisticated</string>"
				+"\n<string name=\"prefKeyCalcPowerUsageType\">3</string>"
				+"\n<string name=\"prefKeyUnitSystem\">1</string>"
				+"\n<string name=\"prefKeyUserLevel\">3</string>"
				+"\n<boolean name=\"prefKeyEnableProfiles\" value=\"true\" />"
				+"\n<boolean name=\"prefKeySaveConfigOnSwitch\" value=\"false\" />"
				+"\n<string name=\"prefKeyStatusbarAddToChoice\">0</string>"
				+"\n<boolean name=\"prefKeyUserLevelSet\" value=\"true\" />"
				+"\n<int name=\"prefKeyMinFreqDefault\" value=\""+frequencyMin+"\" />"
				+"\n<string name=\"prefKeyMinFreq\">"+frequencyMin+"</string>"
				+"\n<int name=\"prefKeyMaxFreqDefault\" value=\""+frequencyMax+"\" />"
				+"\n<string name=\"prefKeyMaxFreq\">"+frequencyMax+"</string>"
			+"\n</map>"
			+"\n";
	}
	
	private static final int sqlRfcxProfilePrimaryKey = 7;
	private static final String sqlCheckIfProfileExists = "SELECT COUNT(*) FROM cpuProfiles WHERE profileName='RFCx' AND _id="+sqlRfcxProfilePrimaryKey+";";
	private static final String sqlDeleteRfcxProfile = "DELETE FROM cpuProfiles WHERE profileName='RFCx';";
	private static final String sqlUpdateTriggers = "UPDATE triggers SET screenOffProfileId="+sqlRfcxProfilePrimaryKey+", batteryProfileId="+sqlRfcxProfilePrimaryKey+", powerProfileId="+sqlRfcxProfilePrimaryKey+", callInProgressProfileId="+sqlRfcxProfilePrimaryKey+", screenLockedProfileId="+sqlRfcxProfilePrimaryKey+";";
	
	private static String getSqlInsertRfcxProfile(int frequencyMin, int frequencyMax, int governorThresholdUp, int governorThresholdDown) { 
		return "INSERT INTO cpuProfiles VALUES ("+sqlRfcxProfilePrimaryKey+", 'RFCx', 'conservative', "+frequencyMax+", "+frequencyMin+", "+wifiState+", "+gpsState+", "+bluetoothState+", "+mobiledataState+", "+governorThresholdUp+", "+governorThresholdDown+", "+backgroundSyncState+", "+virtualGovernor+", "+mobiledataConnectionState+", '', "+powersaveBias+", "+AIRPLANEMODE+", 0);";
	}
	
	private static String getSqlUpdateRfcxProfile(int frequencyMin, int frequencyMax, int governorThresholdUp, int governorThresholdDown) { 
		return "UPDATE cpuProfiles SET frequencyMax="+frequencyMax+", frequencyMin="+frequencyMin+", wifiState="+wifiState+", gpsState="+gpsState+", bluetoothState="+bluetoothState+", mobiledataState="+mobiledataState+", governorThresholdUp="+governorThresholdUp+", governorThresholdDown="+governorThresholdDown+", backgroundSyncState="+backgroundSyncState+", virtualGovernor="+virtualGovernor+", mobiledataConnectionState="+mobiledataConnectionState+", powersaveBias="+powersaveBias+", AIRPLANEMODE="+AIRPLANEMODE+" WHERE profileName='RFCx';";
	}
	
	public void set(Context context) {
		if (isInstalled(context)) {
			createOrUpdateDbProfile(context);
			overWritePrefsXml(context);
		} else {
			Log.e(TAG,"CPUTuner is NOT installed");
		}
	}
	
	private static void overWritePrefsXml(Context context) {
		
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		String tmpPrefsFilePath = context.getFilesDir().getAbsolutePath().toString()+"/txt/cputuner.xml";
		File tmpPrefsFile = new File(tmpPrefsFilePath);			
        if (tmpPrefsFile.exists()) { tmpPrefsFile.delete(); }
        
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(tmpPrefsFile));
        	outFile.write(getXmlPrefString(app.CPUTUNER_FREQ_MIN, app.CPUTUNER_FREQ_MAX, app.CPUTUNER_GOVERNOR_UP, app.CPUTUNER_GOVERNOR_DOWN));
        	outFile.close();
        	(new FileUtils()).chmod(tmpPrefsFile, 0755);
        	if (tmpPrefsFile.exists()) {
        		(new ShellCommands()).executeCommand("cp "+tmpPrefsFilePath+" "+getPrefsPath(context),null,true,context);
        	}
        } catch (IOException e) {
        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
        }	
	}
	
	private static void createOrUpdateDbProfile(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		ShellCommands sh = new ShellCommands();
		String pre = "sqlite3 "+getDbPath(context)+" ";
		if (sh.executeCommand(pre+"\""+sqlCheckIfProfileExists+"\"","0",true,context)) {
			Log.d(TAG, "Inserting RFCx profile into CPUTuner database for the first time.");
			sh.executeCommand(pre+"\""+sqlDeleteRfcxProfile+"\"", null, true, context);
			sh.executeCommand(pre+"\""+getSqlInsertRfcxProfile(app.CPUTUNER_FREQ_MIN, app.CPUTUNER_FREQ_MAX, app.CPUTUNER_GOVERNOR_UP, app.CPUTUNER_GOVERNOR_DOWN)+"\"", null, true, context);
		} else {
			Log.v(TAG, "Updating RFCx profile in CPUTuner database.");
			sh.executeCommand(pre+"\""+getSqlUpdateRfcxProfile(app.CPUTUNER_FREQ_MIN, app.CPUTUNER_FREQ_MAX, app.CPUTUNER_GOVERNOR_UP, app.CPUTUNER_GOVERNOR_DOWN)+"\"", null, true, context);
		}
		
		sh.executeCommand(pre+"\""+sqlUpdateTriggers+"\"", null, true, context);
	}
	
	private static boolean isInstalled(Context context) {
		String prefsPath = getPrefsPath(context);
		return (new File(prefsPath.substring(0, prefsPath.lastIndexOf("/")))).exists();
	}
	
	private static String getCpuTunerFilesDir(Context context) {
		String rfcxAppFilesDir = context.getFilesDir().getAbsolutePath();
		return rfcxAppFilesDir.substring(0, rfcxAppFilesDir.indexOf("org.rfcx.guardian."))+cpuTunerAppName;
	}
	
	private static String getPrefsPath(Context context) {
		return getCpuTunerFilesDir(context) +"/shared_prefs/ch.amana.android.cputuner_preferences.xml";
	}
	
	private static String getDbPath(Context context) {
		return getCpuTunerFilesDir(context) +"/databases/cputuner";
	}
	
}
