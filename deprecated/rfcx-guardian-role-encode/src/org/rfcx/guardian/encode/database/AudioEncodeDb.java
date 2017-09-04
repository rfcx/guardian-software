package org.rfcx.guardian.encode.database;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.encode.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AudioEncodeDb {
	
	public AudioEncodeDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbEncodeQueue = new DbEncodeQueue(context);
		this.dbEncoded = new DbEncoded(context);
	}

	private static final String logTag = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioEncodeDb.class.getSimpleName();
	private int VERSION = 1;
	static final String DATABASE = "audio";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_SAMPLE_RATE = "samplerate";
	static final String C_BITRATE = "bitrate";
	static final String C_CODEC = "codec";
	static final String C_DURATION = "duration";
	static final String C_CREATION_DURATION = "creation_duration";
	static final String C_FILEPATH = "filepath";
	static final String C_ATTEMPTS = "attempts";
	
	private static final String[] ALL_COLUMNS = 
		new String[] { 
			C_CREATED_AT,	// 0 
			C_TIMESTAMP,	// 1
			C_FORMAT, 		// 2
			C_DIGEST, 		// 3
			C_SAMPLE_RATE,	// 4
			C_BITRATE, 		// 5
			C_CODEC, 		// 6
			C_DURATION, 	// 7
			C_CREATION_DURATION,// 8 
			C_FILEPATH, 	// 9
			C_ATTEMPTS 		// 10
		};
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_BITRATE).append(" INTEGER")
			.append(", ").append(C_CODEC).append(" TEXT")
			.append(", ").append(C_DURATION).append(" INTEGER")
			.append(", ").append(C_CREATION_DURATION).append(" INTEGER")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbEncodeQueue {
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
					RfcxLog.logExc(logTag, e);
				}
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(logTag, e);
				}
			}
		}
		final DbHelper dbHelper;
		
		public DbEncodeQueue(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, value);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_SAMPLE_RATE, samplerate);
			values.put(C_BITRATE, bitrate);
			values.put(C_CODEC, codec);
			values.put(C_DURATION, duration);
			values.put(C_CREATION_DURATION, creation_duration);
			values.put(C_FILEPATH, filepath);
			values.put(C_ATTEMPTS, 0);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String[] getLatestRow() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, null, null, C_TIMESTAMP, 0);
		}
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { 
				db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { 
				db.close(); 
			}
		}
		
		public void deleteSingleRow(String timestamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { 
				db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_TIMESTAMP+"='"+timestamp+"'");
			} finally { 
				db.close(); 
			}
		}
		
		public int getCount() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getCount(db, TABLE, null, null);
		}

		public String[] getSingleRowByAudioId(String audioId) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, " substr("+C_TIMESTAMP+",0,14) = ?", new String[] { audioId.substring(0,13) }, C_CREATED_AT, 0);
		}
		
		public void incrementSingleRowAttempts(String audioId) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("UPDATE "+TABLE+" SET "+C_ATTEMPTS+"=cast("+C_ATTEMPTS+" as INT)+1 WHERE substr("+C_TIMESTAMP+",0,14)='"+audioId.substring(0,13)+"'");
			} finally { db.close(); }
		}
		
	}
	public final DbEncodeQueue dbEncodeQueue;
	
	public class DbEncoded {
		private String TABLE = "encoded";
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
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { 
					RfcxLog.logExc(logTag, e);
				}
			}
		}
		final DbHelper dbHelper;
		
		public DbEncoded(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration, String filepath) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, value);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_SAMPLE_RATE, samplerate);
			values.put(C_BITRATE, bitrate);
			values.put(C_CODEC, codec);
			values.put(C_DURATION, duration);
			values.put(C_CREATION_DURATION, creation_duration);
			values.put(C_FILEPATH, filepath);
			values.put(C_ATTEMPTS, 0);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String[] getLatestRow() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, null, null, C_TIMESTAMP, 0);
		}
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { 
				db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { 
				db.close(); 
			}
		}
		
		public void deleteSingleRow(String timestamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { 
				db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_TIMESTAMP+"='"+timestamp+"'");
			} finally { 
				db.close(); 
			}
		}
		
		public void incrementSingleRowAttempts(String audioTimeStamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("UPDATE "+TABLE+" SET "+C_ATTEMPTS+"=cast("+C_ATTEMPTS+" as INT)+1 WHERE substr("+C_TIMESTAMP+",0,14)='"+audioTimeStamp.substring(0,13)+"'");
			} finally { db.close(); }
		}
		
		public int getCount() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getCount(db, TABLE, null, null);
		}

		public String[] getSingleRowByAudioId(String audioId) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getSingleRow(db, TABLE, ALL_COLUMNS, " substr("+C_TIMESTAMP+",0,14) = ?", new String[] { audioId.substring(0,13) }, C_CREATED_AT, 0);
		}
		
	}
	public final DbEncoded dbEncoded;
	
}
