package org.rfcx.rfcx_src_android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AudioCaptureDbHelper {

	static final String TAG = "AudioCaptureDbHelper";
	
	private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.US);
	
	static final String DATABASE = "rfcx-src-audio.db";
	static final int VERSION = 1;
	static final String TABLE = "audio";
	static final String C_CREATED_AT = "created_at";
	static final String C_SPECTRUM = "body";
	
	class DbHelper extends SQLiteOpenHelper {
		
		public DbHelper(Context context) {
			super(context, DATABASE, null, VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			String sql = "CREATE TABLE " + TABLE + " ("
					+ C_CREATED_AT + " DATETIME, "
					+ C_SPECTRUM + " TEXT "
					+ ")";
			db.execSQL(sql);
			Log.d(TAG, "onCreated() SQL: " + sql);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			Log.d(TAG, "onUpgrade()");
			onCreate(db);
		}
		
	}
	
	final DbHelper dbHelper;

	public AudioCaptureDbHelper(Context context) {
		this.dbHelper = new DbHelper(context);
		Log.i(TAG, "Initialized data");
	}
	
	public void close() {
		this.dbHelper.close();
	}
	
	public void insertOrIgnore(ContentValues values) {
		
		Date dateTime = new Date();
		values.put(AudioCaptureDbHelper.C_CREATED_AT, dateFormat.format(dateTime));
		
		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		try {
			db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		} finally {
			db.close();
		}
	}
}
