package org.rfcx.guardian.admin.device.android.capture;

import java.util.Date;

import org.json.JSONArray;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.content.ContentValues;
import android.content.Context;

public class DeviceScreenShotDb {
	
	public DeviceScreenShotDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbCaptured = new DbCaptured(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "screenshots";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_FILEPATH = "filepath";
	static final String C_WIDTH = "width";
	static final String C_HEIGHT = "height";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_FILEPATH, C_WIDTH, C_HEIGHT, C_LAST_ACCESSED_AT };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_WIDTH).append(" INTEGER")
			.append(", ").append(C_HEIGHT).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbCaptured {

		final DbUtils dbUtils;

		private String TABLE = "captured";
		
		public DbCaptured(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}
		
		public int insert(String timestamp, String format, String digest, String width, String height, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_FILEPATH, filepath);
			values.put(C_WIDTH, (int) Integer.parseInt(width));
			values.put(C_HEIGHT, (int) Integer.parseInt(height));
			values.put(C_LAST_ACCESSED_AT, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public JSONArray getLatestRowAsJsonArray() {
			return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public int deleteSingleRowByTimestamp(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
			return 0;
		}
		
		public long updateLastAccessedAtByTimestamp(String timestamp) {
			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_TIMESTAMP, timestampValue);
			return rightNow;
		}

	}
	public final DbCaptured dbCaptured;
	
}
