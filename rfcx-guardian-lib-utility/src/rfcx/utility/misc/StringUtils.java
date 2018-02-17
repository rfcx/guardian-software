package rfcx.utility.misc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import android.util.Base64;
import rfcx.utility.rfcx.RfcxLog;

public class StringUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", StringUtils.class);
	
	private static final char[] lowerCaseAlphanumericRef = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private static final char[] upperLowerCaseAlphanumericRef = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	
	public static String randomAlphanumericString(int stringLength, boolean allowUpperCaseCharacters) {
		char[] charRef = lowerCaseAlphanumericRef; if (allowUpperCaseCharacters) { charRef = upperLowerCaseAlphanumericRef; }
		StringBuilder stringBuilder = new StringBuilder(stringLength);
		Random random = new Random();
		for (int i = 0; i < stringLength; i++) {
		    stringBuilder.append(charRef[random.nextInt(charRef.length)]);
		}
		return stringBuilder.toString();
	}

	public static String gZipStringToBase64(String inputString) {
		return Base64.encodeToString(gZipStringToByteArray(inputString),Base64.DEFAULT);
	}
	
	public static byte[] gZipStringToByteArray(String inputString) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gZIPOutputStream.write(inputString.getBytes("UTF-8"));
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		} finally { 
			if (gZIPOutputStream != null) {
				try { 
					gZIPOutputStream.close();
				} catch (IOException e) {
					RfcxLog.logExc(logTag, e);
				};
			}
		}
		return byteArrayOutputStream.toByteArray();
	}
	
}
