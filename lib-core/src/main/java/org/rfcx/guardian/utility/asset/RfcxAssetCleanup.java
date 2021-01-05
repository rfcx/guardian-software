package org.rfcx.guardian.utility.asset;

import android.text.TextUtils;
import android.util.Log;

import org.rfcx.guardian.utility.datetime.DateTimeUtils;
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


	public void runFileSystemAssetCleanup(String[] assetDirectoriesToScan, List<String> assetFilePathsFromDatabase, int checkFilesUnModifiedSinceThisManyMinutes) {
		runFileSystemAssetCleanup(assetDirectoriesToScan, assetFilePathsFromDatabase, checkFilesUnModifiedSinceThisManyMinutes, true, true);
	}

	public void runFileSystemAssetCleanup(String[] assetDirectoriesToScan, List<String> assetFilePathsFromDatabase, int checkFilesUnModifiedSinceThisManyMinutes, boolean deleteEmptyDirectories, boolean printToLogEvenIfNoActionIsTaken) {

		List<File> allAssetFilesFromFilesystem = getAssetFilesFromFilesystem(assetDirectoriesToScan);
		List<File> filteredAssetFilesFromFilesystem = filterFilesByElapsedTimeSinceModification(allAssetFilesFromFilesystem, checkFilesUnModifiedSinceThisManyMinutes);
		List<String> relativeAssetFilePathsFromDatabase = relativizeFilePathList(assetFilePathsFromDatabase);

		List<File> assetFilesToRemove = new ArrayList<File>();

		for (File assetFile : filteredAssetFilesFromFilesystem) {
			if (!ArrayUtils.doesStringListContainString(relativeAssetFilePathsFromDatabase, relativeFilePath(assetFile.getAbsolutePath()))) {
				assetFilesToRemove.add(assetFile);
			}
		}

		if (printToLogEvenIfNoActionIsTaken) {
			Log.d(logTag, "Asset Cleanup - "
					+ allAssetFilesFromFilesystem.size() + " file(s) found. "
					+ ((filteredAssetFilesFromFilesystem.size() == 0) ? "No" : filteredAssetFilesFromFilesystem.size())
					+ " file(s) older than cleanup threshold of " + DateTimeUtils.milliSecondDurationAsReadableString(checkFilesUnModifiedSinceThisManyMinutes * 60 * 1000) + ". "
					+ ((assetFilesToRemove.size() == 0) ? "No files are eligible for cleanup." : (assetFilesToRemove.size() + " will be deleted."))
			);
		}

		List<String> deletedAssetFilePaths = new ArrayList<String>();
		for (String filePath : FileUtils.deleteAndReturnFilePaths(assetFilesToRemove)) {
			deletedAssetFilePaths.add(filePath.substring(1+filePath.lastIndexOf("/")));
		}
		if (deletedAssetFilePaths.size() > 0) {
			Log.d(logTag, "Asset Cleanup - Deleted Files - " + TextUtils.join(" ", deletedAssetFilePaths));
		}

		List<String> deletedAssetDirectories = new ArrayList<String>();
		if (deleteEmptyDirectories) {
			for (int i = 0; i < 2; i++) {
				List<File> allEmptyAssetDirectories = getAssetEmptyDirectoriesOnFilesystem(assetDirectoriesToScan);
				List<File> filteredEmptyAssetDirectories = filterFilesByElapsedTimeSinceModification(allEmptyAssetDirectories, checkFilesUnModifiedSinceThisManyMinutes);
				for (String dirPath : FileUtils.deleteAndReturnFilePaths(filteredEmptyAssetDirectories)) {
					deletedAssetDirectories.add(relativeFilePath(dirPath));
				}
			}
		}

		if (deletedAssetDirectories.size() > 0) {
			Log.d(logTag, "Asset Cleanup - Empty Directories - " + TextUtils.join(" ", deletedAssetDirectories));
		} else if (printToLogEvenIfNoActionIsTaken) {
			Log.d(logTag, "Asset Cleanup - No Empty Directories were eligible for cleanup.");
		}
	}


	public void runCleanupOnOneDirectory(String dirPath, long checkFilesUnModifiedSinceThisManyMilliSeconds, boolean deleteEmptyDirectories) {
		runFileSystemAssetCleanup(new String[] { dirPath }, new ArrayList<String>(), Math.round(checkFilesUnModifiedSinceThisManyMilliSeconds/60000), deleteEmptyDirectories, false);
	}


	private String getDilesDirBase() {
		return "org.rfcx.guardian."+this.appRole.toLowerCase(Locale.US)+"/files/";
	}

	private String relativeFilePath(String filePath) {
		return ((filePath.length() > getDilesDirBase().length()) && (filePath.indexOf(getDilesDirBase()) >= 0)) ? filePath.substring(filePath.indexOf(getDilesDirBase())+getDilesDirBase().length()) : filePath;
	}

	private List<String> relativizeFilePathList(List<String> filePathList) {
		List<String> assetFilePaths = new ArrayList<String>();
		for (String assetFilePath : filePathList) {
			assetFilePaths.add(relativeFilePath(assetFilePath));
		}
		return assetFilePaths;
	}


	private static List<File> getAssetFilesFromFilesystem(String[] directoriesToScan) {

		List<File> assetFiles = new ArrayList<File>();
		for (String assetDir : directoriesToScan) {
			for (File assetFile : FileUtils.getDirectoryContents(assetDir, true)) {
				assetFiles.add(assetFile);
			}
		}
		return assetFiles;
	}

	private static List<File> filterFilesByElapsedTimeSinceModification(List<File> unFilteredAssetFiles, int ifUnModifiedSinceThisManyMinutes) {

		final long ifNotModifiedWithin = ifUnModifiedSinceThisManyMinutes * 60 * 1000;

		List<File> assetFiles = new ArrayList<File>();
		for (File assetFile : unFilteredAssetFiles) {
			if (FileUtils.millisecondsSinceLastModified(assetFile) > ifNotModifiedWithin) {
				assetFiles.add(assetFile);
			}
		}
		return assetFiles;
	}

	private static List<File> getAssetEmptyDirectoriesOnFilesystem(String[] directoriesToScan) {

		List<File> assetDirs = new ArrayList<File>();
		for (String assetDir : directoriesToScan) {
			for (File innerDir : FileUtils.getEmptyDirectories(assetDir)) {
				assetDirs.add(innerDir);
			}
		}
		return assetDirs;
	}
	
}
