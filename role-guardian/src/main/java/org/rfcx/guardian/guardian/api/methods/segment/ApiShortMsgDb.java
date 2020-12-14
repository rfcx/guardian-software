package org.rfcx.guardian.guardian.api.methods.segment;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiShortMsgDb {

	public ApiShortMsgDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbFullMsg = new DbFullMsg(context);
		this.dbMsgSegment = new DbMsgSegment(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "short-msg";
	static final String C_CREATED_AT = "created_at";
	static final String C_ID = "id";
	static final String C_PARENT_ID = "parent_id";
	static final String C_BODY = "body";
	static final String C_ATTEMPTS = "attempts";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	public static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_ID, C_PARENT_ID, C_BODY, C_ATTEMPTS, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] {  }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_ID).append(" TEXT")
			.append(", ").append(C_PARENT_ID).append(" TEXT")
			.append(", ").append(C_BODY).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}

	public class DbFullMsg {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "full";

		public DbFullMsg(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(String id, String body) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ID, id);
			values.put(C_BODY, body);
			values.put(C_ATTEMPTS, 0);
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

		public int deleteSingleRowById(String id) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_ID, id);
			return 0;
		}

		public long updateLastAccessedAtById(String id) {
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_ID, id);
			return rightNow;
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

	}
	public final DbFullMsg dbFullMsg;


	public class DbMsgSegment {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "segment";

		public DbMsgSegment(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(String id, String body) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_ID, id);
			values.put(C_BODY, body);
			values.put(C_ATTEMPTS, 0);
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

		public int deleteSingleRowById(String id) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_ID, id);
			return 0;
		}

		public long updateLastAccessedAtById(String id) {
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_ID, id);
			return rightNow;
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

	}
	public final DbMsgSegment dbMsgSegment;

	
}
