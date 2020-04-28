package org.rfcx.guardian.utility.device.control;

import java.util.HashMap;
import java.util.Map;

import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DeviceKeyEntry {

	public DeviceKeyEntry(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "DeviceKeyEntry");
		defineKeyCodeMap();
	}

	private String logTag;
	
	private Map<String, int[]> keyCodeMap = new HashMap<String, int[]>();
	
	private void defineKeyCodeMap() {
		
		Map<String, int[]> keyCodeMap = new HashMap<String, int[]>();
		
		keyCodeMap.put("0", new int[] { 7 } );
		keyCodeMap.put("1", new int[] { 8 } );
		keyCodeMap.put("2", new int[] { 9 } );
		keyCodeMap.put("3", new int[] { 10 } );
		keyCodeMap.put("4", new int[] { 11 } );
		keyCodeMap.put("5", new int[] { 12 } );
		keyCodeMap.put("6", new int[] { 13 } );
		keyCodeMap.put("7", new int[] { 14 } );
		keyCodeMap.put("8", new int[] { 15 } );
		keyCodeMap.put("9", new int[] { 16 } );
		
		keyCodeMap.put("*", new int[] { 17 } ); // star/asterisk
		keyCodeMap.put("#", new int[] { 18 } ); // pound
		keyCodeMap.put("|", new int[] { 23 } ); // enter

		keyCodeMap.put("^", new int[] { 19 } ); // up
		keyCodeMap.put("v", new int[] { 20 } ); // down
		keyCodeMap.put("<", new int[] { 21 } ); // left
		keyCodeMap.put(">", new int[] { 22 } ); // right
		
		this.keyCodeMap = keyCodeMap;
	}
	
	
	private String getKeyCodeSequenceShellCommand(String keyCodeSequence) {
		
		char[] keyCodeChars = keyCodeSequence.toCharArray();
		String[] keyEvents = new String[keyCodeChars.length];
		
		for (int i = 0; i < keyCodeChars.length; i++) {
			keyEvents[i] = "input keyevent " + this.keyCodeMap.get(String.valueOf(keyCodeChars[i]))[0];
		}
		return TextUtils.join(" && ", keyEvents)+";\n";
	}
	
	public void testExecuteString(String keyCodeSequence) {
		
		Log.d(logTag, getKeyCodeSequenceShellCommand(keyCodeSequence) );
	}
	

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
