package org.rfcx.guardian.guardian.device.android;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class DeviceDataTransferDb {
	
	public DeviceDataTransferDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbTransferred = new DbTransferred(context);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, DeviceDataTransferDb.class);
	private int VERSION = 1;
	static final String DATABASE = "data";
	static final String C_CREATED_AT = "created_at";
	static final String C_START_TIME = "start_time";
	static final String C_END_TIME = "end_time";
	static final String C_BYTES_RECEIVED_CURRENT = "bytes_received_current";
	static final String C_BYTES_SENT_CURRENT = "bytes_sent_current";
	static final String C_BYTES_RECEIVED_TOTAL = "bytes_received_total";
	static final String C_BYTES_SENT_TOTAL = "bytes_sent_total";
	private static final String[] ALL_COLUMNS = new String[] { C_START_TIME, C_END_TIME, C_BYTES_RECEIVED_CURRENT, C_BYTES_SENT_CURRENT, C_BYTES_RECEIVED_TOTAL, C_BYTES_SENT_TOTAL };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_START_TIME).append(" INTEGER")
			.append(", ").append(C_END_TIME).append(" INTEGER")
			.append(", ").append(C_BYTES_RECEIVED_CURRENT).append(" INTEGER")
			.append(", ").append(C_BYTES_SENT_CURRENT).append(" INTEGER")
			.append(", ").append(C_BYTES_RECEIVED_TOTAL).append(" INTEGER")
			.append(", ").append(C_BYTES_SENT_TOTAL).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbTransferred {

		final DbUtils dbUtils;

		private String TABLE = "transferred";
		
		public DbTransferred(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(Date created_at, Date start_time, Date end_time, long bytes_rx_current, long bytes_tx_current, long bytes_rx_total, long bytes_tx_total) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at.getTime());
			values.put(C_START_TIME, start_time.getTime());
			values.put(C_END_TIME, end_time.getTime());
			values.put(C_BYTES_RECEIVED_CURRENT, bytes_rx_current);
			values.put(C_BYTES_SENT_CURRENT, bytes_tx_current);
			values.put(C_BYTES_RECEIVED_TOTAL, bytes_rx_total);
			values.put(C_BYTES_SENT_TOTAL, bytes_tx_total);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		private List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}
	}
	public final DbTransferred dbTransferred;
	

}
