package org.rfcx.guardian.guardian.device.android;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class DeviceSensorDb {
	
	public DeviceSensorDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbLightMeter = new DbLightMeter(context);
		this.dbAccelerometer = new DbAccelerometer(context);
		this.dbGeoPosition = new DbGeoPosition(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceSensorDb.class);
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
	
	public class DbLightMeter {

		final DbUtils dbUtils;

		private String TABLE = "lightmeter";
		
		public DbLightMeter(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, long luminosity, String value_2) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, luminosity);
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbLightMeter dbLightMeter;
	
	
	public class DbAccelerometer {

		final DbUtils dbUtils;

		private String TABLE = "accelerometer";
		
		public DbAccelerometer(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date measured_at, String x_y_z, int sample_count) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, x_y_z);
			values.put(C_VALUE_2, sample_count);
			
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
	public final DbAccelerometer dbAccelerometer;
	
	public class DbGeoPosition {

		final DbUtils dbUtils;

		private String TABLE = "geoposition";
		
		public DbGeoPosition(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(double measured_at, double latitude, double longitude, double accuracy, double altitude) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, (long) Math.round(measured_at));
			values.put(C_VALUE_1, latitude+","+longitude);
			values.put(C_VALUE_2, Math.round(accuracy)+","+Math.round(altitude));
			
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
	public final DbGeoPosition dbGeoPosition;
	
}
