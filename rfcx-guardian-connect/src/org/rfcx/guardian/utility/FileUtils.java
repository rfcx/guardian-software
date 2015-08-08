package org.rfcx.guardian.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.rfcx.guardian.connect.R;

import android.text.TextUtils;
import android.util.Log;

public class FileUtils {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+FileUtils.class.getSimpleName();

	public String sha1Hash(String filePath) {
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
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (FileNotFoundException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		}
		return null;
	}
	
	public int chmod(File file, int mode) {
		try {
			Class fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
			return (Integer) setPermissions.invoke(null, file.getAbsolutePath(), mode, -1, -1);
		} catch (ClassNotFoundException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (SecurityException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (NoSuchMethodException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (IllegalArgumentException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (IllegalAccessException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		} catch (InvocationTargetException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		}
		return 0;
	}
	
}
