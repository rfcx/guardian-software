package guardian.device.android;

import java.util.Date;
import java.util.List;

import rfcx.utility.database.DbUtils;
import rfcx.utility.rfcx.RfcxLog;
import rfcx.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import guardian.RfcxGuardian;

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
		private String TABLE = "transferred";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { 
					RfcxLog.logExc(logTag, e);
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { 
					db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(logTag, e);
				}
			}
		}
		final DbHelper dbHelper;
		
		public DbTransferred(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(Date created_at, Date start_time, Date end_time, long bytes_rx_current, long bytes_tx_current, long bytes_rx_total, long bytes_tx_total) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at.getTime());
			values.put(C_START_TIME, start_time.getTime());
			values.put(C_END_TIME, end_time.getTime());
			values.put(C_BYTES_RECEIVED_CURRENT, bytes_rx_current);
			values.put(C_BYTES_SENT_CURRENT, bytes_tx_current);
			values.put(C_BYTES_RECEIVED_TOTAL, bytes_rx_total);
			values.put(C_BYTES_SENT_TOTAL, bytes_tx_total);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		private List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, null);
//			ArrayList<String[]> list = new ArrayList<String[]>();
//			try { 
//				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
//				if (cursor.getCount() > 0) {
//					if (cursor.moveToFirst()) { 
//						do { list.add(new String[] { cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6) });
//						} while (cursor.moveToNext());
//					} 
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			} finally { 
//				db.close(); 
//			}
//			return list;
		}
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { 
				db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { 
				db.close(); 
			}
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
//			String concatRows = null;
//			ArrayList<String> rowList = new ArrayList<String>();
//			try {
//				for (String[] row : getAllRows()) {
//					rowList.add(TextUtils.join("*", row));
//				}
//				concatRows = (rowList.size() > 0) ? TextUtils.join("|", rowList) : null;
//			} catch (Exception e) {
//				RfcxLog.logExc(TAG, e);
//			}
//			return concatRows;
		}
	}
	public final DbTransferred dbTransferred;
	

}
