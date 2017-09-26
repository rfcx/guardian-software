package org.rfcx.guardian.utility.device.control;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.utility.ShellCommands;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.Context;
import android.text.TextUtils;

public class DeviceKeyEntry {

	public DeviceKeyEntry(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, DeviceKeyEntry.class);
	}

	private String logTag = RfcxLog.generateLogTag("Utils", DeviceKeyEntry.class);
	
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
		
		ShellCommands.executeCommand(
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
	
	
	
//
//
//Down
//adb shell input keyevent 20
//
//Up
//adb shell input keyevent 19
//
//Right
//adb shell input keyevent 22
//
//Left
//adb shell input keyevent 21
//
//Enter
//adb shell input keyevent 23
//
//0 -->  "KEYCODE_UNKNOWN" 
//1 -->  "KEYCODE_MENU" 
//2 -->  "KEYCODE_SOFT_RIGHT" 
//3 -->  "KEYCODE_HOME" 
//4 -->  "KEYCODE_BACK" 
//5 -->  "KEYCODE_CALL" 
//6 -->  "KEYCODE_ENDCALL" 
//7 -->  "KEYCODE_0" 
//8 -->  "KEYCODE_1" 
//9 -->  "KEYCODE_2" 
//10 -->  "KEYCODE_3" 
//11 -->  "KEYCODE_4" 
//12 -->  "KEYCODE_5" 
//13 -->  "KEYCODE_6" 
//14 -->  "KEYCODE_7" 
//15 -->  "KEYCODE_8" 
//16 -->  "KEYCODE_9" 
//17 -->  "KEYCODE_STAR" 
//18 -->  "KEYCODE_POUND" 
//19 -->  "KEYCODE_DPAD_UP" 
//20 -->  "KEYCODE_DPAD_DOWN" 
//21 -->  "KEYCODE_DPAD_LEFT" 
//22 -->  "KEYCODE_DPAD_RIGHT" 
//23 -->  "KEYCODE_DPAD_CENTER" 
//24 -->  "KEYCODE_VOLUME_UP" 
//25 -->  "KEYCODE_VOLUME_DOWN" 
//26 -->  "KEYCODE_POWER" 
//27 -->  "KEYCODE_CAMERA" 
//28 -->  "KEYCODE_CLEAR" 
//29 -->  "KEYCODE_A" 
//30 -->  "KEYCODE_B" 
//31 -->  "KEYCODE_C" 
//32 -->  "KEYCODE_D" 
//33 -->  "KEYCODE_E" 
//34 -->  "KEYCODE_F" 
//35 -->  "KEYCODE_G" 
//36 -->  "KEYCODE_H" 
//37 -->  "KEYCODE_I" 
//38 -->  "KEYCODE_J" 
//39 -->  "KEYCODE_K" 
//40 -->  "KEYCODE_L" 
//41 -->  "KEYCODE_M" 
//42 -->  "KEYCODE_N" 
//43 -->  "KEYCODE_O" 
//44 -->  "KEYCODE_P" 
//45 -->  "KEYCODE_Q" 
//46 -->  "KEYCODE_R" 
//47 -->  "KEYCODE_S" 
//48 -->  "KEYCODE_T" 
//49 -->  "KEYCODE_U" 
//50 -->  "KEYCODE_V" 
//51 -->  "KEYCODE_W" 
//52 -->  "KEYCODE_X" 
//53 -->  "KEYCODE_Y" 
//54 -->  "KEYCODE_Z" 
//55 -->  "KEYCODE_COMMA" 
//56 -->  "KEYCODE_PERIOD" 
//57 -->  "KEYCODE_ALT_LEFT" 
//58 -->  "KEYCODE_ALT_RIGHT" 
//59 -->  "KEYCODE_SHIFT_LEFT" 
//60 -->  "KEYCODE_SHIFT_RIGHT" 
//61 -->  "KEYCODE_TAB" 
//62 -->  "KEYCODE_SPACE" 
//63 -->  "KEYCODE_SYM" 
//64 -->  "KEYCODE_EXPLORER" 
//65 -->  "KEYCODE_ENVELOPE" 
//66 -->  "KEYCODE_ENTER" 
//67 -->  "KEYCODE_DEL" 
//68 -->  "KEYCODE_GRAVE" 
//69 -->  "KEYCODE_MINUS" 
//70 -->  "KEYCODE_EQUALS" 
//71 -->  "KEYCODE_LEFT_BRACKET" 
//72 -->  "KEYCODE_RIGHT_BRACKET" 
//73 -->  "KEYCODE_BACKSLASH" 
//74 -->  "KEYCODE_SEMICOLON" 
//75 -->  "KEYCODE_APOSTROPHE" 
//76 -->  "KEYCODE_SLASH" 
//77 -->  "KEYCODE_AT" 
//78 -->  "KEYCODE_NUM" 
//79 -->  "KEYCODE_HEADSETHOOK" 
//80 -->  "KEYCODE_FOCUS" 
//81 -->  "KEYCODE_PLUS" 
//82 -->  "KEYCODE_MENU" 
//83 -->  "KEYCODE_NOTIFICATION" 
//84 -->  "KEYCODE_SEARCH" 
//85 -->  "TAG_LAST_KEYCODE"
//	
//	
	
	
}
