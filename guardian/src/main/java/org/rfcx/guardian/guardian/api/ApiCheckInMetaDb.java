package org.rfcx.guardian.guardian.api;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiCheckInMetaDb {

	public ApiCheckInMetaDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbMeta = new DbMeta(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, ApiCheckInMetaDb.class);
	private int VERSION = 1;
	static final String DATABASE = "checkin";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_JSON = "json";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_JSON, C_LAST_ACCESSED_AT };
	
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_JSON).append(" TEXT")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbMeta {

		final DbUtils dbUtils;

		private String TABLE = "meta";
		
		public DbMeta(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(long timestamp, String json) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp);
			values.put(C_JSON, json);
			values.put(C_LAST_ACCESSED_AT, 0);
			
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
		
		public int deleteSingleRowByTimestamp(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
			return 0;
		}
		
		public long updateLastAccessedAtByTimestamp(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_TIMESTAMP, timestampValue);
			return rightNow;
		}

	}
	public final DbMeta dbMeta;
	
	
}
