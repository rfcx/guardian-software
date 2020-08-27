package org.rfcx.guardian.guardian.api.mqtt;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiCheckInStatsDb {

	public ApiCheckInStatsDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbStats = new DbStats(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "checkin";
	static final String C_GUID = "guid";
	static final String C_LATENCY = "latency";
	static final String C_SIZE = "size";
	static final String C_COMPLETED_AT = "created_at";
	private static final String[] ALL_COLUMNS = new String[] { C_GUID, C_LATENCY, C_SIZE, C_COMPLETED_AT };
	
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_GUID).append(" TEXT")
			.append(", ").append(C_LATENCY).append(" TEXT")
			.append(", ").append(C_SIZE).append(" TEXT")
			.append(", ").append(C_COMPLETED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbStats {

		final DbUtils dbUtils;

		private String TABLE = "stats";
		
		public DbStats(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String guid, long latency, long size) {
			
			ContentValues values = new ContentValues();
			values.put(C_GUID, guid);
			values.put(C_LATENCY, latency);
			values.put(C_SIZE, size);
			values.put(C_COMPLETED_AT, (new Date()).getTime());
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_COMPLETED_AT, date);
		}

		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}


	}
	public final DbStats dbStats;
	
	
}
