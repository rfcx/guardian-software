package org.rfcx.guardian.guardian.audio.encode;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;

public class AudioEncodeDb {
	
	public AudioEncodeDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbEncodeQueue = new DbEncodeQueue(context);
		this.dbEncoded = new DbEncoded(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_SAMPLE_RATE = "samplerate";
	static final String C_BITRATE = "bitrate";
	static final String C_CODEC = "codec";
	static final String C_DURATION = "duration";
	static final String C_CREATION_DURATION = "creation_duration";
	static final String C_ENCODE_PURPOSE = "encode_purpose";
	static final String C_FILEPATH = "filepath";
	static final String C_ATTEMPTS = "attempts";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_SAMPLE_RATE, C_BITRATE, C_CODEC, C_DURATION, C_CREATION_DURATION, C_ENCODE_PURPOSE, C_FILEPATH, C_ATTEMPTS };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_BITRATE).append(" INTEGER")
			.append(", ").append(C_CODEC).append(" TEXT")
			.append(", ").append(C_DURATION).append(" INTEGER")
			.append(", ").append(C_CREATION_DURATION).append(" INTEGER")
			.append(", ").append(C_ENCODE_PURPOSE).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbEncodeQueue {

		final DbUtils dbUtils;

		private String TABLE = "queued";
		
		public DbEncodeQueue(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration, String encode_purpose, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, value);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_SAMPLE_RATE, samplerate);
			values.put(C_BITRATE, bitrate);
			values.put(C_CODEC, codec);
			values.put(C_DURATION, duration);
			values.put(C_CREATION_DURATION, creation_duration);
			values.put(C_ENCODE_PURPOSE, encode_purpose);
			values.put(C_FILEPATH, filepath);
			values.put(C_ATTEMPTS, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRow(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
		}

		public void deleteSingleRow(String timestamp, String encodePurpose) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_TIMESTAMP, timestampValue, C_ENCODE_PURPOSE, encodePurpose);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public String[] getSingleRowByAudioId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_TIMESTAMP+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_TIMESTAMP, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_TIMESTAMP, timestamp);
		}
		
	}
	public final DbEncodeQueue dbEncodeQueue;
	
	public class DbEncoded {

		final DbUtils dbUtils;

		private String TABLE = "encoded";
		
		public DbEncoded(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration, String encode_purpose, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, value);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_SAMPLE_RATE, samplerate);
			values.put(C_BITRATE, bitrate);
			values.put(C_CODEC, codec);
			values.put(C_DURATION, duration);
			values.put(C_CREATION_DURATION, creation_duration);
			values.put(C_ENCODE_PURPOSE, encode_purpose);
			values.put(C_FILEPATH, filepath);
			values.put(C_ATTEMPTS, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getRowsWithLimit(int maxRows) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null, 0, maxRows);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRow(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
		}

		public void deleteSingleRow(String timestamp, String encodePurpose) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_TIMESTAMP, timestampValue, C_ENCODE_PURPOSE, encodePurpose);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public String[] getSingleRowByAudioId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_TIMESTAMP+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_TIMESTAMP, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_TIMESTAMP, timestamp);
		}
		
		
	}
	public final DbEncoded dbEncoded;
	
}
