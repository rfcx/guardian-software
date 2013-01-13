package org.rfcx.rfcx_src_android;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TransmitDbHelper {

	static final String TAG = "TransmitDbHelper";
	
	static final String DATABASE = "rfcx-src-transmit.db";
	static final int VERSION = 1;
	static final String TABLE = "transmit";
	static final String C_CREATED_AT = "created_at";
	static final String C_SENT_AT = "sent_at";
	static final String C_BODY = "body";
	
	class DbHelper extends SQLiteOpenHelper {
		
		public DbHelper(Context context) {
			super(context, DATABASE, null, VERSION);
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			String sql = "CREATE TABLE " + TABLE + " ("
					+ C_CREATED_AT + " DATETIME, "
					+ C_SENT_AT + " DATETIME, "
					+ C_BODY + " TEXT "
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

	public TransmitDbHelper(Context context) {
		this.dbHelper = new DbHelper(context);
		Log.i(TAG, "Initialized data");
	}
	
	public void close() {
		this.dbHelper.close();
	}
	
	public void insertOrIgnore(ContentValues values) {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		Date dateTime = new Date();
		values.put(TransmitDbHelper.C_CREATED_AT, dateFormat.format(dateTime));
		
		Log.d(TAG, "insertOrIgnore() on " + values);
		
		SQLiteDatabase db = this.dbHelper.getWritableDatabase();
		try {
			db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		} finally {
			db.close();
		}
	}
	
}
