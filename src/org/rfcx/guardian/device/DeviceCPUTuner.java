package org.rfcx.guardian.device;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.FileUtils;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class DeviceCPUTuner {

	private static final String TAG = "RfcxGuardian-"+DeviceCPUTuner.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	private static final int frequencyMin = 30720;
	private static final int frequencyMax = 122880;//61440;
	private static final int wifiState = 2;
	private static final int gpsState = 0;
	private static final int bluetoothState = 1;
	private static final int mobiledataState = 1;
	private static final int governorThresholdUp = 98;
	private static final int governorThresholdDown = 90;
	private static final int backgroundSyncState = 2;
	private static final int virtualGovernor = 3;
	private static final int mobiledataConnectionState = 0;
	private static final int powersaveBias = 1000;
	private static final int AIRPLANEMODE = 0;
	
	
	private static final String prefsXml = 
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

	private static final String prefsPath = "/data/data/ch.amana.android.cputuner/shared_prefs/ch.amana.android.cputuner_preferences.xml";
	private static final String dbPath = "/data/data/ch.amana.android.cputuner/databases/cputuner";
	
	public void set(Context context) {
		if (isInstalled()) {
			createOrUpdateDbProfile(context);
			overWritePrefsXml(context);
		} else {
			Log.e(TAG,"cpuTuner is NOT installed");
		}
	}
	
	private static void overWritePrefsXml(Context context) {
		String tmpPrefsFilePath = context.getFilesDir().getAbsolutePath().toString()+"/txt/cpuTuner.xml";
		File tmpPrefsFile = new File(tmpPrefsFilePath);			
        if (tmpPrefsFile.exists()) { tmpPrefsFile.delete(); }
        try {
        	BufferedWriter outFile = new BufferedWriter(new FileWriter(tmpPrefsFile));
        	outFile.write(prefsXml);
        	outFile.close();
        	(new FileUtils()).chmod(tmpPrefsFile, 0755);
        	if (tmpPrefsFile.exists()) {
        		(new ShellCommands()).executeCommandAsRoot("cp "+tmpPrefsFilePath+" "+prefsPath,null,context);
        	}
        } catch (IOException e) {
        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
        }	
	}
	
	private static void createOrUpdateDbProfile(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		ShellCommands sh = new ShellCommands();
		String pre = "sqlite3 "+dbPath+" ";
		if (sh.executeCommandAsRoot(pre+"\"SELECT COUNT(*) FROM cpuProfiles WHERE profileName='RFCx' AND _id=7;\"","0",context)) {
			Log.d(TAG, "Inserting RFCx profile into CPUTuner database for the first time.");
			sh.executeCommandAsRoot(pre+"\"DELETE FROM cpuProfiles WHERE profileName='RFCx';\"", null, context);
			sh.executeCommandAsRoot(pre+"\"INSERT INTO cpuProfiles VALUES (7, 'RFCx', 'conservative', "+frequencyMax+", "+frequencyMin+", "+wifiState+", "+gpsState+", "+bluetoothState+", "+mobiledataState+", "+governorThresholdUp+", "+governorThresholdDown+", "+backgroundSyncState+", "+virtualGovernor+", "+mobiledataConnectionState+", '', "+powersaveBias+", "+AIRPLANEMODE+", 0);\"", null, context);
		} else {
			Log.v(TAG, "Updating RFCx profile in CPUTuner database.");
			sh.executeCommandAsRoot(pre+"\"UPDATE cpuProfiles SET frequencyMax="+frequencyMax+", frequencyMin="+frequencyMin+", wifiState="+wifiState+", gpsState="+gpsState+", bluetoothState="+bluetoothState+", mobiledataState="+mobiledataState+", governorThresholdUp="+governorThresholdUp+", governorThresholdDown="+governorThresholdDown+", backgroundSyncState="+backgroundSyncState+", virtualGovernor="+virtualGovernor+", mobiledataConnectionState="+mobiledataConnectionState+", powersaveBias="+powersaveBias+", AIRPLANEMODE="+AIRPLANEMODE+" WHERE profileName='RFCx';\"", null, context);
		}
		
		sh.executeCommandAsRoot(pre+"\"UPDATE triggers SET screenOffProfileId=7, batteryProfileId=7, powerProfileId=7, callInProgressProfileId=7, screenLockedProfileId=7;\"", null, context);
	}
	
	private static boolean isInstalled() {
		return (new File(prefsPath.substring(0, prefsPath.lastIndexOf("/")))).exists();
	}
	
	
}
