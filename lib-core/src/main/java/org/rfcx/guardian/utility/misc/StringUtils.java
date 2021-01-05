package org.rfcx.guardian.utility.misc;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.util.Base64;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

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

	public static String stringToGZippedBase64(String inputString) {
		return byteArrayToBase64String( stringToGZippedByteArray( inputString ) );
	}

	public static String gZippedBase64ToUnGZippedString(String base64String) {
		return gZippedByteArrayToUnGZippedString( base64StringToByteArray( base64String ) );
	}

//	public static String stringToBase85(String inputString) {
//		try {
//			return Base85.getZ85Encoder().encodeToString(inputString.getBytes("UTF-8"));
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
//		return null;
//	}
//
//	public static String stringToGZippedBase85(String inputString) {
//		return Base85.getAscii85Encoder().encodeToString(stringToGZippedByteArray(inputString, "UTF-8", true));
//	}
//
//	public static String gZippedBase85ToUnGZippedString(String base85String) {
//		return gZippedByteArrayToUnGZippedString(Base85.getAscii85Decoder().decodeToBytes(base85String), true);
//	}

//	public void testBzip() {
//		String originalString = "abc";
//		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream();
//		try (BZip2CompressorOutputStream bzip2Output =
//					 new BZip2CompressorOutputStream(compressedStream)) {
//			bzip2Output.write(originalString.getBytes());
//		}
//	}

	public static byte[] stringToGZippedByteArray(String inputString) {
		return stringToGZippedByteArray(inputString, "UTF-8");
	}

	public static byte[] stringToGZippedByteArray(String inputString, String charsetName) {

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
	
	public static String gZippedByteArrayToUnGZippedString(byte[] inputByteArray) {
		
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

	public static String capitalizeFirstChar(String str) {
		return str.substring(0,1).toUpperCase()+str.substring(1);
	}

//	public static String smsDecode(String origStr) {
//		String encoded = origStr
////						.replaceAll("\\^",",")
////						.replaceAll("\\[","_").replaceAll("\\]","¿")
////						.replaceAll("\\{","é").replaceAll("\\}","è");
//						.replaceAll(Pattern.quote(","), "^")
//						.replaceAll(Pattern.quote("["), "_").replaceAll(Pattern.quote("]"), "¿")
//						.replaceAll(Pattern.quote("{"), "é").replaceAll(Pattern.quote("}"), "è");
//		return encoded;
//	}

//	public static String smsDecode(String origStr) {
//
//
//	}

//	static class GZIPOutputStream_BestCompression extends GZIPOutputStream {
//		public GZIPOutputStream_BestCompression(OutputStream out) throws IOException { super(out); }
//		public void setToBest() { def.setLevel(Deflater.BEST_COMPRESSION); }
//	}

}
