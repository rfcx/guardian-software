package org.rfcx.guardian.guardian.api.asset;

import android.content.Context;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.util.ArrayList;
import java.util.List;

public class AssetUtils {

	public AssetUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AssetUtils");

	private RfcxGuardian app;







	public String getAssetExchangeLogList(String assetStatus, int rowLimit) {

		List<String[]> assetRows = new ArrayList<String[]>();
		if (assetStatus.equalsIgnoreCase("purged")) {
			assetRows = app.apiAssetExchangeLogDb.dbPurged.getLatestRowsWithLimitExcludeCreatedAt(rowLimit);
		}
		return DbUtils.getConcatRows(assetRows);
	}





}
