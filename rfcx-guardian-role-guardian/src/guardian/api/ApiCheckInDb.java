package guardian.api;

import java.util.Date;
import java.util.List;

import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.database.DbUtils;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import guardian.RfcxGuardian;

public class ApiCheckInDb {

	public ApiCheckInDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbQueued = new DbQueued(context);
		this.dbSent = new DbSent(context);
		this.dbSkipped = new DbSkipped(context);
		this.dbStashed = new DbStashed(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInDb.class);
	private int VERSION = 1;
	static final String DATABASE = "checkin";
	static final String C_CREATED_AT = "created_at";
	static final String C_AUDIO = "audio";
	static final String C_JSON = "json";
	static final String C_ATTEMPTS = "attempts";
	static final String C_FILEPATH = "filepath";
	public static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_AUDIO, C_JSON, C_ATTEMPTS, C_FILEPATH };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_AUDIO).append(" TEXT")
			.append(", ").append(C_JSON).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbQueued {

		final DbUtils dbUtils;

		private String TABLE = "queued";
		
		public DbQueued(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String audio, String json, String attempts, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public List<String[]> getLatestRowsWithLimit(int maxRows) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
		}
		
		public String[] getSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_AUDIO+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public void deleteAllRows() {
			this.dbUtils.deleteAllRows(TABLE);
		}

	}
	public final DbQueued dbQueued;
	
	public class DbSkipped {

		final DbUtils dbUtils;

		private String TABLE = "skipped";
		
		public DbSkipped(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String created_at, String audio, String json, String attempts, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at);
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public List<String[]> getLatestRowsWithLimit(int maxRows) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
		}
		
		public String[] getSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_AUDIO+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public void deleteAllRows() {
			this.dbUtils.deleteAllRows(TABLE);
		}

	}
	public final DbSkipped dbSkipped;
	
	public class DbStashed {

		final DbUtils dbUtils;

		private String TABLE = "stashed";
		
		public DbStashed(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String audio, String json, String attempts, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public List<String[]> getLatestRowsWithLimit(int maxRows) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
		}
		
		public String[] getSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_AUDIO+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public void deleteAllRows() {
			this.dbUtils.deleteAllRows(TABLE);
		}

	}
	public final DbStashed dbStashed;
	
	public class DbSent {
		
		final DbUtils dbUtils;

		private String TABLE = "sent";
		
		public DbSent(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String audio, String json, String attempts, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
		}
		
		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
		}
		
		public List<String[]> getLatestRowsWithLimit(int maxRows) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public void deleteSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
		}
		
		public String[] getSingleRowByAudioAttachmentId(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_AUDIO+",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public void decrementSingleRowAttempts(String audioId) {
			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
		public void deleteAllRows() {
			this.dbUtils.deleteAllRows(TABLE);
		}

	}
	public final DbSent dbSent;
	
}
