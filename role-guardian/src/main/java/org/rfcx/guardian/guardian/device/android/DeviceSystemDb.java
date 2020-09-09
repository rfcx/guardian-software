package org.rfcx.guardian.guardian.device.android;

import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

public class DeviceSystemDb {
	
	public DeviceSystemDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbMqttBrokerConnections = new DbMqttBrokerConnections(context);
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

	public class DbMqttBrokerConnections {

		final DbUtils dbUtils;

		private String TABLE = "mqttbroker";

		public DbMqttBrokerConnections(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(Date measured_at, long connection_latency, long subscription_latency, String protocol, String host, int port) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, connection_latency+"*"+subscription_latency);
			values.put(C_VALUE_2, (protocol + "://" + host + ":" + port).replaceAll("\\*", "-").replaceAll("\\|","-"));

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
	public final DbMqttBrokerConnections dbMqttBrokerConnections;

	public class DbDateTimeOffsets {

		final DbUtils dbUtils;

		private String TABLE = "datetimeoffsets";

		public DbDateTimeOffsets(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(long measured_at, String source, long offset, String timezone) {

			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at);
			values.put(C_VALUE_1, source);
			values.put(C_VALUE_2, offset+"*"+timezone);

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
