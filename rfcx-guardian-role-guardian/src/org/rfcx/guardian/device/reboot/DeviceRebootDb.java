package org.rfcx.guardian.device.reboot;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DeviceRebootDb {
	
	public DeviceRebootDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbReboot = new DbReboot(context);
	}

	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+DeviceRebootDb.class.getSimpleName();
	private int VERSION = 1;
	static final String DATABASE = "reboot";
	static final String C_CREATED_AT = "created_at";
	static final String C_REBOOTED_AT = "rebooted_at";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_REBOOTED_AT };
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
			sbOut.append("CREATE TABLE ").append(tableName)
				.append("(").append(C_CREATED_AT).append(" INTEGER")
				.append(", ").append(C_REBOOTED_AT).append(" INTEGER")
				.append(")");
		return sbOut.toString();
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
		public void insert(long rebootedAt) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_REBOOTED_AT, rebootedAt);
			
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		
		private List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			return DbUtils.getRows(db, TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
	}
	public final DbReboot dbReboot;
	

	
}
