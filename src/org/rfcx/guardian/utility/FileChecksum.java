package org.rfcx.guardian.utility;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.text.TextUtils;
import android.util.Log;

public class FileChecksum {

	private static final String TAG = FileChecksum.class.getSimpleName();
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";

	public String getSha1FileChecksum(String filePath) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
		    FileInputStream fileInputStream = new FileInputStream(filePath);
			byte[] dataBytes = new byte[1024];
		    int nread = 0;
		    while ((nread = fileInputStream.read(dataBytes)) != -1) {
		    	messageDigest.update(dataBytes, 0, nread);
		    };
		    fileInputStream.close();
		    byte[] mdbytes = messageDigest.digest();
		    StringBuffer stringBuilder = new StringBuffer("");
		    for (int i = 0; i < mdbytes.length; i++) {
		    	stringBuilder.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		    }
			return stringBuilder.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (FileNotFoundException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() + TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC);
		}
		return null;
	}
	
}
