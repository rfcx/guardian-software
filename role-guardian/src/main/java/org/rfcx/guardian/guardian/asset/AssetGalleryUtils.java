package org.rfcx.guardian.guardian.asset;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.asset.RfcxAudioFileUtils;
import org.rfcx.guardian.utility.asset.RfcxClassifierFileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class AssetGalleryUtils {

	public AssetGalleryUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetGalleryUtils");

	private RfcxGuardian app;



	public String getGalleryAssetFilePath(String assetType, String assetId, String fileType) {

		Context context = app.getApplicationContext();
		long numericAssetId = Long.parseLong(assetId);

		if (assetType.equalsIgnoreCase("classifier")) {
			return RfcxClassifierFileUtils.getClassifierFileLocation_Gallery(context, numericAssetId);

		} else if (assetType.equalsIgnoreCase("audio")) {
			return RfcxAudioFileUtils.getAudioFileLocation_Gallery(context, numericAssetId, fileType);

		}/* else if (assetType.equalsIgnoreCase("apk")) {

		}*/

		return null;
	}


}
