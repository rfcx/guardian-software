package database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.src_android.RfcxSource;

import utility.DateTimeUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SmsDb {

	public SmsDb(Context context) {
		this.dbSms = new DbSms(context);
	}
	
	private static final String TAG = SmsDb.class.getSimpleName();
	static final int VERSION = 1;
	static final String DATABASE = "sms";
	static final String C_CREATED_AT = "created_at";
	static final String C_ORIGIN = "origin";
	static final String C_MESSAGE = "message";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_ORIGIN, C_MESSAGE };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(").append(C_CREATED_AT).append(" DATETIME");
		sbOut.append(", "+C_ORIGIN+" TEXT");
		sbOut.append(", "+C_MESSAGE+" TEXT");
		return sbOut.append(")").toString();
	}
	
	public class DbSms {
		private String TABLE = "sms";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) {
					Log.e(TAG, e.getMessage());
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG, e.getMessage()); }
			}
		}
		final DbHelper dbHelper;
		public DbSms(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String origin, String message) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			values.put(C_ORIGIN, origin);
			values.put(C_MESSAGE, message);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getSmsSince(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
//			try { Cursor cursor = db.query(TABLE, STATS, C_CREATED_AT+">=?",
//					new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, null);
//				try { return cursor.moveToNext() ? new int[] { cursor.getInt(0), cursor.getInt(1) } : null;
//				} finally { cursor.close(); }
//			} finally { db.close(); }
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG, (e!=null) ? e.getMessage() : "Null Exception"); } finally { db.close(); }
			return list;
		}
		public void clearSmsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<'"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
	}
	public final DbSms dbSms;

}
