package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceRebootDb {
	
	public DeviceRebootDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbRebootComplete = new DbRebootComplete(context);
		this.dbRebootAttempt = new DbRebootAttempt(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceRebootDb.class);
	private int VERSION = 1;
	static final String DATABASE = "reboot";
	static final String C_CREATED_AT = "created_at";
	static final String C_REBOOTED_AT = "rebooted_at";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_REBOOTED_AT };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
			sbOut.append("CREATE TABLE ").append(tableName)
				.append("(").append(C_CREATED_AT).append(" INTEGER")
				.append(", ").append(C_REBOOTED_AT).append(" INTEGER")
				.append(")");
		return sbOut.toString();
	}
	
	public class DbRebootComplete {

		final DbUtils dbUtils;

		private String TABLE = "reboots";
		
		public DbRebootComplete(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(long rebootedAt) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_REBOOTED_AT, rebootedAt);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		private List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
	}
	public final DbRebootComplete dbRebootComplete;
	
	
	public class DbRebootAttempt {

		final DbUtils dbUtils;

		private String TABLE = "attempts";
		
		public DbRebootAttempt(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(long rebootedAt) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_REBOOTED_AT, rebootedAt);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		private List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
	}
	public final DbRebootAttempt dbRebootAttempt;
	

	
}
