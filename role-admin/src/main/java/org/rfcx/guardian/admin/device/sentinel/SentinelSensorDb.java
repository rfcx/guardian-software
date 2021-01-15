package org.rfcx.guardian.admin.device.sentinel;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class SentinelSensorDb {

	public SentinelSensorDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbAccelerometer = new DbAccelerometer(context);
		this.dbCompass = new DbCompass(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "sentinel-sensor";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VALUE_1 = "value_1";
	static final String C_VALUE_2 = "value_2";
	static final String C_VALUE_3 = "value_3";
	static final String C_VALUE_4 = "value_4";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VALUE_1, C_VALUE_2, C_VALUE_3, C_VALUE_4 };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VALUE_1).append(" TEXT")
			.append(", ").append(C_VALUE_2).append(" TEXT")
			.append(", ").append(C_VALUE_3).append(" TEXT")
			.append(", ").append(C_VALUE_4).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbAccelerometer {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "accelerometer";
		
		public DbAccelerometer(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}
		
		public int insert(long measured_at, String value_1, String value_2, String value_3, String value_4) {
			
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at);
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
			
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
	public final DbAccelerometer dbAccelerometer;





	public class DbCompass {

		final DbUtils dbUtils;

		private String TABLE = "compass";

		public DbCompass(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(long measured_at, String value_1, String value_2, String value_3, String value_4) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at);
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));

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
	public final DbCompass dbCompass;
	
	

	
	

	
}
