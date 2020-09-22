package org.rfcx.guardian.admin.device.sentinel;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;

public class SentinelPowerDb {
	
	public SentinelPowerDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbSentinelPowerBattery = new DbSentinelPowerBattery(context);
		this.dbSentinelPowerInput = new DbSentinelPowerInput(context);
		this.dbSentinelPowerSystem = new DbSentinelPowerSystem(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "sentinel-power";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VOLTAGE = "voltage";
	static final String C_CURRENT = "current";
	static final String C_MISC = "misc";
	static final String C_POWER = "power";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VOLTAGE, C_CURRENT, C_MISC, C_POWER};

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VOLTAGE).append(" TEXT")
			.append(", ").append(C_CURRENT).append(" TEXT")
			.append(", ").append(C_MISC).append(" TEXT")
			.append(", ").append(C_POWER).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbSentinelPowerBattery {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "battery";

		public DbSentinelPowerBattery(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(long measuredAt, long voltage, long current, String misc, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_MISC, misc);
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
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(long measuredAt, long voltage, long current, long misc, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_MISC, misc);
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
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(long measuredAt, long voltage, long current, long misc, long power) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measuredAt);
			values.put(C_VOLTAGE, voltage);
			values.put(C_CURRENT, current);
			values.put(C_MISC, misc);
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
