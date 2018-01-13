package guardian.device.sentinel;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import guardian.RfcxGuardian;

public class SentinelPowerDb {
	
	public SentinelPowerDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbExternalPowerBattery = new DbExternalPowerBattery(context);
		this.dbExternalPowerSolar = new DbExternalPowerSolar(context);
		this.dbExternalPowerLoad = new DbExternalPowerLoad(context);
		this.dbExternalPowerController = new DbExternalPowerController(context);
	}
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SentinelPowerDb.class);
	private int VERSION = 1;
	static final String DATABASE = "sentinel-power";
	static final String C_MEASURED_AT = "measured_at";
	static final String C_VALUE_1 = "value_1";
	static final String C_VALUE_2 = "value_2";
	static final String C_VALUE_3 = "value_3";
	static final String C_VALUE_4 = "value_4";
	private static final String[] ALL_COLUMNS = new String[] { C_MEASURED_AT, C_VALUE_1, C_VALUE_2, C_VALUE_3, C_VALUE_4 };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_MEASURED_AT).append(" INTEGER")
			.append(", ").append(C_VALUE_1).append(" TEXT")
			.append(", ").append(C_VALUE_2).append(" TEXT")
			.append(", ").append(C_VALUE_3).append(" TEXT")
			.append(", ").append(C_VALUE_4).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbExternalPowerBattery {
		private String TABLE = "battery";
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
		
		public DbExternalPowerBattery(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
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
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}

	}
	public final DbExternalPowerBattery dbExternalPowerBattery;
	
	
	
	public class DbExternalPowerSolar {
		private String TABLE = "solar";
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
		
		public DbExternalPowerSolar(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
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
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}

	}
	public final DbExternalPowerSolar dbExternalPowerSolar;
	
	
	
	public class DbExternalPowerLoad {
		private String TABLE = "load";
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
		
		public DbExternalPowerLoad(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
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
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}

	}
	public final DbExternalPowerLoad dbExternalPowerLoad;
	
	
	
	public class DbExternalPowerController {
		private String TABLE = "controller";
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
		
		public DbExternalPowerController(Context context) {
			this.dbHelper = new DbHelper(context);
		}
		
		public void close() {
			this.dbHelper.close();
		}
		
		public void insert(Date measured_at, String value_1, String value_2, String value_3, String value_4) {
			ContentValues values = new ContentValues();
			values.put(C_MEASURED_AT, measured_at.getTime());
			values.put(C_VALUE_1, value_1.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_2, value_2.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_3, value_3.replaceAll("\\*", "-").replaceAll("\\|","-"));
			values.put(C_VALUE_4, value_4.replaceAll("\\*", "-").replaceAll("\\|","-"));
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
		
		public void clearRowsBefore(Date date) {
			SQLiteDatabase db = this.dbHelper.getWritableDatabase();
			try { db.execSQL("DELETE FROM "+TABLE+" WHERE "+C_MEASURED_AT+"<="+date.getTime());
			} finally { db.close(); }
		}
		
		public String getConcatRows() {
			return DbUtils.getConcatRows(getAllRows());
		}

	}
	public final DbExternalPowerController dbExternalPowerController;
	

	
}
