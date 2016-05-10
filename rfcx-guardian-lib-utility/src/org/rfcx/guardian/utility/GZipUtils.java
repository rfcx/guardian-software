package org.rfcx.guardian.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.rfcx.guardian.utility.rfcx.RfcxConstants;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class GZipUtils {

	private static final String TAG = "Rfcx-Utils-"+GZipUtils.class.getSimpleName();
	
	public static String gZipStringToBase64(String inputString) {
		return Base64.encodeToString(gZipString(inputString),Base64.DEFAULT);
	}
	
	public static byte[] gZipString(String inputString) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gZIPOutputStream = null;
		try {
			gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			gZIPOutputStream.write(inputString.getBytes("UTF-8"));
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		} finally { if (gZIPOutputStream != null) {
			try { gZIPOutputStream.close();
			} catch (IOException e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			};
		} }
		return byteArrayOutputStream.toByteArray();
	}
	
	public static void gZipFile(File inputFile, File outputFile) {
		
		(new File(outputFile.getAbsolutePath().substring(0,outputFile.getAbsolutePath().lastIndexOf("/")))).mkdirs();
		
		try {
			FileInputStream fileInputStream = new FileInputStream(inputFile.getAbsolutePath());
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath());
            GZIPOutputStream gZipOutputStream = new GZIPOutputStream(fileOutputStream);
            byte[] buffer = new byte[1024];
            int len;
            while((len=fileInputStream.read(buffer)) != -1){
                gZipOutputStream.write(buffer, 0, len);
            }
            gZipOutputStream.close();
            fileOutputStream.close();
            fileInputStream.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		} catch (IOException e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
		}
	}
	
	public static void gZipFile(String inputFilePath, String outputFilePath) {
		
		gZipFile( (new File(inputFilePath)), (new File(outputFilePath)));
		
	}

}
