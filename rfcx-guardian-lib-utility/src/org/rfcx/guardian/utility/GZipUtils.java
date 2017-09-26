package org.rfcx.guardian.utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.util.Base64;

public class GZipUtils {

	private static final String logTag = RfcxLog.generateLogTag("Utils", GZipUtils.class);
	
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
	
	public static void gZipFile(File inputFile, File outputFile) {
		
		(new File(outputFile.getAbsolutePath().substring(0,outputFile.getAbsolutePath().lastIndexOf("/")))).mkdirs();
		
		try {
			
			FileInputStream fileInputStream = new FileInputStream(inputFile.getAbsolutePath());
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath());
            GZIPOutputStream gZipOutputStream = new GZIPOutputStream(fileOutputStream);
            
            byte[] buffer = new byte[1024];
            int len;
            while ( (len = fileInputStream.read(buffer) ) != -1 ) {
                gZipOutputStream.write(buffer, 0, len);
            }
            
            gZipOutputStream.close();
            fileOutputStream.close();
            fileInputStream.close();
            
		} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public static void gZipFile(String inputFilePath, String outputFilePath) {
		gZipFile( (new File(inputFilePath)), (new File(outputFilePath)));
	}
	
	public static void unGZipFile(File inputFile, File outputFile) {
		
		(new File(outputFile.getAbsolutePath().substring(0,outputFile.getAbsolutePath().lastIndexOf("/")))).mkdirs();
		
//		try {
//			FileInputStream fileInputStream = new FileInputStream(inputFile.getAbsolutePath());
//            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath());
//            GZIPOutputStream gZipOutputStream = new GZIPOutputStream(fileOutputStream);
//            byte[] buffer = new byte[1024];
//            int len;
//            while((len=fileInputStream.read(buffer)) != -1){
//                gZipOutputStream.write(buffer, 0, len);
//            }
//            gZipOutputStream.close();
//            fileOutputStream.close();
//            fileInputStream.close();
//		} catch (FileNotFoundException e) {
//			RfcxLog.logExc(TAG, e);
//		} catch (IOException e) {
//			RfcxLog.logExc(TAG, e);
//		}
	}
	
	public static void unGZipFile(String inputFilePath, String outputFilePath) {
		unGZipFile( (new File(inputFilePath)), (new File(outputFilePath)));
	}

}
