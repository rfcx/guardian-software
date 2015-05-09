package org.rfcx.guardian.database;

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
import android.util.Log;

public class SmsDb {

	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public SmsDb(Context context) {
		this.dbReceived = new DbReceived(context);
		this.dbQueued = new DbQueued(context);
	}
	
	private static final String TAG = "RfcxGuardian-"+SmsDb.class.getSimpleName();
	public DateTimeUtils dateTimeUtils = new DateTimeUtils();
	static final int VERSION = 1;
	static final String DATABASE = "sms";
	static final String C_CREATED_AT = "created_at";
	static final String C_NUMBER = "number";
	static final String C_MESSAGE = "message";
	static final String C_DIGEST = "digest";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_NUMBER, C_MESSAGE, C_DIGEST };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(").append(C_CREATED_AT).append(" DATETIME");
		sbOut.append(", "+C_NUMBER+" TEXT");
		sbOut.append(", "+C_MESSAGE+" TEXT");
		sbOut.append(", "+C_DIGEST+" TEXT");
		return sbOut.append(")").toString();
	}
	
	public class DbReceived {
		private String TABLE = "received";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); }
			}
		}
		final DbHelper dbHelper;
		public DbReceived(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String created_at, String number, String message, String digest) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at);
			values.put(C_NUMBER, number);
			values.put(C_MESSAGE, message);
			values.put(C_DIGEST, digest);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllMessages() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearMessageBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public void deleteSingleMessage(String digest) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_DIGEST+"='"+digest+"'");
			} finally { db.close(); }
		}
	}
	public final DbReceived dbReceived;
	
	public class DbQueued {
		private String TABLE = "queued";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); }
			}
		}
		final DbHelper dbHelper;
		public DbQueued(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String created_at, String number, String message, String digest) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at);
			values.put(C_NUMBER, number);
			values.put(C_MESSAGE, message);
			values.put(C_DIGEST, digest);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllMessages() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearMessagesBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public void deleteSingleMessage(String digest) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_DIGEST+"='"+digest+"'");
			} finally { db.close(); }
		}
	}
	public final DbQueued dbQueued;

}
