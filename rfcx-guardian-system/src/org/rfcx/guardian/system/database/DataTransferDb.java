package org.rfcx.guardian.system.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.DateTimeUtils;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DataTransferDb {
	
	public DataTransferDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbTransferred = new DbTransferred(context);
	}

	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+DataTransferDb.class.getSimpleName();
	public DateTimeUtils dateTimeUtils = new DateTimeUtils();
	private int VERSION = 1;
	static final String DATABASE = "data";
	static final String C_ID = BaseColumns._ID;
	static final String C_CREATED_AT = "created_at";
	static final String C_START_TIME = "start_time";
	static final String C_END_TIME = "end_time";
	static final String C_BYTES_RECEIVED_CURRENT = "bytes_received_current";
	static final String C_BYTES_SENT_CURRENT = "bytes_sent_current";
	static final String C_BYTES_RECEIVED_TOTAL = "bytes_received_total";
	static final String C_BYTES_SENT_TOTAL = "bytes_sent_total";
	private static final String[] CONCAT_ROWS = { "COUNT("+C_CREATED_AT+")", "GROUP_CONCAT( "+C_START_TIME+" || '*' || "+C_END_TIME+" || '*' || "+C_BYTES_RECEIVED_CURRENT+" || '*' || "+C_BYTES_SENT_CURRENT+" || '*' || "+C_BYTES_RECEIVED_TOTAL+" || '*' || "+C_BYTES_SENT_TOTAL+", '|')" };
	private static final String[] ALL_COLUMNS = new String[] { C_ID, C_CREATED_AT, C_START_TIME, C_END_TIME, C_BYTES_RECEIVED_CURRENT, C_BYTES_SENT_CURRENT, C_BYTES_RECEIVED_TOTAL, C_BYTES_SENT_TOTAL };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(");
		sbOut.append(C_ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT");
		sbOut.append(", "+C_CREATED_AT).append(" DATETIME");
		sbOut.append(", "+C_START_TIME+" DATETIME");
		sbOut.append(", "+C_END_TIME+" DATETIME");
		sbOut.append(", "+C_BYTES_RECEIVED_CURRENT+" TEXT");
		sbOut.append(", "+C_BYTES_SENT_CURRENT+" TEXT");
		sbOut.append(", "+C_BYTES_RECEIVED_TOTAL+" TEXT");
		sbOut.append(", "+C_BYTES_SENT_TOTAL+" TEXT");
		return sbOut.append(")").toString();
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
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : org.rfcx.guardian.utility.Constants.NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : org.rfcx.guardian.utility.Constants.NULL_EXC); }
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
			values.put(C_CREATED_AT, dateTimeUtils.getDateTime(created_at));
			values.put(C_START_TIME, dateTimeUtils.getDateTime(start_time));
			values.put(C_END_TIME, dateTimeUtils.getDateTime(end_time));
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
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : org.rfcx.guardian.utility.Constants.NULL_EXC); } finally { db.close(); }
			return list;
		}
		public Cursor getAllRowsAsCursor() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : org.rfcx.guardian.utility.Constants.NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbTransferred dbTransferred;
	

}
