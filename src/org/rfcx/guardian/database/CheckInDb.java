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
import android.text.TextUtils;
import android.util.Log;

public class CheckInDb {

	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public CheckInDb(Context context) {
		this.dbQueued = new DbQueued(context);
	}
	
	private static final String TAG = "RfcxGuardian-"+CheckInDb.class.getSimpleName();
	public DateTimeUtils dateTimeUtils = new DateTimeUtils();
	static final int VERSION = 1;
	static final String DATABASE = "checkin";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_AUDIO = "audio";
	static final String C_SCREENSHOT = "screenshot";
	static final String C_JSON = "json";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_AUDIO, C_SCREENSHOT, C_JSON };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(").append(C_CREATED_AT).append(" DATETIME");
		sbOut.append(", "+C_TIMESTAMP+" TEXT");
		sbOut.append(", "+C_AUDIO+" TEXT");
		sbOut.append(", "+C_SCREENSHOT+" TEXT");
		sbOut.append(", "+C_JSON+" TEXT");
		return sbOut.append(")").toString();
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
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db);
				} catch (SQLException e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); }
			}
		}
		final DbHelper dbHelper;
		public DbQueued(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(String value, String audio, String screenshot, String json) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			values.put(C_TIMESTAMP, value);
			values.put(C_AUDIO, audio);
			values.put(C_SCREENSHOT, screenshot);
			values.put(C_JSON, json);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllQueued() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearQueuedBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		
		public String getSerializedQueued() {
			List<String[]> queuedList = getAllQueued();
			String[] queuedArray = new String[queuedList.size()];
			for (int i = 0; i < queuedList.size(); i++) {
				queuedArray[i] = TextUtils.join("|", queuedList.get(i));
			}
			return (queuedList.size() > 0) ? TextUtils.join("$", queuedArray) : "";
		}
	}
	public final DbQueued dbQueued;
	
}
