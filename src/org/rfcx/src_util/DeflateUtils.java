package org.rfcx.src_util;

import java.io.UnsupportedEncodingException;
import java.util.zip.Deflater;

import android.util.Log;

public class DeflateUtils {
	
	private static final String ENCODING = "ISO-8859-1";
	private static final int COMPRESSION_LEVEL = 9;
	private static final boolean EXCLUDE_WRAPPING = true;
	
	public static String deflate(String inputString) {
		String outputString = "";
		 try {
		     byte[] inputBytes = inputString.getBytes(ENCODING);
		     byte[] outputBytes = new byte[inputString.length()];
		     Deflater deflater = new Deflater( COMPRESSION_LEVEL, EXCLUDE_WRAPPING );
		     deflater.setInput(inputBytes);
		     deflater.finish();
		     outputString = new String(outputBytes, 0, deflater.deflate(outputBytes), ENCODING);		     
		 } catch(UnsupportedEncodingException e) {
			 Log.e(DeflateUtils.class.getSimpleName(), e.getMessage());
		 }
		return outputString;
	}
	
}
