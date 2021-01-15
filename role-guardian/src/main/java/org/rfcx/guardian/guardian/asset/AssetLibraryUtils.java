package org.rfcx.guardian.guardian.asset;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AssetLibraryUtils {

	public AssetLibraryUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetLibraryUtils");

	private RfcxGuardian app;



	public String getLibraryAssetFilePath(String assetType, String assetId, String fileType) {

		Context context = app.getApplicationContext();
		long numericAssetId = Long.parseLong(assetId);

		if (assetType.equalsIgnoreCase("classifier")) {
			return RfcxClassifierFileUtils.getClassifierFileLocation_Library(context, numericAssetId);

		} else if (assetType.equalsIgnoreCase("audio")) {
			return RfcxAudioFileUtils.getAudioFileLocation_Library(context, numericAssetId, fileType);

		}/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

		return null;
	}


}
