package org.rfcx.guardian.utility.misc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.util.Base64;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.vendor.Base85;

public class StringUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "StringUtils");
	
	private static final char[] lowerCaseAlphanumericRef = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private static final char[] upperLowerCaseAlphanumericRef = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

	public static String randomAlphanumericString(int stringLength, boolean allowUpperCaseCharacters) {
		char[] charRef = lowerCaseAlphanumericRef; if (allowUpperCaseCharacters) { charRef = upperLowerCaseAlphanumericRef; }
		StringBuilder stringBuilder = new StringBuilder(stringLength);
		Random random = new Random();
		for (int i = 0; i < stringLength; i++) {
		    stringBuilder.append(charRef[random.nextInt(charRef.length)]);
		}
		return stringBuilder.toString();
	}
	
	public static String getSha1HashOfString(String inputString) {
		MessageDigest messageDigest = null;
		try {
			messageDigest = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			RfcxLog.logExc(logTag, e);
		}
		return byteArrayToHexString(messageDigest.digest(inputString.getBytes()));
	}


//	public static String stringToBrotliBase64(String inputString) {
//		return byteArrayToBase64String( stringToBrotliByteArray( inputString ) );
//	}
//
//	public static byte[] stringToBrotliByteArray(String inputString) {
//		return stringToBrotliByteArray(inputString, "UTF-8");
//	}
//
//	public static byte[] stringToBrotliByteArray(String inputString, String charsetName) {
//
//		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//		BrotliOutputStream brotliOutputStream = null;
//		try {
//			brotliOutputStream = new BrotliOutputStream(byteArrayOutputStream);
//			brotliOutputStream.write(inputString.getBytes(charsetName));
//		} catch (IOException e) {
//			RfcxLog.logExc(logTag, e);
//		} finally {
//			if (brotliOutputStream != null) {
//				try {
//					brotliOutputStream.close();
//				} catch (IOException e) {
//					RfcxLog.logExc(logTag, e);
//				}
//			}
//		}
//
//		return byteArrayOutputStream.toByteArray();
//	}

	public static String stringToGZipAscii85(String inputString) {
		return byteArrayToAscii85String( stringToGZipByteArray( inputString ) );
	}

	public static String byteArrayToAscii85String(byte[] byteArray) {
		return Base85.getRfc1924Encoder().encodeToString( byteArray );
	}

	public static String gZipAscii85ToUnGZipString(String ascii85String) {
		return gZipByteArrayToUnGZipString( ascii85StringToByteArray( ascii85String ) );
	}

	public static byte[] ascii85StringToByteArray(String ascii85String) {
		return Base85.getRfc1924Decoder().decodeToBytes(ascii85String);
	}


	public static String stringToGZipBase64(String inputString) {
		return byteArrayToBase64String( stringToGZipByteArray( inputString ) );
	}

	public static String gZipBase64ToUnGZipString(String base64String) {
		return gZipByteArrayToUnGZipString( base64StringToByteArray( base64String ) );
	}

	public static byte[] stringToGZipByteArray(String inputString) {
		return stringToGZipByteArray(inputString, "UTF-8");
	}

	public static byte[] stringToGZipByteArray(String inputString, String charsetName) {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream) { { this.def.setLevel(Deflater.BEST_COMPRESSION); } };
			gZIPOutputStream.write(inputString.getBytes(charsetName));
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			if (gZIPOutputStream != null) {
				try {
					gZIPOutputStream.close();
				} catch (IOException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}

		return byteArrayOutputStream.toByteArray();
	}
	
	public static String gZipByteArrayToUnGZipString(byte[] inputByteArray) {
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try {
			GZIPInputStream gZIPInputStream = new GZIPInputStream(new ByteArrayInputStream(inputByteArray));
			int res = 0;
			byte buf[] = new byte[1024];
			while (res >= 0) {
				res = gZIPInputStream.read(buf, 0, buf.length);
				if (res > 0) {
					byteArrayOutputStream.write(buf, 0, res);
				}
			}
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}

		return new String(byteArrayOutputStream.toByteArray());
	}
	
	public static String byteArrayToHexString(byte[] byteArray) {
		StringBuilder hexString = new StringBuilder();
		for (int i = 0; i < byteArray.length; i++) {
			hexString.append( Integer.toString( (byteArray[i] & 0xff) + 0x100, 16 ).substring(1) );
		}
		return hexString.toString();
	}

	public static String byteArrayToBase64String(byte[] byteArray) {
		return Base64.encodeToString(byteArray, Base64.NO_WRAP);
	}

	public static byte[] base64StringToByteArray(String base64String) {
		return Base64.decode( base64String, Base64.NO_WRAP );
	}
	
	public static boolean saveStringToFile(String stringContents, String filePath) {
		File fileObj = new File(filePath);
		fileObj.mkdirs();
    		if (fileObj.exists()) { fileObj.delete(); }
    		try {
    			BufferedWriter outFile = new BufferedWriter(new FileWriter(filePath));
    			outFile.write(stringContents);
    			outFile.close();
    			FileUtils.chmod(filePath, "rwx", "rx");
    		} catch (IOException e) {
    			RfcxLog.logExc(logTag, e);
    		}
    		return fileObj.exists();
	}

	public static String leftPadStringWithChar(String inputStr, int targetLength, String padWithChar) {
		StringBuilder paddedStr = new StringBuilder();
		if (inputStr.length() < targetLength) {
			for (int i = 0; i < targetLength-inputStr.length(); i++) {
				paddedStr.append(padWithChar);
			}
			paddedStr.append(inputStr);
			return paddedStr.toString();
		}
		return inputStr;
	}

	public static String capitalizeFirstChar(String str) {
		return str.substring(0,1).toUpperCase()+str.substring(1);
	}

	public static String smsEncodeAscii85(String unEncodedStr) {
		return unEncodedStr
			.replaceAll(Pattern.quote("-"), "ä")
			.replaceAll(Pattern.quote("="), "ö")
			.replaceAll(Pattern.quote(">"), "Ä")
			.replaceAll(Pattern.quote("?"), "¿")
			.replaceAll(Pattern.quote("^"), "Ö")
			.replaceAll(Pattern.quote("`"), "ø")
			.replaceAll(Pattern.quote("{"), "Ø")
			.replaceAll(Pattern.quote("|"), "è")
			.replaceAll(Pattern.quote("}"), "é")
			.replaceAll(Pattern.quote("~"), "ù");
	}

	public static String smsDecodeAscii85(String unDecodedStr) {
		return unDecodedStr
			.replaceAll(Pattern.quote("ä"), "-")
			.replaceAll(Pattern.quote("ö"), "=")
			.replaceAll(Pattern.quote("Ä"), ">")
			.replaceAll(Pattern.quote("¿"), "?")
			.replaceAll(Pattern.quote("Ö"), "^")
			.replaceAll(Pattern.quote("ø"), "`")
			.replaceAll(Pattern.quote("Ø"), "{")
			.replaceAll(Pattern.quote("è"), "|")
			.replaceAll(Pattern.quote("é"), "}")
			.replaceAll(Pattern.quote("ù"), "~");
	}

//	public static String smsDecode(String origStr) {
//
//
//	}

//	static class GZIPOutputStream_BestCompression extends GZIPOutputStream {
//		public GZIPOutputStream_BestCompression(OutputStream out) throws IOException { super(out); }
//		public void setToBest() { def.setLevel(Deflater.BEST_COMPRESSION); }
//	}



	public static String readStringFromFile(String filePath) {
		try {

			File fileObj = new File(filePath);

			if (fileObj.exists()) {
				FileInputStream input = new FileInputStream(fileObj);
				StringBuilder fileContent = new StringBuilder();
				byte[] buffer = new byte[256];
				while (input.read(buffer) != -1) {
					fileContent.append(new String(buffer));
				}
				String txtFileContents = fileContent.toString().trim();
				input.close();
				return txtFileContents;
			}
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}

	public static String joinArrayString(String[] arr, String delimiter) {
		if (arr == null || 0 == arr.length) return "";

		StringBuilder sb = new StringBuilder();
		sb.append(arr[0]);

		for (int i = 1; i < arr.length; i++) sb.append(delimiter).append(arr[i]);

		return sb.toString();
	}

}
