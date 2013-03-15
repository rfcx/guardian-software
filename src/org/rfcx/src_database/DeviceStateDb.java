package org.rfcx.src_database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.src_util.DateTimeUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DeviceStateDb {

	public DeviceStateDb(Context context) {
		this.dbBattery = new DbBattery(context);
		this.dbCpu = new DbCpu(context);
		this.dbLight = new DbLight(context);
	}
	
	private static final String TAG = DeviceStateDb.class.getSimpleName();
	static final int VERSION = 1;
	static final String DATABASE = "device";
	static final String C_CREATED_AT = "created_at";
	static final String C_VALUE = "value";
	private static final String[] STATS = { "COUNT("+C_VALUE+")", "AVG("+C_VALUE+")" };
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_VALUE };
	static final String CREATE_CLMNS = "(" + C_CREATED_AT + " DATETIME, " + C_VALUE + " INT " + ")";

	// for saving battery charge values
	public class DbBattery {
		private String TABLE = "battery";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { try { db.execSQL("CREATE TABLE " + TABLE + CREATE_CLMNS); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbBattery(Context context) { this.dbHelper = new DbHelper(context); }
		
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} catch (Exception e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
		public String[] getLast() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
				try { return cursor.moveToNext() ? new String[] { cursor.getString(0), cursor.getString(1) } : null;
				} finally { cursor.close(); }
			} finally { db.close(); }
		}
		public List<String[]> getStats() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null);
				if (cursor.getCount() > 0) { cursor.moveToFirst();
					try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
					} while (cursor.moveToNext()); } finally { cursor.close(); } }
			} finally { db.close(); }
			return list;
		}
		public List<String[]> getStatsSince(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, C_CREATED_AT+">=?", new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, C_CREATED_AT+" ASC", null); cursor.moveToFirst();
				try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
				} while (cursor.moveToNext()); } finally { cursor.close(); }
			} finally { db.close(); }
			return list;
		}
		public void clearStatsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} catch (SQLException e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
	}
	public final DbBattery dbBattery;
	
	// for saving CPU average usage values
	public class DbCpu {
		private String TABLE = "cpu";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { try { db.execSQL("CREATE TABLE " + TABLE + CREATE_CLMNS); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbCpu(Context context) { this.dbHelper = new DbHelper(context); }
		
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} catch (Exception e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
		public String[] getLast() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
				try { return cursor.moveToNext() ? new String[] { cursor.getString(0), cursor.getString(1) } : null;
				} finally { cursor.close(); }
			} finally { db.close(); }
		}
		public List<String[]> getStats() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null);
				if (cursor.getCount() > 0) { cursor.moveToFirst();
					try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
					} while (cursor.moveToNext()); } finally { cursor.close(); } }
			} finally { db.close(); }
			return list;
		}
		public List<String[]> getStatsSince(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, C_CREATED_AT+">=?", new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, C_CREATED_AT+" ASC", null); cursor.moveToFirst();
				try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
				} while (cursor.moveToNext()); } finally { cursor.close(); }
			} finally { db.close(); }
			return list;
		}
		public void clearStatsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} catch (SQLException e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
	}
	public final DbCpu dbCpu;
	
	// for saving light sensor values
	public class DbLight {
		private String TABLE = "light";
		class DbHelper extends SQLiteOpenHelper {
			public DbHelper(Context context) { super(context, DATABASE+"-"+TABLE+".db", null, VERSION); }
			@Override
			public void onCreate(SQLiteDatabase db) { try { db.execSQL("CREATE TABLE " + TABLE + CREATE_CLMNS); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { try { db.execSQL("DROP TABLE IF EXISTS " + TABLE); onCreate(db); } catch (SQLException e) { Log.e(TAG, e.getMessage()); } }
		}
		final DbHelper dbHelper;
		public void close() { this.dbHelper.close(); }
		
		public DbLight(Context context) { this.dbHelper = new DbHelper(context); }
		
		public void insert(int value) {
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new DateTimeUtils()).getDateTime());
			values.put(C_VALUE, value);
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.insertWithOnConflict(TABLE, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			} catch (Exception e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
		public String[] getLast() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" DESC", "1");
				try { return cursor.moveToNext() ? new String[] { cursor.getString(0), cursor.getString(1) } : null;
				} finally { cursor.close(); }
			} finally { db.close(); }
		}
		public List<String[]> getStats() {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null);
				if (cursor.getCount() > 0) { cursor.moveToFirst();
					try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
					} while (cursor.moveToNext()); } finally { cursor.close(); } }
			} finally { db.close(); }
			return list;
		}
//		public String[] getStatsAverage() {
//			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
//			String valueDate = (new DateTimeUtils()).getDateTime(); int valueSum = 0; int valueCount = 1;
//			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, null, null, null, null, C_CREATED_AT+" ASC", null); cursor.moveToFirst();
//				try { do { valueDate = cursor.getString(0); valueSum = valueSum + cursor.getInt(1); valueCount++;
//				} while (cursor.moveToNext()); } finally { valueCount = cursor.getCount(); cursor.close(); }
//			} finally { db.close(); }
//			return new String[] { valueDate, "" + Math.round(valueSum/valueCount) };
//		}
		public List<String[]> getStatsSince(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			ArrayList<String[]> list = new ArrayList<String[]>();
			try { Cursor cursor = db.query(TABLE, ALL_COLUMNS, C_CREATED_AT+">=?", new String[] { (new DateTimeUtils()).getDateTime(date) }, null, null, C_CREATED_AT+" ASC", null); cursor.moveToFirst();
				try { do { list.add(new String[] { cursor.getString(0), cursor.getString(1) });
				} while (cursor.moveToNext()); } finally { cursor.close(); }
			} finally { db.close(); }
			return list;
		}
		public void clearStatsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_CREATED_AT+"<='"+(new DateTimeUtils()).getDateTime(date)+"'");
			} catch (SQLException e) { Log.e(TAG, e.getMessage());
			} finally { db.close(); }
		}
	}
	public final DbLight dbLight;
}
