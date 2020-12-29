package org.rfcx.guardian.guardian.api.methods.download;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.guardian.api.methods.segment.ApiSegmentUtils;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiDownloadDb {

	public ApiDownloadDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbQueued = new DbQueued(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "downloads";
	static final String C_CREATED_AT = "created_at";
	static final String C_ASSET_ID = "group_id";
	static final String C_PROTOCOL = "protocol";
	static final String C_ATTEMPTS = "attempts";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	public static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_ASSET_ID, C_PROTOCOL, C_ATTEMPTS, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] {  }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_ASSET_ID).append(" TEXT")
			.append(", ").append(C_PROTOCOL).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}

	public class DbQueued {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "queued";

		public DbQueued(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(String assetId, String protocol) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ASSET_ID, assetId);
			values.put(C_PROTOCOL, protocol);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

	}
	public final DbQueued dbQueued;

	
}
