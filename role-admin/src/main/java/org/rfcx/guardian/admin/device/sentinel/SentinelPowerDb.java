package org.rfcx.guardian.admin.device.sentinel;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;

public class SentinelPowerDb {
	
	public SentinelPowerDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbSentinelPowerBattery = new DbSentinelPowerBattery(context);
		this.dbSentinelPowerInput = new DbSentinelPowerInput(context);
		this.dbSentinelPowerSystem = new DbSentinelPowerSystem(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "sentinel-power";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VOLTAGE = "voltage";
	static final String C_CURRENT = "current";
	static final String C_TEMPERATURE = "temperature";
	static final String C_POWER = "power";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VOLTAGE, C_CURRENT, C_TEMPERATURE, C_POWER};

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VOLTAGE).append(" INTEGER")
			.append(", ").append(C_CURRENT).append(" INTEGER")
			.append(", ").append(C_TEMPERATURE).append(" INTEGER")
			.append(", ").append(C_POWER).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbSentinelPowerBattery {

		final DbUtils dbUtils;

		private String TABLE = "battery";

		public DbSentinelPowerBattery(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(long measuredAt, long voltage, long current, long temperature, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_TEMPERATURE, temperature);
			values.put(C_POWER, power);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public JSONArray getLatestRowAsJsonArray() {
			return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, null);
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

		public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
			return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
		}

	}
	public final DbSentinelPowerBattery dbSentinelPowerBattery;



	public class DbSentinelPowerInput {

		final DbUtils dbUtils;

		private String TABLE = "input";

		public DbSentinelPowerInput(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(long measuredAt, long voltage, long current, long temperature, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_TEMPERATURE, temperature);
			values.put(C_POWER, power);

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

		public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
			return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
		}

	}
	public final DbSentinelPowerInput dbSentinelPowerInput;



	public class DbSentinelPowerSystem {

		final DbUtils dbUtils;

		private String TABLE = "system";

		public DbSentinelPowerSystem(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(long measuredAt, long voltage, long current, long temperature, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_TEMPERATURE, temperature);
			values.put(C_POWER, power);

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

		public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
			return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
		}

	}
	public final DbSentinelPowerSystem dbSentinelPowerSystem;

	
}
