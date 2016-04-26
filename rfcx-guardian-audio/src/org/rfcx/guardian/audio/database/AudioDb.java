package org.rfcx.guardian.audio.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.audio.RfcxGuardian;
import org.rfcx.guardian.utility.RfcxConstants;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class AudioDb {
	
	public AudioDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbCaptured = new DbCaptured(context);
		this.dbEncoded = new DbEncoded(context);
	}

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+AudioDb.class.getSimpleName();
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
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_SAMPLE_RATE, C_BITRATE, C_CODEC, C_DURATION, C_CREATION_DURATION };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName);
		sbOut.append("(").append(C_CREATED_AT).append(" INTEGER");
		sbOut.append(", "+C_TIMESTAMP+" TEXT");
		sbOut.append(", "+C_FORMAT+" TEXT");
		sbOut.append(", "+C_DIGEST+" TEXT");
		sbOut.append(", "+C_SAMPLE_RATE+" INTEGER");
		sbOut.append(", "+C_BITRATE+" INTEGER");
		sbOut.append(", "+C_CODEC+" TEXT");
		sbOut.append(", "+C_DURATION+" INTEGER");
		sbOut.append(", "+C_CREATION_DURATION+" INTEGER");
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
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
			}
		}
		final DbHelper dbHelper;
		public DbCaptured(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration) {
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
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearCapturedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public String getSerializedCaptured() {
			List<String[]> capturedList = getAllCaptured();
			String[] capturedArray = new String[capturedList.size()];
			for (int i = 0; i < capturedList.size(); i++) {
				capturedArray[i] = TextUtils.join("|", capturedList.get(i));
			}
			return (capturedList.size() > 0) ? TextUtils.join("$", capturedArray) : "";
		}
	}
	public final DbCaptured dbCaptured;
	
	
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
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); }
			}
		}
		final DbHelper dbHelper;
		public DbEncoded(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String value, String format, String digest, int samplerate, int bitrate, String codec, long duration, long creation_duration) {
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
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllEncoded() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); } finally { db.close(); }
			return list;
		}
		public String[] getLatestRow() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] row = new String[] {null,null,null};
			try { 
				Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_TIMESTAMP+" DESC", "1");
				if (cursor.getCount() > 0) {
					try {
						if (cursor.moveToFirst()) { do { row = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8) };
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC); } finally { db.close(); }
			return row;
		}
		public void clearEncodedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public void deleteSingleEncoded(String timestamp) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_TIMESTAMP+"='"+timestamp+"'");
			} finally { db.close(); }
		}
		
	}
	public final DbEncoded dbEncoded;
	
}
