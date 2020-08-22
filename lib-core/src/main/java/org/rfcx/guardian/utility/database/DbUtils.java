package org.rfcx.guardian.utility.database;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class DbUtils {
	
	private static final String logTag = RfcxLog.generateLogTag("Utils", "DbUtils");

	private static final int DEFAULT_ROWOFFSET = 0;
	private static final int DEFAULT_ROWLIMIT = 1000;
	private static final String DEFAULT_ORDER = "DESC";
	
	class DbHelper extends SQLiteOpenHelper {
		
		String TABLE;
		String CREATE_COLUMN_QUERY;
		boolean DROP_TABLE_ON_UPGRADE = true;

		public DbHelper(Context context, String database, String table, int version, String createColumnQuery) {
			super(context, database+"-"+table+".db", null, version);
			this.TABLE = table;
			this.CREATE_COLUMN_QUERY = createColumnQuery;
		}

		public DbHelper(Context context, String database, String table, int version, String createColumnQuery, boolean suppressDropTableOnUpgrade) {
			super(context, database+"-"+table+".db", null, version);
			this.TABLE = table;
			this.CREATE_COLUMN_QUERY = createColumnQuery;
			this.DROP_TABLE_ON_UPGRADE = !suppressDropTableOnUpgrade;
		}
	
		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				db.execSQL(this.CREATE_COLUMN_QUERY);
			} catch (SQLException e) { 
				RfcxLog.logExc(logTag, e);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (this.DROP_TABLE_ON_UPGRADE) {
				try {
					db.execSQL("DROP TABLE IF EXISTS " + this.TABLE);
					onCreate(db);
				} catch (SQLException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
		}
	}
	
	public DbHelper dbHelper;
	private SQLiteDatabase sqlLiteDb = null;


	public DbUtils(Context context, String database, String table, int version, String createColumnQuery) {
		this.dbHelper = new DbHelper(context, database, table, version, createColumnQuery);
	}

	public DbUtils(Context context, String database, String table, int version, String createColumnQuery, boolean suppressDropTableOnUpgrade) {
		this.dbHelper = new DbHelper(context, database, table, version, createColumnQuery, suppressDropTableOnUpgrade);
	}
	
	private SQLiteDatabase openDb() {
		if (		(this.sqlLiteDb == null)
			|| 	!(this.sqlLiteDb.isOpen())
			) {
			try {
				this.sqlLiteDb = this.dbHelper.getWritableDatabase();
		//		this.sqlLiteDb.execSQL("PRAGMA read_uncommitted = true;");
		//		this.sqlLiteDb.execSQL("PRAGMA synchronous = OFF;");
			} catch (Exception e) { 
				RfcxLog.logExc(logTag, e);
			}	
		}
		return this.sqlLiteDb;
	}
	
	private void closeDb() {
//		try {
//			if (this.sqlLiteDb != null) { this.sqlLiteDb.close(); }
//		} catch (Exception e) { 
//			RfcxLog.logExc(logTag, e);
//		}
	}
	
	public int insertRow(String table, ContentValues values) {
		int rowCount = 0;
		SQLiteDatabase db = openDb();
		try {
			db.insertWithOnConflict(table, null, values, SQLiteDatabase.CONFLICT_IGNORE);
			rowCount = getCount(db, table, null, null);
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
		return rowCount;
	}

	public void updateFirstRow(String table, ContentValues values) {
		SQLiteDatabase db = openDb();
		try {
			String where = "rowid=(SELECT MIN(rowid) FROM " + table + ")";
			db.update(table, values, where, null);
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}
	
	public static List<String[]> getRows(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset, int rowLimit) {
		
		ArrayList<String[]> rowList = new ArrayList<String[]>();
		try {
			if (orderBy != null) { orderBy = (orderBy.substring(orderBy.length()-4).equalsIgnoreCase(" ASC") || orderBy.substring(orderBy.length()-5).equalsIgnoreCase(" DESC")) ? orderBy : (orderBy+" "+DEFAULT_ORDER); }
			Cursor cursor = db.query(tableName, tableColumns, selection, selectionArgs, null, null, orderBy, ""+(rowOffset+rowLimit));
			if ((cursor != null) && (cursor.getCount() > rowOffset) && cursor.moveToPosition(rowOffset)) {
				do { 
					rowList.add( cursorToStringArray( cursor, tableColumns.length ) );
				} while (cursor.moveToNext());
			}
			cursor.close();
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		}
		return rowList;
	}
	
	public List<String[]> getRows(String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset, int rowLimit) {
		SQLiteDatabase db = openDb();
		List<String[]> rows = getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy, rowOffset, rowLimit);
		closeDb();
		return rows;
	}
	
	public static List<String[]> getRows(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy) {
		return getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy, DEFAULT_ROWOFFSET, DEFAULT_ROWLIMIT);
	}
	
	public List<String[]> getRows(String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy) {
		SQLiteDatabase db = openDb();
		List<String[]> rows = getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy);
		closeDb();
		return rows;
	}
	
	public static String[] getSingleRow(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset) {
		String[] row = placeHolderStringArray(tableColumns.length);
		try { 
			for (String[] singleRow : getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy, rowOffset, 1)) {
				row = singleRow;
				break;
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		}
		return row;
	}
	
	public String[] getSingleRow(String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset) {
		SQLiteDatabase db = openDb();
		String[] row = getSingleRow(db, tableName, tableColumns, selection, selectionArgs, orderBy, rowOffset);
		closeDb();
		return row;
	}

	private static JSONArray getRowsAsJsonArray(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset, int rowLimit) {
		JSONArray jsonArray = new JSONArray();
		try {
			for (String[] row : getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy, rowOffset, rowLimit)) {
				JSONObject jsonRow = new JSONObject();
				for (int i = 0; i < tableColumns.length; i++) {
					jsonRow.put(tableColumns[i], row[i]);
				}
				jsonArray.put(jsonRow);
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		}
		return jsonArray;
	}
	
	public JSONArray getRowsAsJsonArray(String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset, int rowLimit) {
		SQLiteDatabase db = openDb();
		JSONArray jsonRows = getRowsAsJsonArray(db, tableName, tableColumns, selection, selectionArgs, orderBy, rowOffset, rowLimit);
		closeDb();
		return jsonRows;
	}
	
	private static JSONArray getRowsAsJsonArray(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy) {
		return getRowsAsJsonArray(db, tableName, tableColumns, selection, selectionArgs, orderBy, DEFAULT_ROWOFFSET, DEFAULT_ROWLIMIT);
	}
	
	public JSONArray getRowsAsJsonArray(String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy) {
		SQLiteDatabase db = openDb();
		JSONArray jsonRows = getRowsAsJsonArray(db, tableName, tableColumns, selection, selectionArgs, orderBy);
		closeDb();
		return jsonRows;
	}
	
	public void deleteRowsOlderThan(String tableName, String dateColumn, Date olderThanDate) {
		SQLiteDatabase db = openDb();
		try {
			db.execSQL("DELETE FROM "+tableName+" WHERE "+dateColumn+"<="+olderThanDate.getTime());
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e); 
		} finally {
			closeDb();
		}
	}
	
	public void deleteRowsWithinQueryByTimestamp(String tableName, String timestampColumn, String timestampValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { timestampColumn }, "substr("+timestampColumn+",1,"+timestampValue.length()+") = ?", new String[] { timestampValue }, null) ) {
				db.execSQL("DELETE FROM "+tableName+" WHERE "+ timestampColumn +" = '"+ dbRow[0] +"'");
			
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e); 
		} finally {
			closeDb();
		}
	}
	
	public void adjustNumericColumnValuesWithinQueryByTimestamp(String adjustmentAmount, String tableName, String numericColumnName, String timestampColumn, String timestampValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { timestampColumn, numericColumnName }, "substr("+timestampColumn+",1,"+timestampValue.length()+") = ?", new String[] { timestampValue }, null) ) {
				db.execSQL("UPDATE "+tableName+" SET "+numericColumnName+"=cast("+numericColumnName+" as INT)"+adjustmentAmount+" WHERE "+ timestampColumn +" = '"+ dbRow[0] +"'");
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e); 
		} finally {
			closeDb();
		}
	}
	
	public void setDatetimeColumnValuesWithinQueryByTimestamp(String tableName, String datetimeColumnName, long datetimeColumnValue, String timestampColumn, String timestampValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { timestampColumn, datetimeColumnName }, "substr("+timestampColumn+",1,"+timestampValue.length()+") = ?", new String[] { timestampValue }, null) ) {
				db.execSQL("UPDATE "+tableName+" SET "+datetimeColumnName+"="+datetimeColumnValue+" WHERE "+ timestampColumn +" = '"+ dbRow[0] +"'");
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e); 
		} finally {
			closeDb();
		}
	}

	public static int getSumOfColumn(SQLiteDatabase db, String tableName, String sumColumn, String selection, String[] selectionArgs) {
		int sum = 0;
		try {
			for (String[] singleRow : getRows(db, tableName, new String[] { "SUM("+sumColumn+")" }, selection, selectionArgs, null, 0, 1)) {
				if (singleRow[0] != null) {
					sum = Integer.parseInt(singleRow[0]);
					break;
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return sum;
	}

	public int getSumOfColumn(String tableName, String sumColumn, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		int sum = getSumOfColumn(db, tableName, sumColumn, selection, selectionArgs);
		closeDb();
		return sum;
	}

	public static int getCount(SQLiteDatabase db, String tableName, String selection, String[] selectionArgs) {
		int count = 0;
		try { 
			for (String[] singleRow : getRows(db, tableName, new String[] { "COUNT(*)" }, selection, selectionArgs, null, 0, 1)) {
				count = Integer.parseInt(singleRow[0]);
				break;
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		}
		return count;
	}
	
	public int getCount(String tableName, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		int count = getCount(db, tableName, selection, selectionArgs);
		closeDb();
		return count;
	}
	
	public void deleteAllRows(String tableName) {
		SQLiteDatabase db = openDb();
		try {
			db.execSQL("DELETE FROM "+tableName);
			Log.v(logTag, "All rows deleted from '"+tableName+"'");
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e); 
		} finally {
			closeDb();
		}
	}
	
	
	public static String getConcatRows(List<String[]> getRowsOutput) {
		String concatRows = null;
		ArrayList<String> rowList = new ArrayList<String>();
		try {
			for (String[] row : getRowsOutput) {
				rowList.add(TextUtils.join("*", row));
			}
			concatRows = (rowList.size() > 0) ? TextUtils.join("|", rowList) : null;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return concatRows;
	}
	
	public static String getConcatRowsWithLabelPrepended(String labelToPrepend, List<String[]> getRowsOutput) {
		String concatRows = null;
		ArrayList<String> rowList = new ArrayList<String>();
		try {
			for (String[] row : getRowsOutput) {
				rowList.add(labelToPrepend+"*"+TextUtils.join("*", row));
			}
			concatRows = (rowList.size() > 0) ? TextUtils.join("|", rowList) : null;
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return concatRows;
	}
	
	private static String[] cursorToStringArray(Cursor cursor, int columnCount) {
		String[] rtrnStr = new String[] {  };
		if (columnCount == 1) {
			rtrnStr = new String[] { cursor.getString(0) };
		} else if (columnCount == 2) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1) };
		} else if (columnCount == 3) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2) };
		} else if (columnCount == 4) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3) };
		} else if (columnCount == 5) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4) };
		} else if (columnCount == 6) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5) };
		} else if (columnCount == 7) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6) };
		} else if (columnCount == 8) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7) };
		} else if (columnCount == 9) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8) };
		} else if (columnCount == 10) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9) };
		} else if (columnCount == 11) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10) };
		} else if (columnCount == 12) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11) };
		} else if (columnCount == 13) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11), cursor.getString(12) };
		} else if (columnCount == 14) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11), cursor.getString(12), cursor.getString(13) };
		} else if (columnCount == 15) {
			rtrnStr = new String[] { cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3), cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getString(11), cursor.getString(12), cursor.getString(13), cursor.getString(14) };
		}
		return rtrnStr;
	}
	
	public static String[] placeHolderStringArray(int columnCount) {
		String[] rtrnStr = new String[] {  };
		if (columnCount == 1) {
			rtrnStr = new String[] { null };
		} else if (columnCount == 2) {
			rtrnStr = new String[] { null, null };
		} else if (columnCount == 3) {
			rtrnStr = new String[] { null, null, null };
		} else if (columnCount == 4) {
			rtrnStr = new String[] { null, null, null, null };
		} else if (columnCount == 5) {
			rtrnStr = new String[] { null, null, null, null, null };
		} else if (columnCount == 6) {
			rtrnStr = new String[] { null, null, null, null, null, null };
		} else if (columnCount == 7) {
			rtrnStr = new String[] { null, null, null, null, null, null, null };
		} else if (columnCount == 8) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null };
		} else if (columnCount == 9) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 10) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 11) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 12) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 13) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 14) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null };
		} else if (columnCount == 15) {
			rtrnStr = new String[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null };
		}
		return rtrnStr;
	}
	
}
