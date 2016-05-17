package org.rfcx.guardian.api.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.api.RfcxGuardian;
import org.rfcx.guardian.utility.DateTimeUtils;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class CheckInDb {

	public CheckInDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbQueued = new DbQueued(context);
		this.dbSkipped = new DbSkipped(context);
		this.dbStashed = new DbStashed(context);
	}
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+CheckInDb.class.getSimpleName();
	private int VERSION = 1;
	static final String DATABASE = "checkin";
	static final String C_CREATED_AT = "created_at";
	static final String C_AUDIO = "audio";
	static final String C_JSON = "json";
	static final String C_ATTEMPTS = "attempts";
	static final String C_FILEPATH = "filepath";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_AUDIO, C_JSON, C_ATTEMPTS, C_FILEPATH };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" DATETIME")
			.append(", ").append(C_AUDIO).append(" TEXT")
			.append(", ").append(C_JSON).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
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
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
		}
		final DbHelper dbHelper;
		public DbQueued(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String audio, String json, String attempts, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllQueued() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, null);
			
//			ArrayList<String[]> list = new ArrayList<String[]>();
//			try { 
//				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
//				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
//					do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) });
//					} while (cursor.moveToNext());
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return list;
		}
		public List<String[]> getQueuedWithOffset(int rowOffset, int rowLimit) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
			
//			ArrayList<String[]> list = new ArrayList<String[]>();
//			try { 
//				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", ""+(rowOffset+rowLimit));
//				if ((cursor.getCount() > rowOffset) && cursor.moveToPosition(rowOffset)) {
//					do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) });
//					} while (cursor.moveToNext());
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return list;
		}
		
		public String[] getLatestRow() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
			
//			String[] latestRow = new String[] { null, null, null };
//			try { 
//				for (String[] singleRow : DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, C_CREATED_AT+" DESC", 0, 1)) {
//					latestRow = singleRow;
//				}
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return latestRow;
			
//			StlatestRoww = new String[] {null,null,null};
//			try { 
//				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
//				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
//					do { row = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) };
//					} while (cursor.moveToNext());
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return row;
		}
		
		public void deleteSingleRowByAudioAttachmentId(String audioId) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE substr("+C_AUDIO+",0,14)='"+audioId.substring(0,13)+"'");
			} finally { db.close(); }
		}
		
		public String[] getSingleRowByAudioAttachmentId(String audioId) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, " substr("+C_AUDIO+",0,14) = ?", new String[] { audioId.substring(0,13) }, C_CREATED_AT, 0);
//			String[] row = new String[] {null,null,null};
//			try { 
//				Cursor cursor = db.query(TABLE, ALL_COLUMNS, " substr("+C_AUDIO+",0,14) = ?", new String[] { audioId.substring(0,13) }, null, null, C_CREATED_AT+" DESC", "1");
//				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
//					do { row = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) };
//					} while (cursor.moveToNext());
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return row;
		}
		
		public void clearQueuedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+DateTimeUtils.getDateTime(date)+"'");
			} finally { db.close(); }
		}
		
		public void incrementSingleRowAttempts(String audioFile) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("UPDATE "+TABLE+" SET "+C_ATTEMPTS+"=cast("+C_ATTEMPTS+" as INT)+1 WHERE substr("+C_AUDIO+",0,14)='"+audioFile.substring(0,13)+"'");
			} finally { db.close(); }
		}
		
		public int getCount() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getCount(db, TABLE, null, null);
//			String[] QUERY = new String[] { "COUNT(*)" };
//			String[] countReturn = new String[] { "0" };
//			try { 
//				Cursor cursor = db.query(TABLE, QUERY, null, null, null, null, null, null);
//				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
//					do { countReturn = new String[] { cursor.getString(0) };
//					} while (cursor.moveToNext());
//				}
//				cursor.close();
//			} catch (Exception e) { 
//				RfcxLog.logExc(TAG, e);
//			}
//			return countReturn[0];
		}

	}
	public final DbQueued dbQueued;
	
	public class DbSkipped {
		private String TABLE = "skipped";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
		}
		final DbHelper dbHelper;
		public DbSkipped(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String created_at, String audio, String json, String attempts, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, created_at);
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllSkipped() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { 
				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
					do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) });
					} while (cursor.moveToNext());
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e);
			}
			return list;
		}
		public String[] getLatestRow() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] row = new String[] {null,null,null};
			try { 
				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
					do { row = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) };
					} while (cursor.moveToNext());
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e);
			}
			return row;
		}
		
		public void deleteSingleRowByAudioAttachment(String audioFile) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE substr("+C_AUDIO+",0,14)='"+audioFile.substring(0,13)+"'");
			} finally { db.close(); }
		}
		
		public void clearSkippedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+DateTimeUtils.getDateTime(date)+"'");
			} finally { db.close(); }
		}
		
		public String getCount() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] QUERY = new String[] { "COUNT(*)" };
			String[] countReturn = new String[] { "0" };
			try { 
				Cursor cursor = db.query(TABLE, QUERY, null, null, null, null, null, null);
				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
					do { countReturn = new String[] { cursor.getString(0) };
					} while (cursor.moveToNext());
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e);
			}
			return countReturn[0];
		}

	}
	public final DbSkipped dbSkipped;
	
	public class DbStashed {
		private String TABLE = "stashed";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				try {
					db.execSQL(createColumnString(TABLE));
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(TAG, e);
				}
			}
		}
		final DbHelper dbHelper;
		public DbStashed(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String audio, String json, String attempts, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
			values.put(C_AUDIO, audio);
			values.put(C_JSON, json);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_FILEPATH, filepath);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		public String getCount() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] QUERY = new String[] { "COUNT(*)" };
			String[] countReturn = new String[] { "0" };
			try { 
				Cursor cursor = db.query(TABLE, QUERY, null, null, null, null, null, null);
				if ((cursor.getCount() > 0) && cursor.moveToFirst()) {
					do { countReturn = new String[] { cursor.getString(0) };
					} while (cursor.moveToNext());
				}
				cursor.close();
			} catch (Exception e) { 
				RfcxLog.logExc(TAG, e);
			}
			return countReturn[0];
		}

	}
	public final DbStashed dbStashed;
	
}
