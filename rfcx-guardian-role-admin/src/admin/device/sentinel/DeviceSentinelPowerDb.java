package admin.device.sentinel;

import java.util.Date;
import java.util.List;

import rfcx.utility.database.DbUtils;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;

import admin.RfcxGuardian;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DeviceSentinelPowerDb {
	
	public DeviceSentinelPowerDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbSentinelPowerBattery = new DbSentinelPowerBattery(context);
		this.dbSentinelPowerInput = new DbSentinelPowerInput(context);
		this.dbSentinelPowerSystem = new DbSentinelPowerSystem(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSentinelPowerDb.class);
	private int VERSION = 1;
	static final String DATABASE = "sentinel-power";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VOLTAGE = "voltage";
	static final String C_CURRENT = "current";
	static final String C_TEMPERATURE = "temperature";
	static final String C_VALUE_4 = "value_4";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VOLTAGE, C_CURRENT, C_TEMPERATURE, C_VALUE_4 };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VOLTAGE).append(" TEXT")
			.append(", ").append(C_CURRENT).append(" TEXT")
			.append(", ").append(C_TEMPERATURE).append(" TEXT")
			.append(", ").append(C_VALUE_4).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbSentinelPowerBattery {

		final DbUtils dbUtils;

		private String TABLE = "battery";
		
		public DbSentinelPowerBattery(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VOLTAGE, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_CURRENT, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_TEMPERATURE, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbSentinelPowerBattery dbSentinelPowerBattery;
	
	
	
	public class DbSentinelPowerInput {

		final DbUtils dbUtils;

		private String TABLE = "input";
		
		public DbSentinelPowerInput(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VOLTAGE, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_CURRENT, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_TEMPERATURE, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbSentinelPowerInput dbSentinelPowerInput;
	
	
	
	public class DbSentinelPowerSystem {

		final DbUtils dbUtils;

		private String TABLE = "system";
		
		public DbSentinelPowerSystem(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VOLTAGE, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_CURRENT, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_TEMPERATURE, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbSentinelPowerSystem dbSentinelPowerSystem;
	
	
	

	
}
