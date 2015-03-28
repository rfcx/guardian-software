package org.rfcx.guardian.utility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class ExtCPUTuner {

	private static final String TAG = "RfcxGuardian-"+ExtCPUTuner.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private ShellCommands shellCommands = new ShellCommands();
	private FileUtils fileUtils = new FileUtils();
	
	private static final String prefsPath = "/data/data/ch.amana.android.cputuner/shared_prefs/ch.amana.android.cputuner_preferences.xml";
	
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
			+"\n<int name=\"prefKeyMinFreqDefault\" value=\"30720\" />"
			+"\n<string name=\"prefKeyMinFreq\">30720</string>"
			+"\n<int name=\"prefKeyMaxFreqDefault\" value=\"122880\" />"
			+"\n<string name=\"prefKeyMaxFreq\">122880</string>"
		+"\n</map>"
		+"\n";

	public void resetPrefsXml(Context context) {
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		if ((new File(prefsPath.substring(0, prefsPath.lastIndexOf("/")))).exists()) {
			String tmpPrefsFilePath = app.getApplicationContext().getFilesDir().getAbsolutePath().toString()+"/txt/cpuTuner.xml";
			File tmpPrefsFile = new File(tmpPrefsFilePath);			
	        if (tmpPrefsFile.exists()) { tmpPrefsFile.delete(); }
	        try {
	        	BufferedWriter outFile = new BufferedWriter(new FileWriter(tmpPrefsFile));
	        	outFile.write(prefsXml);
	        	outFile.close();
	        	fileUtils.chmod(tmpPrefsFile, 0755);
	        	if (tmpPrefsFile.exists()) { 
	        		Log.d(TAG,"TRYING: cp "+tmpPrefsFilePath+" "+prefsPath);
	        		shellCommands.executeCommandAsRoot("cp "+tmpPrefsFilePath+" "+prefsPath,null,context);
	        	}
	        } catch (IOException e) {
	        	Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
	        }
			
		} else {
			Log.e(TAG,"cpuTuner is NOT installed");
		}
		
	}
	
}
