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
import android.util.Log;

public class DeviceStateDb {
	
	private static final String NULL_EXC = "Exception thrown, but exception itself is null.";
	
	public DeviceStateDb(Context context) {
		this.dbBattery = new DbBattery(context);
		this.dbCpu = new DbCpu(context);
		this.dbLight = new DbLight(context);
		this.dbCpuClock = new DbCpuClock(context);
		this.dbBatteryTemperature = new DbBatteryTemperature(context);
	}
	
	private static final String TAG = DeviceStateDb.class.getSimpleName();
	public DateTimeUtils dateTimeUtils = new DateTimeUtils();
	static final int VERSION = 1;
	static final String DATABASE = "device";
	static final String C_CREATED_AT = "created_at";
	static final String C_VALUE = "value";
	private static final String[] STATS_COLUMNS = { "COUNT("+C_VALUE+")", "ROUND(AVG("+C_VALUE+"))", "MAX("+C_CREATED_AT+")", "MIN("+C_CREATED_AT+")", "GROUP_CONCAT("+C_VALUE+")" };
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_VALUE };
	static final String CREATE_CLMNS = "(" + C_CREATED_AT + " DATETIME, " + C_VALUE + " INT " + ")";
	
	// Prototype DbHelper methods
	private void _onCreate(SQLiteDatabase db, String table) { try { db.execSQL("CREATE TABLE " + table + CREATE_CLMNS); } catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } }
	private void _onUpgrade(SQLiteDatabase db, String table, int oldVersion, int newVersion) { try { db.execSQL("DROP TABLE IF EXISTS " + table); _onCreate(db, table); } catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } }
	
	private String[] _getLast(SQLiteDatabase db, String table) {
		String[] last = new String[] {};
		try { Cursor cursor = db.query(table, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
			try { last = cursor.moveToNext() ? new String[] { cursor.getString(0), cursor.getString(1) } : new String[] {};
			} finally { cursor.close(); }
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
		return last;
	}
	
	private List<String[]> _getStats(SQLiteDatabase db, String table) {
		ArrayList<String[]> list = new ArrayList<String[]>();
		try { Cursor cursor = db.query(table, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null);
			if (cursor.getCount() > 0) {
				try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
				} while (cursor.moveToNext()); } } finally { cursor.close(); } }
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
		return list;
	}
	private String[] _getStatsSummary(SQLiteDatabase db, String table) {
		String[] stats = new String[] { null, null, null, null, null };
		try { Cursor cursor = db.query(table, STATS_COLUMNS, null, null, null, null, null, null);
			try { if (cursor.moveToFirst()) { do { for (int i = 0; i < stats.length; i++) { stats[i] = cursor.getString(i); }
			} while (cursor.moveToNext()); } } finally { cursor.close(); }
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
		return stats;
	}
	private List<String[]> _getStatsSince(SQLiteDatabase db, String table, Date date) {
		ArrayList<String[]> list = new ArrayList<String[]>();
		try { Cursor cursor = db.query(table, ALL_COLUMNS, C_CREATED_AT+">=?", new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, C_CREATED_AT+" ASC", null);
		try { if (cursor.moveToFirst()) { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
			} while (cursor.moveToNext()); } } finally { cursor.close(); }
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
		return list;
	}
	private void _clearStatsBefore(SQLiteDatabase db, String table, Date date) {
		try { db.execSQL("DELETE FROM "+table+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
		} catch (SQLException e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC);
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
	}
	private void _insert(SQLiteDatabase db, String table, int value) {
		ContentValues values = new ContentValues();
		values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
		values.put(C_VALUE, value);
		try { db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
		} catch (Exception e) { Log.e(TAG,(e!=null) ? e.getMessage() : NULL_EXC); } finally { db.close(); }
	}
	
	// for saving battery charge values
	public class DbBattery {
		private String TABLE = "battery";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { _onCreate(db, TABLE); }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { _onUpgrade(db, TABLE, oldVersion, newVersion); }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbBattery(Context context) { this.dbHelper = new DbHelper(context); }
		
		public String[] getLast() { return _getLast(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStats() { return _getStats(this.dbHelper.getWritableDatabase(), TABLE); }
		public String[] getStatsSummary() { return _getStatsSummary(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStatsSince(Date date) { return _getStatsSince(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void clearStatsBefore(Date date) { _clearStatsBefore(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void insert(int value) { _insert(this.dbHelper.getWritableDatabase(), TABLE, value); }
	}
	public final DbBattery dbBattery;
	
	// for saving CPU average usage values
	public class DbCpu {
		private String TABLE = "cpu";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { _onCreate(db, TABLE); }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { _onUpgrade(db, TABLE, oldVersion, newVersion); }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbCpu(Context context) { this.dbHelper = new DbHelper(context); }
		
		public String[] getLast() { return _getLast(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStats() { return _getStats(this.dbHelper.getWritableDatabase(), TABLE); }
		public String[] getStatsSummary() { return _getStatsSummary(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStatsSince(Date date) { return _getStatsSince(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void clearStatsBefore(Date date) { _clearStatsBefore(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void insert(int value) { _insert(this.dbHelper.getWritableDatabase(), TABLE, value); }
	}
	public final DbCpu dbCpu;
	
	// for saving light sensor values
	public class DbLight {
		private String TABLE = "light";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { _onCreate(db, TABLE); }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { _onUpgrade(db, TABLE, oldVersion, newVersion); }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbLight(Context context) { this.dbHelper = new DbHelper(context); }
		
		public String[] getLast() { return _getLast(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStats() { return _getStats(this.dbHelper.getWritableDatabase(), TABLE); }
		public String[] getStatsSummary() { return _getStatsSummary(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStatsSince(Date date) { return _getStatsSince(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void clearStatsBefore(Date date) { _clearStatsBefore(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void insert(int value) { _insert(this.dbHelper.getWritableDatabase(), TABLE, value); }
	}
	public final DbLight dbLight;
	
	// CPU Clockspeed
	public class DbCpuClock {
		private String TABLE = "cpuclock";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { _onCreate(db, TABLE); }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { _onUpgrade(db, TABLE, oldVersion, newVersion); }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbCpuClock(Context context) { this.dbHelper = new DbHelper(context); }
		
		public String[] getLast() { return _getLast(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStats() { return _getStats(this.dbHelper.getWritableDatabase(), TABLE); }
		public String[] getStatsSummary() { return _getStatsSummary(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStatsSince(Date date) { return _getStatsSince(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void clearStatsBefore(Date date) { _clearStatsBefore(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void insert(int value) { _insert(this.dbHelper.getWritableDatabase(), TABLE, value); }
	}
	public final DbCpuClock dbCpuClock;
	
	// Battery Temperature
	public class DbBatteryTemperature {
		private String TABLE = "batterytemperature";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { _onCreate(db, TABLE); }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { _onUpgrade(db, TABLE, oldVersion, newVersion); }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbBatteryTemperature(Context context) { this.dbHelper = new DbHelper(context); }
		
		public String[] getLast() { return _getLast(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStats() { return _getStats(this.dbHelper.getWritableDatabase(), TABLE); }
		public String[] getStatsSummary() { return _getStatsSummary(this.dbHelper.getWritableDatabase(), TABLE); }
		public List<String[]> getStatsSince(Date date) { return _getStatsSince(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void clearStatsBefore(Date date) { _clearStatsBefore(this.dbHelper.getWritableDatabase(), TABLE, date); }
		public void insert(int value) { _insert(this.dbHelper.getWritableDatabase(), TABLE, value); }
	}
	public final DbBatteryTemperature dbBatteryTemperature;
}
