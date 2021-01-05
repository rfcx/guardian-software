package org.rfcx.guardian.guardian.audio.playback;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioPlaybackDb {

	public AudioPlaybackDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbQueued = new DbQueued(context);
		this.dbCompleted = new DbCompleted(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "playback";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_ASSET_ID = "asset_id";
	static final String C_FORMAT = "format";
	static final String C_SAMPLE_RATE = "sample_rate";
	static final String C_FILEPATH = "filepath";
	static final String C_DURATION = "duration";
	static final String C_ATTEMPTS = "attempts";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_ASSET_ID, C_FORMAT, C_SAMPLE_RATE, C_FILEPATH, C_DURATION, C_ATTEMPTS };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_ASSET_ID).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_DURATION).append(" INTEGER")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbQueued {

		final DbUtils dbUtils;

		private String TABLE = "queued";
		
		public DbQueued(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String assetId, String format, long sampleRate, long duration, String filePath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ASSET_ID, assetId);
			values.put(C_FORMAT, format);
			values.put(C_SAMPLE_RATE, sampleRate);
			values.put(C_FILEPATH, filePath);
			values.put(C_DURATION, duration);
			values.put(C_ATTEMPTS, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public int deleteSingleRowByCreatedAt(String createdAt) {
			this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_CREATED_AT, createdAt);
			return 0;
		}

		public void incrementSingleRowAttempts(String createdAt) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_CREATED_AT, createdAt);
		}

		
	}
	public final DbQueued dbQueued;




	public class DbCompleted {

		final DbUtils dbUtils;

		private String TABLE = "completed";

		public DbCompleted(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(String assetId, String format, long sampleRate, long duration, String filePath) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ASSET_ID, assetId);
			values.put(C_FORMAT, format);
			values.put(C_SAMPLE_RATE, sampleRate);
			values.put(C_FILEPATH, filePath);
			values.put(C_DURATION, duration);
			values.put(C_ATTEMPTS, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public int deleteSingleRowByCreatedAt(String createdAt) {
			this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_CREATED_AT, createdAt);
			return 0;
		}

		public void incrementSingleRowAttempts(String createdAt) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_CREATED_AT, createdAt);
		}


	}
	public final DbCompleted dbCompleted;

	
}
