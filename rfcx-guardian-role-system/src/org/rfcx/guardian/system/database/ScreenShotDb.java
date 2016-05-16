package org.rfcx.guardian.system.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class ScreenShotDb {
	
	public ScreenShotDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbCaptured = new DbCaptured(context);
	}

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+ScreenShotDb.class.getSimpleName();
	private int VERSION = 1;
	static final String DATABASE = "screenshots";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_FILEPATH = "filepath";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_FILEPATH };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName);
		sbOut.append("(").append(C_CREATED_AT).append(" INTEGER");
		sbOut.append(", "+C_TIMESTAMP+" TEXT");
		sbOut.append(", "+C_FORMAT+" TEXT");
		sbOut.append(", "+C_DIGEST+" TEXT");
		sbOut.append(", "+C_FILEPATH+" TEXT");
		return sbOut.append(")").toString();
	}
	
	public class DbCaptured {
		private String TABLE = "captured";
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
		public DbCaptured(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String timestamp, String format, String digest, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_FILEPATH, filepath);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		public List<String[]> getAllCaptured() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { 
				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null,  C_CREATED_AT+" DESC", null);
				if (cursor.getCount() > 0) {
					if (cursor.moveToFirst()) { 
						do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) });
						} while (cursor.moveToNext()); 
					} 
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e); 
			} finally { 
				db.close(); 
			}
			return list;
		}
		
		public void clearCapturedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public void deleteSingleRowByTimestamp(String timestamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_TIMESTAMP+"='"+timestamp+"'");
			} finally { db.close(); }
		}
		
		public String[] getSingleRowByTimestamp(String timestamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] row = new String[] {null,null,null};
			try { 
				Cursor cursor = db.query(TABLE, ALL_COLUMNS, " "+C_TIMESTAMP+" = ?", new String[] { timestamp }, null, null, C_CREATED_AT+" DESC", "1");
				if (cursor.getCount() > 0) {
					if (cursor.moveToFirst()) { 
						do { row = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) };
						} while (cursor.moveToNext());
					}
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e); 
			} finally { 
				db.close(); 
			}
			return row;
		}

	}
	public final DbCaptured dbCaptured;
	
}
