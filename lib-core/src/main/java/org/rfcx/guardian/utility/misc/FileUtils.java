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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.rfcx.guardian.utility.device.capture.DeviceDiskUsage;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarOutputStream;

import android.content.Context;
import android.util.Base64;
import android.util.Base64OutputStream;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class FileUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "FileUtils");
	
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
	
	public static boolean chmod(File file, String owner_rwx, String everybody_rwx) {
		try {

			if (file.exists()) {

//				Log.d(logTag, "chmod: " + owner_rwx + "-" + everybody_rwx + " " + file.getAbsolutePath());

				if (owner_rwx.toLowerCase().indexOf("r") >= 0) {
					if (everybody_rwx.toLowerCase().indexOf("r") >= 0) { file.setReadable(true, false); } else { file.setReadable(true); }
				}
				if (owner_rwx.toLowerCase().indexOf("w") >= 0) {
					if (everybody_rwx.toLowerCase().indexOf("w") >= 0) { file.setWritable(true, false); } else { file.setWritable(true); }
				}
				if (owner_rwx.toLowerCase().indexOf("x") >= 0) {
					if (everybody_rwx.toLowerCase().indexOf("x") >= 0) { file.setExecutable(true, false); } else { file.setExecutable(true); }
				}

				return true;
			}

		} catch (SecurityException e) {
			RfcxLog.logExc(logTag, e);
		} catch (IllegalArgumentException e) {
			RfcxLog.logExc(logTag, e);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		return false;
	}
	
	public static boolean chmod(String filePath, String owner_rwx, String everybody_rwx) {
		return chmod(new File(filePath), owner_rwx, everybody_rwx);
	}

	public static long getFileSizeInBytes(File file) {
		return file.length();
	}

	public static long getFileSizeInBytes(String filePath) {
		return (new File(filePath)).length();
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

	public static boolean exists(String filePath) {
		return (filePath != null) && (new File(filePath)).exists();
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

	public static void initializeDirectoryRecursively(String dirPath, boolean isExternal) {
		if (!isExternal || DeviceDiskUsage.isExternalStorageWritable()) {
			(new File(dirPath)).mkdirs();
			FileUtils.chmod(dirPath, "rw", "rw");
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
	

	
}
