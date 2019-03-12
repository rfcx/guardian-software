package org.rfcx.guardian.utility.misc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class FileUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", FileUtils.class);
	
	public static String sha1Hash(String filePath) {
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
		    StringBuffer stringBuffer = new StringBuffer("");
		    for (int i = 0; i < mdbytes.length; i++) {
		    	stringBuffer.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		    }
			return stringBuffer.toString();
		} catch (NoSuchAlgorithmException e) {
			RfcxLog.logExc(logTag, e);
		} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	public static String fileAsBase64String(String filePath) {
		
		String base64String = null;
		
		try {
			FileInputStream fileInputStream = new FileInputStream(filePath);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			Base64OutputStream base64OutputStream = new Base64OutputStream(outputStream,Base64.NO_WRAP);
	
			byte[] buffer = new byte[3 * 512]; int len = 0;
			while ((len = fileInputStream.read(buffer)) >= 0) {
			    base64OutputStream.write(buffer, 0, len);
			}
	
			base64OutputStream.flush();
			base64OutputStream.close();
	
			base64String = new String(outputStream.toByteArray(), "UTF-8");
			
			outputStream.close();
			fileInputStream.close();
			
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
		
		return base64String;
	}
	
	public static byte[] fileAsByteArray(String filePath) {
		
		byte[] fileBytes = null;
		
		try {
			File fileObj = new File(filePath);
			int fileLength = (int) fileObj.length();
			fileBytes = new byte[fileLength];
		
	    		InputStream fileInputStream = new FileInputStream(fileObj);
    			int offset = 0; int numRead = 0;
    			while (		(offset < fileBytes.length)
    					&& 	((numRead = fileInputStream.read(fileBytes, offset, fileBytes.length-offset)) >= 0)
    				) { offset += numRead; }
    			if (offset < fileBytes.length) { fileInputStream.close(); throw new IOException("Could not completely read file "+fileObj.getName()); }
    			fileInputStream.close();
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
    		return fileBytes;
	}
	
	public static int chmod(File file, int mode) {
		try {
			Class fileUtils = Class.forName("android.os.FileUtils");
			Method setPermissions = fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
			return (Integer) setPermissions.invoke(null, file.getAbsolutePath(), mode, -1, -1);
		} catch (ClassNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (SecurityException e) {
			RfcxLog.logExc(logTag, e);
		} catch (NoSuchMethodException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IllegalArgumentException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IllegalAccessException e) {
			RfcxLog.logExc(logTag, e);
		} catch (InvocationTargetException e) {
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return 0;
	}
	
	public static int chmod(String filePath, int mode) {
		return chmod(new File(filePath), mode);
	}
	
	public static Date lastModifiedAt(File fileObj) {
		Date modifiedAt = null;
		if (fileObj.exists()) {
			modifiedAt = new Date(fileObj.lastModified());
		}
		return modifiedAt;
	}
	
	public static Date lastModifiedAt(String filePath) {
		return lastModifiedAt(new File(filePath));
	}
	
	public static long millisecondsSinceLastModified(File fileObj) {
		return (new Date()).getTime() - lastModifiedAt(fileObj).getTime();
	}
	
	public static long millisecondsSinceLastModified(String filePath) {
		return millisecondsSinceLastModified(new File(filePath));
	}
	
	public static void copy(File srcFile, File dstFile) throws IOException {
		
		(new File(dstFile.getAbsolutePath().substring(0,dstFile.getAbsolutePath().lastIndexOf("/")))).mkdirs();
		
	    InputStream inputStream = new FileInputStream(srcFile);
	    OutputStream outputStream = new FileOutputStream(dstFile);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = inputStream.read(buf)) > 0) {
	        outputStream.write(buf, 0, len);
	    }
	    inputStream.close();
	    outputStream.close();
	}
	
	public static void copy(String srcFilePath, String dstFilePath) throws IOException {
		copy(new File(srcFilePath), new File(dstFilePath));
	}

	
	public static boolean delete(File fileObj) {
		
		if (!fileObj.exists()) { return true; }
		if (!fileObj.isDirectory()) { return fileObj.delete(); }

		for (File innerFileObj : fileObj.listFiles()) {
			try { 
				if (innerFileObj.isDirectory()) {
					delete(innerFileObj);
				} else {
					innerFileObj.delete();
				}
			} catch (Exception e) { 
				RfcxLog.logExc(logTag, e);
			}
		}
		return fileObj.delete();
	}
	
	public static boolean delete(String filePath) {
		return delete(new File(filePath));
	}
	
	public static void delete(List<String> filePaths) {
		for (String filePath : filePaths) {
			delete(filePath);
		}
	}
	

	public static void deleteDirectoryContents(String directoryFilePath) {
		File directory = new File(directoryFilePath);
		for (File file : directory.listFiles()) {
			try { 
				file.delete();
				Log.d(logTag, "Deleted "+file.getName()+" from "+directory.getName()+" directory.");
			} catch (Exception e) { 
				RfcxLog.logExc(logTag, e);
			}
		}
	}
	
	public static void deleteDirectoryContents(String directoryFilePath, List<String> excludeFilePaths) {
		File directory = new File(directoryFilePath);
		for (File file : directory.listFiles()) {
			try { 
				if (!excludeFilePaths.contains(file.getAbsolutePath())) {
					file.delete();
					Log.d(logTag, "Deleted "+file.getName()+" from "+directory.getName()+" directory.");
				}
			} catch (Exception e) { 
				RfcxLog.logExc(logTag, e);
			}
		}
	}
	
	public static void deleteDirectoryContentsIfOlderThanExpirationAge(String directoryFilePath, int expirationAgeInMinutes) {
		File directory = new File(directoryFilePath);
		for (File file : directory.listFiles()) {
			try { 
				int fileAgeInMinutes = Math.round((millisecondsSinceLastModified(file) / 1000 ) / 60);
				if (fileAgeInMinutes > expirationAgeInMinutes) {
					file.delete();
					Log.d(logTag, "Deleted "+file.getName()+" from "+directory.getName()+" directory ("+fileAgeInMinutes+" minutes old).");
				}
			} catch (Exception e) { 
				RfcxLog.logExc(logTag, e);
			}
		}
	}
	
	
	public static boolean createTarArchiveFromFileList(List<String> inputFilePaths, String outputTarFilePath) {

		if ((new File(outputTarFilePath)).exists()) {
			Log.d(logTag, "'"+outputTarFilePath+"' already exists. This file will not be overwritten.");
			return false;
		} else {
			try {
				FileOutputStream tarFileOutputStream = new FileOutputStream(outputTarFilePath);
				TarOutputStream tarOutputStream = new TarOutputStream(new BufferedOutputStream(tarFileOutputStream));
				for (String filePath : inputFilePaths) {
					File fileObj = new File(filePath);
					if (fileObj.exists()) {
						tarOutputStream.putNextEntry(new TarEntry(fileObj, fileObj.getName()));
						BufferedInputStream originInputStream = new BufferedInputStream(new FileInputStream(fileObj));
						int count;
						byte data[] = new byte[2048];
						while((count = originInputStream.read(data)) != -1) {
							tarOutputStream.write(data, 0, count);
						}
						tarOutputStream.flush();
						originInputStream.close();
					} else {
						Log.d(logTag, filePath+" does not exist.");
					}
				}
				tarOutputStream.close();
			} catch (FileNotFoundException e) {
				RfcxLog.logExc(logTag, e);
			} catch (IOException e) {
				RfcxLog.logExc(logTag, e);
			}
			return (new File(outputTarFilePath)).exists();
		}
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
	
	public static void gUnZipFile(File inputFile, File outputFile) {
		
		(new File(outputFile.getAbsolutePath().substring(0,outputFile.getAbsolutePath().lastIndexOf("/")))).mkdirs();
		
		try {
			
			FileInputStream fileInputStream = new FileInputStream(inputFile.getAbsolutePath());
			GZIPInputStream gZipInputStream = new GZIPInputStream(fileInputStream);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile.getAbsolutePath());
            
            byte[] buffer = new byte[1024];
            int len;
            while ( (len = gZipInputStream.read(buffer) ) != -1 ) {
            		fileOutputStream.write(buffer, 0, len);
            }
            
            fileOutputStream.close();
            gZipInputStream.close();
            fileInputStream.close();
            
		} catch (FileNotFoundException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public static void gUnZipFile(String inputFilePath, String outputFilePath) {
		gUnZipFile( (new File(inputFilePath)), (new File(outputFilePath)));
	}
	
	
	public static String getSystemApplicationDirPath(Context context) {
		String currentAppFilesDir = context.getFilesDir().getAbsolutePath();
		return currentAppFilesDir.substring(0, currentAppFilesDir.indexOf("org.rfcx.org.rfcx.guardian.guardian."));
	}
	
    public static boolean isExternalStorageAvailable() {
    	
    		int requiredFreeMegaBytes = 16;

        StatFs extDiskStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        double extDiskAvailSize = (double) extDiskStat.getAvailableBlocks() * (double) extDiskStat.getBlockSize();
        // One binary gigabyte equals 1,073,741,824 bytes.
        double mbAvailable = extDiskAvailSize / 1048576;

        String extDiskState = Environment.getExternalStorageState();
        boolean extDiskIsAvailable = false;
        boolean extDiskIsWriteable = false;

        if (Environment.MEDIA_MOUNTED.equals(extDiskState)) {
            // We can read and write the media
            extDiskIsAvailable = extDiskIsWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(extDiskState)) {
            // We can only read the media
            extDiskIsAvailable = true;
            extDiskIsWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            extDiskIsAvailable = extDiskIsWriteable = false;
        }

        return extDiskIsAvailable && extDiskIsWriteable && (mbAvailable >= requiredFreeMegaBytes);
        
    }
    
    public static boolean isInternalStorageAvailable() {
    	
		int requiredFreeMegaBytes = 16;
		
		return true;

//	    StatFs intDiskStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
//	    double intDiskAvailSize = (double) intDiskStat.getAvailableBlocks() * (double) intDiskStat.getBlockSize();
//	    // One binary gigabyte equals 1,073,741,824 bytes.
//	    double mbAvailable = intDiskAvailSize / 1048576;
//	
//	    String intDiskState = Environment.getExternalStorageState();
//	    boolean intDiskIsAvailable = false;
//	    boolean intDiskIsWriteable = false;
//	
//	    if (Environment.MEDIA_MOUNTED.equals(intDiskState)) {
//	        // We can read and write the media
//	        intDiskIsAvailable = intDiskIsWriteable = true;
//	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(intDiskState)) {
//	        // We can only read the media
//	        intDiskIsAvailable = true;
//	        intDiskIsWriteable = false;
//	    } else {
//	        // Something else is wrong. It may be one of many other states, but all we need
//	        // to know is we can neither read nor write
//	        intDiskIsAvailable = intDiskIsWriteable = false;
//	    }
//	
//	    return intDiskIsAvailable && intDiskIsWriteable && (mbAvailable >= requiredFreeMegaBytes);
	    
	}
	
}
