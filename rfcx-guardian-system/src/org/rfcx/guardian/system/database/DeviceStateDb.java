package org.rfcx.guardian.system.database;

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

public class DeviceStateDb {

	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public DeviceStateDb(Context context, int appVersion) {
		this.VERSION = appVersion;
		this.dbCPU = new DbCPU(context);
		this.dbBattery = new DbBattery(context);
		this.dbPower = new DbPower(context);
		this.dbNetwork = new DbNetwork(context);
		this.dbOffline = new DbOffline(context);
		this.dbLightMeter = new DbLightMeter(context);
	}
	
	private static final String TAG = "Rfcx-System-"+DeviceStateDb.class.getSimpleName();
	public DateTimeUtils dateTimeUtils = new DateTimeUtils();
	private int VERSION = 1;
	static final String DATABASE = "device";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VALUE_1 = "value_1";
	static final String C_VALUE_2 = "value_2";
	private static final String[] CONCAT_ROWS = { "COUNT("+C_MEASURED_AT+")", "GROUP_CONCAT( "+C_MEASURED_AT+" || '*' || "+C_VALUE_1+" || '*' || "+C_VALUE_2+", '|')" };
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VALUE_1, C_VALUE_2 };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName).append("(").append(C_MEASURED_AT).append(" DATETIME");
		sbOut.append(", "+C_VALUE_1+" TEXT");
		sbOut.append(", "+C_VALUE_2+" TEXT");
		return sbOut.append(")").toString();
	}
	
	public class DbCPU {
		private String TABLE = "cpu";
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
		public DbCPU(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, int cpu_percent, int cpu_clock) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, cpu_percent);
			values.put(C_VALUE_2, cpu_clock);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbCPU dbCPU;
	
	
	public class DbBattery {
		private String TABLE = "battery";
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
		public DbBattery(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, int battery_percent, int battery_temperature) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, battery_percent);
			values.put(C_VALUE_2, battery_temperature);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbBattery dbBattery;
	
	public class DbPower {
		private String TABLE = "power";
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
		public DbPower(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, boolean is_powered, boolean is_charged) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, is_powered);
			values.put(C_VALUE_2, is_charged);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbPower dbPower;
	
	public class DbNetwork {
		private String TABLE = "network";
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
		public DbNetwork(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, int signal_strength, String carrier_name) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, signal_strength);
			values.put(C_VALUE_2, carrier_name);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbNetwork dbNetwork;

	public class DbOffline {
		private String TABLE = "offline";
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
		public DbOffline(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, long offline_period, String carrier_name) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, offline_period);
			values.put(C_VALUE_2, carrier_name);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbOffline dbOffline;
	
	public class DbLightMeter {
		private String TABLE = "lightmeter";
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
		public DbLightMeter(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		public void close() {
			this.dbHelper.close();
		}
		public void insert(Date measured_at, long luminosity, String value_2) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, dateTimeUtils.getDateTime(measured_at));
			values.put(C_VALUE_1, luminosity);
			values.put(C_VALUE_2, value_2);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try {
				db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} finally {
				db.close();
			}
		}
		public List<String[]> getAllRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, null, null);
				if (cursor.getCount() > 0) {
					try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) });
					} while (cursor.moveToNext()); } } finally { cursor.close(); } }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return list;
		}
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} finally { db.close(); }
		}
		public String[] getConcatRows() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			String[] stats = new String[] { null, null };
			try { Cursor cursor = db.query(TABLE, CONCAT_ROWS, null, null, null, null, null, null);
				try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
				} while (cursor.moveToNext()); } } finally { cursor.close(); }
			} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
			return stats;
		}
	}
	public final DbLightMeter dbLightMeter;
	
}
