package org.rfcx.guardian.utility;

import java.util.Random;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class StringMiniUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", StringMiniUtils.class);
	
	public static String randomString(int stringLength) {
		char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		StringBuilder stringBuilder = new StringBuilder(stringLength);
		Random random = new Random();
		for (int i = 0; i < stringLength; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    stringBuilder.append(c);
		}
		return stringBuilder.toString();
	}

}
