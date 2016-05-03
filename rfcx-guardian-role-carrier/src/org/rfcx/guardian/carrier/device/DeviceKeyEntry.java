package org.rfcx.guardian.carrier.device;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.carrier.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;
import org.rfcx.guardian.utility.ShellCommands;

import android.content.Context;
import android.text.TextUtils;

public class DeviceKeyEntry {

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceKeyEntry.class.getSimpleName();
	
	private List<String[]> keyCodes = null;
	
	private static String generateKeyEntryCommand(String[] keyCommands, List<String[]> keyCodeList) {
		List<String> cmdSeq = new ArrayList<String>();
		for (String cmd : keyCommands) {
			cmdSeq.add("input keyevent "+keyEntrySwap(cmd, keyCodeList));
		}
		return TextUtils.join(" && ", cmdSeq);
	}
	
	public void executeKeyEntrySequence(String concatenatedCommandSequence, String concatenatedCommandSequenceDelim, Context context) {
		
		this.keyCodes = defineKeyCodes(this.keyCodes);
		
		(new ShellCommands()).executeCommand(
				generateKeyEntryCommand(
					concatenatedCommandSequence.split(concatenatedCommandSequenceDelim),
					this.keyCodes
					),
				null, false, context);
	}

	private static List<String[]> defineKeyCodes(List<String[]> keyCodes) {
		if (keyCodes == null) {
			List<String[]> setKeyCodes = new ArrayList();
			
			setKeyCodes.add(new String[] { "digit_0", "7" });
			setKeyCodes.add(new String[] { "digit_1", "8" });
			setKeyCodes.add(new String[] { "digit_2", "9" });
			setKeyCodes.add(new String[] { "digit_3", "10" });
			setKeyCodes.add(new String[] { "digit_4", "11" });
			setKeyCodes.add(new String[] { "digit_5", "12" });
			setKeyCodes.add(new String[] { "digit_6", "13" });
			setKeyCodes.add(new String[] { "digit_7", "14" });
			setKeyCodes.add(new String[] { "digit_8", "15" });
			setKeyCodes.add(new String[] { "digit_9", "16" });
			setKeyCodes.add(new String[] { "asterisk", "17" });
			setKeyCodes.add(new String[] { "pound", "18" });
			setKeyCodes.add(new String[] { "up", "19" });
			setKeyCodes.add(new String[] { "down", "20" });
			setKeyCodes.add(new String[] { "left", "21" });
			setKeyCodes.add(new String[] { "right", "22" });
			setKeyCodes.add(new String[] { "enter", "23" });
			
			return setKeyCodes;
		}
		return keyCodes;
	}
	
	private static String keyEntrySwap(String keyEntry, List<String[]> keyCodeList) {
		for (String[] keyCodeOption : keyCodeList) {
			keyEntry.replaceAll(keyCodeOption[0], keyCodeOption[1]);
		}
		return keyEntry;
	}
}
