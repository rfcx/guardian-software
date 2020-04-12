package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceSystemDb {
	
	public DeviceSystemDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbCPU = new DbCPU(context);
		this.dbBattery = new DbBattery(context);
		this.dbPower = new DbPower(context);
		this.dbTelephony = new DbTelephony(context);
		this.dbOffline = new DbOffline(context);
		this.dbDateTimeOffsets = new DbDateTimeOffsets(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "device";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VALUE_1 = "value_1";
	static final String C_VALUE_2 = "value_2";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VALUE_1, C_VALUE_2 };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VALUE_1).append(" TEXT")
			.append(", ").append(C_VALUE_2).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbCPU {

		final DbUtils dbUtils;

		private String TABLE = "cpu";
		
		public DbCPU(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, int cpu_percent, int cpu_clock) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, cpu_percent);
			values.put(C_VALUE_2, cpu_clock);
			
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
	public final DbCPU dbCPU;
	
	
	public class DbBattery {

		final DbUtils dbUtils;

		private String TABLE = "battery";
		
		public DbBattery(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, int battery_percent, int battery_temperature) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, battery_percent);
			values.put(C_VALUE_2, battery_temperature);
			
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
	public final DbBattery dbBattery;
	
	public class DbPower {

		final DbUtils dbUtils;

		private String TABLE = "power";
		
		public DbPower(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, boolean is_powered, boolean is_charged) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, is_powered);
			values.put(C_VALUE_2, is_charged);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public void insert(Date measured_at, int is_powered, int is_charged) {
			insert(measured_at, (is_powered == 1), (is_charged == 1));
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
	public final DbPower dbPower;
	
	public class DbTelephony {

		final DbUtils dbUtils;

		private String TABLE = "telephony";
		
		public DbTelephony(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, int signal_strength, String network_type, String carrier_name) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			// this is obviously a hack...
			// ...to concat two values into a single column.
			// may want to change/consider later
			values.put(C_VALUE_1, signal_strength+"*"+network_type);
			values.put(C_VALUE_2, carrier_name.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbTelephony dbTelephony;

	public class DbOffline {

		final DbUtils dbUtils;

		private String TABLE = "offline";
		
		public DbOffline(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, long offline_period, String carrier_name) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, offline_period);
			values.put(C_VALUE_2, carrier_name.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbOffline dbOffline;
	
	public class DbDateTimeOffsets {

		final DbUtils dbUtils;

		private String TABLE = "datetimeoffsets";
		
		public DbDateTimeOffsets(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(long measured_at, String source, long offset) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at);
			values.put(C_VALUE_1, source);
			values.put(C_VALUE_2, offset);
			
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
	public final DbDateTimeOffsets dbDateTimeOffsets;
	
}
