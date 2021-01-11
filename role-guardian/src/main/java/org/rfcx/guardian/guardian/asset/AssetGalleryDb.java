package org.rfcx.guardian.guardian.asset;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AssetGalleryDb {

	public AssetGalleryDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbAudio = new DbAudio(context);
		this.dbClassifier = new DbClassifier(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "gallery";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_ASSET_ID = "asset_id";
	static final String C_ASSET_TYPE = "asset_type";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_FILEPATH = "filepath";
	static final String C_FILESIZE = "filesize";
	static final String C_META_TEXT = "meta_tag";
	static final String C_META_NUMERIC_1 = "meta_numeric_1";
	static final String C_META_NUMERIC_2 = "meta_numeric_2";
	static final String C_ATTEMPTS = "attempts";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_ASSET_ID, C_ASSET_TYPE, C_FORMAT, C_DIGEST, C_FILEPATH, C_FILESIZE, C_META_TEXT, C_META_NUMERIC_1, C_META_NUMERIC_2, C_ATTEMPTS, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_ASSET_ID).append(" TEXT")
			.append(", ").append(C_ASSET_TYPE).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_FILESIZE).append(" INTEGER")
			.append(", ").append(C_META_TEXT).append(" TEXT")
			.append(", ").append(C_META_NUMERIC_1).append(" INTEGER")
			.append(", ").append(C_META_NUMERIC_2).append(" INTEGER")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbAudio {

		final DbUtils dbUtils;

		private String TABLE = "audio";
		
		public DbAudio(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String assetId, String assetType, String format, String digest, String filePath, long fileSize, String metaText, long metaNumeric_1, long metaNumeric_2) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ASSET_ID, assetId);
			values.put(C_ASSET_TYPE, assetType);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_FILEPATH, filePath);
			values.put(C_FILESIZE, fileSize);
			values.put(C_META_TEXT, metaText);
			values.put(C_META_NUMERIC_1, metaNumeric_1);
			values.put(C_META_NUMERIC_2, metaNumeric_2);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public int getCountByAssetId(String assetId) {
			return this.dbUtils.getCount(TABLE, C_ASSET_ID +"=?",new String[] { assetId });
		}
		
	}
	public final DbAudio dbAudio;


	public class DbClassifier {

		final DbUtils dbUtils;

		private String TABLE = "classifier";

		public DbClassifier(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(String assetId, String assetType, String format, String digest, String filePath, long fileSize, String metaTag, long metaNumeric_1, long metaNumeric_2) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ASSET_ID, assetId);
			values.put(C_ASSET_TYPE, assetType);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_FILEPATH, filePath);
			values.put(C_FILESIZE, fileSize);
			values.put(C_META_TEXT, metaTag);
			values.put(C_META_NUMERIC_1, metaNumeric_1);
			values.put(C_META_NUMERIC_2, metaNumeric_2);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public int getCountByAssetId(String assetId) {
			return this.dbUtils.getCount(TABLE, C_ASSET_ID +"=?",new String[] { assetId });
		}


	}
	public final DbClassifier dbClassifier;


	
}
