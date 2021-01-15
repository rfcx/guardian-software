package org.rfcx.guardian.utility.asset;

import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RfcxAssetCleanup {

	public RfcxAssetCleanup(String appRole) {
		this.logTag = RfcxLog.generateLogTag(appRole, "RfcxAssetCleanup");
		this.appRole = appRole;
	}

	private String logTag;
	private String appRole;


	public void runFileSystemAssetCleanup(String[] assetDirsToScan, List<String> assetPathsFromDb, int checkFilesUnModifiedSinceThisManyMinutes) {
		runFileSystemAssetCleanup(assetDirsToScan, assetPathsFromDb, checkFilesUnModifiedSinceThisManyMinutes, true, true);
	}

	public void runFileSystemAssetCleanup(String[] assetDirsToScan, List<String> assetPathsFromDb, int checkFilesUnModifiedSinceThisManyMinutes, boolean deleteEmptyDirs, boolean verboseLogging) {

		List<File> allAssetFilesFromScan = getAssetFilesFromFilesystem(assetDirsToScan);
		List<File> eligibleAssetFilesFromScan = filterFilesByModificationDate(allAssetFilesFromScan, checkFilesUnModifiedSinceThisManyMinutes);
		List<String> relativeAssetPathsFromDb = conciseFilePaths(assetPathsFromDb);

		List<File> assetFilesToDelete = new ArrayList<File>();

		for (File assetFile : eligibleAssetFilesFromScan) {
			String relFilePath = conciseFilePath(assetFile.getAbsolutePath(), this.appRole);
			if (	!ArrayUtils.doesStringListContainString( relativeAssetPathsFromDb, relFilePath )
				&&	!ArrayUtils.doesStringArrayContainString( assetDirsToScan, assetFile.getAbsolutePath() )
			) {
					assetFilesToDelete.add(assetFile);
			}
		}

		if (verboseLogging) {
			Log.d(logTag, "Asset Cleanup - "
					+ allAssetFilesFromScan.size() + " file" + ((allAssetFilesFromScan.size() != 1) ? "" : "s") + " found in scan. "
					+ ((eligibleAssetFilesFromScan.size() == 0) ? "None" : eligibleAssetFilesFromScan.size())
					+ " are older than cleanup threshold of " + DateTimeUtils.milliSecondDurationAsReadableString(checkFilesUnModifiedSinceThisManyMinutes * 60 * 1000) + ". "
					+ ((assetFilesToDelete.size() == 0) ? "" : (assetFilesToDelete.size() + " will be deleted."))
			);
		}

		List<String> deletedAssetPaths = new ArrayList<String>();
		for (String filePath : FileUtils.deleteAndReturnFilePaths(assetFilesToDelete)) {
			deletedAssetPaths.add(conciseFilePath(filePath, this.appRole));
		}
		if (deletedAssetPaths.size() > 0) {
			Log.d(logTag, "Asset Cleanup - Deleted - " + TextUtils.join(", ", deletedAssetPaths));
		}

		List<String> deletedAssetDirs = new ArrayList<String>();
		if (deleteEmptyDirs) {
			for (int i = 0; i < 2; i++) {
				List<File> allEmptyAssetDirs = getAssetEmptyDirsOnFilesystem(assetDirsToScan);
				List<File> filteredEmptyAssetDirs = filterFilesByModificationDate(allEmptyAssetDirs, checkFilesUnModifiedSinceThisManyMinutes);
				for (String dirPath : FileUtils.deleteAndReturnFilePaths(filteredEmptyAssetDirs)) {
					deletedAssetDirs.add(conciseFilePath(dirPath, this.appRole));
				}
			}
		}

		if (deletedAssetDirs.size() > 0) {
			Log.d(logTag, "Asset Cleanup - Empty Directories - " + TextUtils.join(", ", deletedAssetDirs));
		} else if (verboseLogging) {
			Log.d(logTag, "Asset Cleanup - No Empty Directories were eligible for cleanup.");
		}
	}


	public void runCleanupOnOneDir(String dirPath, long checkFilesUnModifiedSinceThisManyMilliSeconds, boolean deleteEmptyDirs) {
		runFileSystemAssetCleanup(new String[] { dirPath }, new ArrayList<String>(), Math.round(checkFilesUnModifiedSinceThisManyMilliSeconds/60000), deleteEmptyDirs, false);
	}


	private static String getFilesDirBase(String appRole) {
		return "org.rfcx.guardian."+appRole.toLowerCase(Locale.US)+"/files/";
	}

	public static String conciseFilePath(String filePath, String appRole) {
		String appRoleBaseDir = getFilesDirBase(appRole);
		return ((filePath.length() > appRoleBaseDir.length()) && (filePath.indexOf(appRoleBaseDir) >= 0)) ? filePath.substring(filePath.indexOf(appRoleBaseDir)+ appRoleBaseDir.length()) : filePath;
	}

	private List<String> conciseFilePaths(List<String> filePathList) {
		List<String> assetFilePaths = new ArrayList<String>();
		for (String assetFilePath : filePathList) {
			assetFilePaths.add(conciseFilePath(assetFilePath, this.appRole));
		}
		return assetFilePaths;
	}


	private static List<File> getAssetFilesFromFilesystem(String[] dirsToScan) {

		List<File> assetFiles = new ArrayList<File>();
		for (String assetDir : dirsToScan) {
			for (File assetFile : FileUtils.getDirectoryContents(assetDir, true)) {
				assetFiles.add(assetFile);
			}
		}
		return assetFiles;
	}

	private static List<File> filterFilesByModificationDate(List<File> unFilteredAssetFiles, int ifUnModifiedSinceThisManyMinutes) {

		final long ifNotModifiedWithin = ifUnModifiedSinceThisManyMinutes * 60 * 1000;

		List<File> assetFiles = new ArrayList<File>();
		for (File assetFile : unFilteredAssetFiles) {
			if (FileUtils.millisecondsSinceLastModified(assetFile) > ifNotModifiedWithin) {
				assetFiles.add(assetFile);
			}
		}
		return assetFiles;
	}

	private static List<File> getAssetEmptyDirsOnFilesystem(String[] dirsToScan) {

		List<File> assetDirs = new ArrayList<File>();
		for (String assetDir : dirsToScan) {
			for (File innerDir : FileUtils.getEmptyDirectories(assetDir)) {
				assetDirs.add(innerDir);
			}
		}
		return assetDirs;
	}
	
}
