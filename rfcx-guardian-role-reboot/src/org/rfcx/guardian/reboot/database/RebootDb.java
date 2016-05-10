package org.rfcx.guardian.reboot.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.reboot.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class RebootDb {
	
	public RebootDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbReboot = new DbReboot(context);
	}

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+RebootDb.class.getSimpleName();
	private int VERSION = 1;
	static final String DATABASE = "reboot";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName);
		sbOut.append("(").append(C_CREATED_AT).append(" INTEGER");
		sbOut.append(", "+C_TIMESTAMP+" TEXT");
		return sbOut.append(")").toString();
	}
	
	public class DbReboot {
		private String TABLE = "events";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { RfcxLog.logExc(TAG, e); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { RfcxLog.logExc(TAG, e); }
			}
		}
		final DbHelper dbHelper;
		public DbReboot(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(long timestamp) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, ""+timestamp);
			
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllEvents() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { RfcxLog.logExc(TAG, e); } finally { db.close(); }
			return list;
		}
		public void clearEventsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
	}
	public final DbReboot dbReboot;
	

	
}
