package org.rfcx.guardian.guardian.api.mqtt;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiDiagnosticsDb {

	public ApiDiagnosticsDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbBandwidth = new DbBandwidth(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "diagnostics";
	static final String C_COMPLETED_AT = "created_at";
	static final String C_CHECKIN_ID = "checkin_id";
	static final String C_BANDWIDTH = "bandwidth";
	private static final String[] ALL_COLUMNS = new String[] { C_COMPLETED_AT, C_CHECKIN_ID, C_BANDWIDTH };
	
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_COMPLETED_AT).append(" INTEGER")
			.append(", ").append(C_CHECKIN_ID).append(" TEXT")
			.append(", ").append(C_BANDWIDTH).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbBandwidth {

		final DbUtils dbUtils;

		private String TABLE = "bandwidth";
		
		public DbBandwidth(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String checkInId, long bandwidth) {
			
			ContentValues values = new ContentValues();
			values.put(C_COMPLETED_AT, (new Date()).getTime());
			values.put(C_CHECKIN_ID, checkInId);
			values.put(C_BANDWIDTH, bandwidth);
			
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
	public final DbBandwidth dbBandwidth;
	
	
}
