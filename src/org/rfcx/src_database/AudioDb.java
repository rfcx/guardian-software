package org.rfcx.src_database;

import org.rfcx.rfcx_src_android.RfcxSource;
import org.rfcx.src_util.DateTimeUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AudioDb {

	public AudioDb(Context context) {
		this.dbSpectrum = new DbSpectrum(context);
	}
	
	private static final String TAG = AudioDb.class.getSimpleName();
	static final int VERSION = 1;
	static final String DATABASE = "audio";
	static final String C_CREATED_AT = "created_at";
	static final String C_POINTS = "points";
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(").append(C_CREATED_AT).append(" DATETIME");
		sbOut.append(", "+C_POINTS+" TEXT");
		return sbOut.append(")").toString();
	}
		
	public class DbSpectrum {
		private String TABLE = "spectrum";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) {
				super(context, DATABASE+"-"+TABLE+".db", null, VERSION);
			}
			@Override
			public void onCreate(SQLiteDatabase db) {
				db.execSQL(createColumnString(TABLE));
			}
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				db.execSQL("DROP TABLE IF EXISTS " + TABLE);
				if (RfcxSource.verboseLog()) { Log.d(TAG, "onUpgrade()"); }
				onCreate(db);
			}
		}
		final DbHelper dbHelper;
		public DbSpectrum(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(double[] spectrumValues) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			StringBuilder sbPts = (new StringBuilder()).append(spectrumValues[0]);
			for (int i = 1; i < spectrumValues.length; i++) {
				sbPts.append(",").append(spectrumValues[i]);
			}
			values.put(C_POINTS, sbPts.toString());
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
//		public String[] getLast() {
//			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
//			try { Cursor cursor = db.query(TABLE, new String[] {C_CREATED_AT, C_VALUE}, null, null, null, null, C_CREATED_AT+" DESC", "1");
//				try { return cursor.moveToNext() ? new String[] { cursor.getString(0), cursor.getString(1) } : null;
//				} finally { cursor.close(); }
//			} finally { db.close(); }
//		}
//		public int[] getStatsSince(Date date) {
//			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
//			try { Cursor cursor = db.query(TABLE, STATS, C_CREATED_AT+">=?",
//					new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, null);
//				try { return cursor.moveToNext() ? new int[] { cursor.getInt(0), cursor.getInt(1) } : null;
//				} finally { cursor.close(); }
//			} finally { db.close(); }
//		}
//		public void clearStatsBefore(Date date) {
//			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
//			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+(new DateTimeUtils()).getDateTime(date));
//			} finally { db.close(); }
//		}
	}
	public final DbSpectrum dbSpectrum;
	
}
