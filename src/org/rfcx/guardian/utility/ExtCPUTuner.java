package org.rfcx.guardian.utility;

import org.rfcx.guardian.RfcxGuardian;

import android.content.Context;

public class ExtCPUTuner {

	private static final String TAG = "RfcxGuardian-"+ExtCPUTuner.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	private ShellCommands shellCommands = new ShellCommands();
	
	private static final String prefsXml = 
			"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>"
			+"\n<map>"
			+"\n<string name=\"prefKeyConfiguration\">Sophisticated</string>"
			+"\n<string name=\"prefKeyCalcPowerUsageType\">3</string>"
			+"\n<string name=\"prefKeyUnitSystem\">1</string>"
			+"\n<string name=\"prefKeyUserLevel\">3</string>"
			+"\n<int name=\"prefKeyMinFreqDefault\" value=\"30720\" />"
			+"\n<string name=\"prefKeyMinFreq\">30720</string>"
			+"\n<int name=\"prefKeyMaxFreqDefault\" value=\"122880\" />"
			+"\n<string name=\"prefKeyMaxFreq\">122880</string>"
			+"\n<boolean name=\"prefKeyEnableProfiles\" value=\"true\" />"
			+"\n<long name=\"prefKeyLastBoot\" value=\"1363225391032\" />"
			+"\n<boolean name=\"prefKeySaveConfigOnSwitch\" value=\"false\" />"
			+"\n<string name=\"prefKeyStatusbarAddToChoice\">0</string>"
			+"\n<boolean name=\"prefKeyUserLevelSet\" value=\"true\" />"
			+"\n</map>"
			+"\n";

	public void resetPrefsXml(Context context) {
	
		
		
		shellCommands.executeCommandAsRoot(commandContents, outputSearchString, context);
		
		
	}
	
}
