package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceDiskDb {
	
	public DeviceDiskDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbDiskUsage = new DbDiskUsage(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceDiskDb.class);
	private int VERSION = 1;
	static final String DATABASE = "disk";
	static final String C_LABEL = "label";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VALUE_1 = "value_1";
	static final String C_VALUE_2 = "value_2";
	private static final String[] ALL_COLUMNS = new String[] { C_LABEL, C_MEASURED_AT, C_VALUE_1, C_VALUE_2 };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_LABEL).append(" TEXT")
			.append(", ").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VALUE_1).append(" TEXT")
			.append(", ").append(C_VALUE_2).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbDiskUsage {

		final DbUtils dbUtils;

		private String TABLE = "usage";
		
		public DbDiskUsage(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String label, Date measured_at, long bytes_used, long bytes_available) {
			
			ContentValues values = new ContentValues();
			values.put(C_LABEL, label);
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, bytes_used);
			values.put(C_VALUE_2, bytes_available);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		private List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_MEASURED_AT, date);
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}

	}
	public final DbDiskUsage dbDiskUsage;
	
	
}
