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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.rfcx.guardian.utility.device.capture.DeviceStorage;
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
		if ((new File(filePath)).exists()) {
			try {
				MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
				FileInputStream fileInputStream = new FileInputStream(filePath);
				byte[] dataBytes = new byte[1024];
				int nread = 0;
				while ((nread = fileInputStream.read(dataBytes)) != -1) {
					messageDigest.update(dataBytes, 0, nread);
				}
				;
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
		}
		return "";
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

				if (owner_rwx.toLowerCase().contains("r")) {
					if (everybody_rwx.toLowerCase().contains("r")) { file.setReadable(true, false); } else { file.setReadable(true); }
				}
				if (owner_rwx.toLowerCase().contains("w")) {
					if (everybody_rwx.toLowerCase().contains("w")) { file.setWritable(true, false); } else { file.setWritable(true); }
				}
				if (owner_rwx.toLowerCase().contains("x")) {
					if (everybody_rwx.toLowerCase().contains("x")) { file.setExecutable(true, false); } else { file.setExecutable(true); }
				}

				return true;
			}

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

		if (fileObj == null) { return true; }
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
		if (filePath == null) { return true; }
		return delete(new File(filePath));
	}

//	public static void delete(List<File> fileObjs) { delete(fileObjs, false); }

	public static List<String> deleteAndReturnFilePaths(List<File> fileObjList) {
		List<String> filePaths = new ArrayList<String>();
		for (File fileObj : fileObjList) {
			delete(fileObj);
			if (!fileObj.exists()) {
				filePaths.add(fileObj.getAbsolutePath());
			}
		}
		return filePaths;
	}

	public static boolean exists(String filePath) {
		return (filePath != null) && (new File(filePath)).exists();
	}

	public static boolean exists(File fileObj) {
		return (fileObj != null) && fileObj.exists();
	}

	public static List<File> getDirectoryContents(String directoryFilePath, boolean isRecursive) {
		List<File> files = new ArrayList<File>();
		File dirObj = new File(directoryFilePath);
		if (dirObj.isDirectory()) {
			for (File file : Objects.requireNonNull(dirObj.listFiles())) {
				try {
					if (!file.isDirectory()) {
						files.add(file);
					} else if (isRecursive) {
						List<File> innerFiles = getDirectoryContents(file.getAbsolutePath(), isRecursive);
						files.addAll(innerFiles);
					}
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
		return files;
	}

	public static List<File> getEmptyDirectories(String directoryFilePath) {
		List<File> dirs = new ArrayList<File>();
		File dirObj = new File(directoryFilePath);
		if (dirObj.isDirectory()) {
			File[] innerFiles = dirObj.listFiles();
			assert innerFiles != null;
			if (innerFiles.length == 0) {
				dirs.add(dirObj);
			} else {
				for (File innerFile : innerFiles) {
					List<File> innerDirs = getEmptyDirectories(innerFile.getAbsolutePath());
					dirs.addAll(innerDirs);
				}
			}
		}
		return dirs;
	}


	public static void deleteDirectoryContents(String directoryFilePath) {
		File directory = new File(directoryFilePath);
		for (File file : Objects.requireNonNull(directory.listFiles())) {
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
		for (File file : Objects.requireNonNull(directory.listFiles())) {
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
		for (File file : Objects.requireNonNull(directory.listFiles())) {
			try { 
				int fileAgeInMinutes = (int) Math.round((((double) millisecondsSinceLastModified(file)) / 1000 ) / 60);
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
		if (!isExternal || DeviceStorage.isExternalStorageWritable()) {
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
            GZIPOutputStream gZipOutputStream = new GZIPOutputStream(fileOutputStream) { { this.def.setLevel(Deflater.BEST_COMPRESSION); } };
            
            byte[] buffer = new byte[1024];
            int len;
            while ( (len = fileInputStream.read(buffer) ) != -1 ) {
                gZipOutputStream.write(buffer, 0, len);
            }
            
            gZipOutputStream.close();
            fileOutputStream.close();
            fileInputStream.close();
            
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
            
		} catch (IOException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	public static void gUnZipFile(String inputFilePath, String outputFilePath) {
		gUnZipFile( (new File(inputFilePath)), (new File(outputFilePath)));
	}

	public static String bytesAsReadableString(int bytes) {
		return bytesAsReadableString(Long.parseLong(""+bytes));
	}

	public static String bytesAsReadableString(long bytes) {
		StringBuilder sizeStr = new StringBuilder();

		int MB = (int) Math.floor( ((double) bytes) / 1048576 );
		int kB = (int) Math.floor( (((double) bytes) - (MB * 1048576)) / 1024 );
		int B = (int) Math.floor( (bytes - (MB * 1048576) - (kB * 1024)));

		if (MB > 0) {
			sizeStr.append(MB);
			int fracVal = (int) Math.floor(100 * ((double) kB) / 1024);
			sizeStr.append(".").append( (fracVal < 10) ? "0" : "").append(fracVal);
			sizeStr.append(" MB");

		} else if (kB > 1) {
			sizeStr.append(kB);
			int fracVal = (int) Math.floor(10 * ((double) B) / 1024);
			if (kB < 10) { sizeStr.append(".").append(fracVal); }
			sizeStr.append(" kB");

		} else {
			if (kB > 0) { B += (kB * 1024); }
			sizeStr.append(B).append(" bytes");

		}

		return sizeStr.toString();
	}

	
}
