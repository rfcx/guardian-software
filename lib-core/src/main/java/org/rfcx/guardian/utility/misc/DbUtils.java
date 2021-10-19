package org.rfcx.guardian.utility.misc;

import java.util.ArrayList;
import java.util.Arrays;
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
	private static final int DEFAULT_ROWLIMIT = 4096;
	private static final String DEFAULT_ORDER = "DESC";
	
	class DbHelper extends SQLiteOpenHelper {

		String DATABASE;
		String TABLE;
		String CREATE_COLUMN_QUERY;
		boolean DROP_TABLE_ON_UPGRADE = true;

		public DbHelper(Context context, String database, String table, int version, String createColumnQuery, boolean dropTableOnUpgrade) {
			super(context, database+"-"+table+".db", null, version);
			this.DATABASE = database;
			this.TABLE = table;
			this.CREATE_COLUMN_QUERY = createColumnQuery;
			this.DROP_TABLE_ON_UPGRADE = dropTableOnUpgrade;
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
			String upgradeMsg = "Upgrading Database '"+this.DATABASE+"-"+this.TABLE+"' to v"+newVersion;
			if (this.DROP_TABLE_ON_UPGRADE) {
				try {
					upgradeMsg += " - Table has been dropped and contents have been erased.";
					db.execSQL("DROP TABLE IF EXISTS " + this.TABLE);
					onCreate(db);
				} catch (SQLException e) {
					RfcxLog.logExc(logTag, e);
				}
			}
			Log.v(logTag, upgradeMsg);
		}
	}
	
	public DbHelper dbHelper;
	private SQLiteDatabase sqlLiteDb = null;

	public DbUtils(Context context, String database, String table, int version, String createColumnQuery, boolean dropTableOnUpgrade) {
		this.dbHelper = new DbHelper(context, database, table, version, createColumnQuery, dropTableOnUpgrade);
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

	public static String getDbFilePath(Context context, String database, String table) {
		String filesDir = context.getFilesDir().getAbsolutePath();
		return filesDir.substring(0, filesDir.lastIndexOf("/"))+"/databases/"+database+"-"+table+".db";
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

	public void updateRowByColumn(String table, String updateColumn, String updateValue, String pointColumn, String pointValue) {
		SQLiteDatabase db = openDb();
		try {
			ContentValues cv = new ContentValues();
			cv.put(updateColumn, updateValue);
			db.update(table, cv, pointColumn + "=" + pointValue, null);
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

	public List<String[]> getRowsWithNumericColumnHigherOrLowerThan(String tableName, String[] tableColumns, String numericColumnName, long lowerOrHigherThanValue, boolean isLowerNotHigher, String orderBy, int maxRows) {
		SQLiteDatabase db = openDb();
		List<String[]> rows = getRows(db, tableName, tableColumns, numericColumnName+" "+((isLowerNotHigher) ? "<" : ">")+" ?", new String[] { ""+lowerOrHigherThanValue }, orderBy, 0, maxRows);
		closeDb();
		return rows;
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

	public void deleteRowsWithinQueryByOneColumn(String tableName, String queryColumnName, String queryColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryColumnName }, "substr("+queryColumnName+",1,"+queryColumnValue.length()+") = ?", new String[] { queryColumnValue }, null) ) {
				db.execSQL("DELETE FROM "+tableName+" WHERE "+ queryColumnName +" = '"+ dbRow[0] +"'");

			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void deleteRowsWithinQueryByTimestamp(String tableName, String timestampColumn, String timestampValue) {
		deleteRowsWithinQueryByOneColumn(tableName, timestampColumn, timestampValue);
	}

	public void deleteRowsWithinQueryByTwoColumns(String tableName, String queryFirstColumnName, String queryFirstColumnValue, String querySecondColumnName, String querySecondColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryFirstColumnName, querySecondColumnName }, "substr("+queryFirstColumnName+",1,"+queryFirstColumnValue.length()+") = ?", new String[] { queryFirstColumnValue }, null) ) {
				if (dbRow[1].equalsIgnoreCase(querySecondColumnValue)) {
					db.execSQL("DELETE FROM " + tableName + " WHERE " + queryFirstColumnName + " = '" + dbRow[0] + "' AND " + querySecondColumnName + " = '" + dbRow[1] + "'");
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void updateStringColumnValuesWithinQueryByOneColumn(String tableName, String updateColumnName, String updateColumnValue, String queryColumnName, String queryColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryColumnName, updateColumnName }, "substr("+queryColumnName+",1,"+queryColumnValue.length()+") = ?", new String[] { queryColumnValue }, null) ) {
				db.execSQL("UPDATE "+tableName+" SET "+updateColumnName+"='"+updateColumnValue+"' WHERE "+ queryColumnName +" = '"+ dbRow[0] +"'");
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void updateStringColumnValuesWithinQueryByTimestamp(String tableName, String columnName, String columnValue, String timestampColumn, String timestampValue) {
		updateStringColumnValuesWithinQueryByOneColumn(tableName, columnName, columnValue, timestampColumn, timestampValue);
	}

	public void updateStringColumnValuesWithinQueryByTwoColumns(String tableName, String updateColumnName, String updateColumnValue, String queryFirstColumnName, String queryFirstColumnValue, String querySecondColumnName, String querySecondColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryFirstColumnName, querySecondColumnName }, "substr("+queryFirstColumnName+",1,"+queryFirstColumnValue.length()+") = ?", new String[] { queryFirstColumnValue }, null) ) {
				if (dbRow[1].equalsIgnoreCase(querySecondColumnValue)) {
					db.execSQL("UPDATE " + tableName + " SET " + updateColumnName + "='" + updateColumnValue + "' WHERE " + queryFirstColumnName + " = '" + dbRow[0] + "' AND " + querySecondColumnName + " = '" + dbRow[1] + "'");
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void adjustNumericColumnValuesWithinQueryByOneColumn(String adjustmentAmount, String tableName, String numericColumnName, String queryColumnName, String queryColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryColumnName, numericColumnName }, "substr("+queryColumnName+",1,"+queryColumnValue.length()+") = ?", new String[] { queryColumnValue }, null) ) {
				db.execSQL("UPDATE "+tableName+" SET "+numericColumnName+"=cast("+numericColumnName+" as INT)"+adjustmentAmount+" WHERE "+ queryColumnName +" = '"+ dbRow[0] +"'");
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void adjustNumericColumnValuesWithinQueryByTimestamp(String adjustmentAmount, String tableName, String numericColumnName, String timestampColumn, String timestampValue) {
		adjustNumericColumnValuesWithinQueryByOneColumn(adjustmentAmount, tableName, numericColumnName, timestampColumn, timestampValue);
	}

	public void adjustNumericColumnValuesWithinQueryByTwoColumns(String adjustmentAmount, String tableName, String numericColumnName, String queryFirstColumnName, String queryFirstColumnValue, String querySecondColumnName, String querySecondColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[]{queryFirstColumnName, querySecondColumnName}, "substr(" + queryFirstColumnName + ",1," + queryFirstColumnValue.length() + ") = ?", new String[]{queryFirstColumnValue}, null)) {
				if (dbRow[1].equalsIgnoreCase(querySecondColumnValue)) {
					db.execSQL("UPDATE " + tableName + " SET " + numericColumnName + "=cast(" + numericColumnName + " as INT)" + adjustmentAmount + " WHERE " + queryFirstColumnName + " = '" + dbRow[0] + "' AND " + querySecondColumnName + " = '" + dbRow[1] + "'");
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void setDatetimeColumnValuesWithinQueryByOneColumn(String tableName, String datetimeColumnName, long datetimeColumnValue, String queryColumnName, String queryColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryColumnName, datetimeColumnName }, "substr("+queryColumnName+",1,"+queryColumnValue.length()+") = ?", new String[] { queryColumnValue }, null) ) {
				db.execSQL("UPDATE "+tableName+" SET "+datetimeColumnName+"="+datetimeColumnValue+" WHERE "+ queryColumnName +" = '"+ dbRow[0] +"'");
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public void setDatetimeColumnValuesWithinQueryByTimestamp(String tableName, String datetimeColumnName, long datetimeColumnValue, String timestampColumn, String timestampValue) {
		setDatetimeColumnValuesWithinQueryByOneColumn(tableName, datetimeColumnName, datetimeColumnValue, timestampColumn, timestampValue);
	}

	public void setDatetimeColumnValuesWithinQueryByTwoColumns(String tableName, String datetimeColumnName, long datetimeColumnValue, String queryFirstColumnName, String queryFirstColumnValue, String querySecondColumnName, String querySecondColumnValue) {
		SQLiteDatabase db = openDb();
		try {
			for (String[] dbRow : getRows(db, tableName, new String[] { queryFirstColumnName, querySecondColumnName }, "substr("+queryFirstColumnName+",1,"+queryFirstColumnValue.length()+") = ?", new String[] { queryFirstColumnValue }, null) ) {
				if (dbRow[1].equalsIgnoreCase(querySecondColumnValue)) {
					db.execSQL("UPDATE " + tableName + " SET " + datetimeColumnName + "=" + datetimeColumnValue + " WHERE " + queryFirstColumnName + " = '" + dbRow[0] + "' AND " + querySecondColumnName + " = '" + dbRow[1] + "'");
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		} finally {
			closeDb();
		}
	}

	public static long getSumOfColumn(SQLiteDatabase db, String tableName, String sumColumn, String selection, String[] selectionArgs) {
		long sum = 0;
		try {
			for (String[] singleRow : getRows(db, tableName, new String[] { "SUM("+sumColumn+")" }, selection, selectionArgs, null, 0, 1)) {
				if (singleRow[0] != null) {
					sum = Long.parseLong(singleRow[0]);
					break;
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return sum;
	}

	public long getSumOfColumn(String tableName, String sumColumn, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		long sum = getSumOfColumn(db, tableName, sumColumn, selection, selectionArgs);
		closeDb();
		return sum;
	}

	public static long getSumOfLengthOfColumn(SQLiteDatabase db, String tableName, String sumColumn, String selection, String[] selectionArgs) {
		long sum = 0;
		try {
			for (String[] singleRow : getRows(db, tableName, new String[] { "SUM(LENGTH("+sumColumn+"))" }, selection, selectionArgs, null, 0, 1)) {
				if (singleRow[0] != null) {
					sum = Long.parseLong(singleRow[0]);
					break;
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return sum;
	}

	public long getSumOfLengthOfColumn(String tableName, String sumColumn, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		long sum = getSumOfLengthOfColumn(db, tableName, sumColumn, selection, selectionArgs);
		closeDb();
		return sum;
	}

	public static long getMinValueOfColumn(SQLiteDatabase db, String tableName, String minValueOfWhichColumn, String selection, String[] selectionArgs) {
		long minVal = 0;
		try {
			for (String[] singleRow : getRows(db, tableName, new String[] { "MIN("+minValueOfWhichColumn+")" }, selection, selectionArgs, null, 0, 1)) {
				if (singleRow[0] != null) {
					minVal = Long.parseLong(singleRow[0]);
					break;
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return minVal;
	}

	public long getMinValueOfColumn(String tableName, String minValueOfWhichColumn, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		long minVal = getMinValueOfColumn(db, tableName, minValueOfWhichColumn, selection, selectionArgs);
		closeDb();
		return minVal;
	}

	public static long getMaxValueOfColumn(SQLiteDatabase db, String tableName, String maxValueOfWhichColumn, String selection, String[] selectionArgs) {
		long maxVal = 0;
		try {
			for (String[] singleRow : getRows(db, tableName, new String[] { "MAX("+maxValueOfWhichColumn+")" }, selection, selectionArgs, null, 0, 1)) {
				if (singleRow[0] != null) {
					maxVal = Long.parseLong(singleRow[0]);
					break;
				}
			}
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return maxVal;
	}

	public long getMaxValueOfColumn(String tableName, String maxValueOfWhichColumn, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openDb();
		long maxVal = getMaxValueOfColumn(db, tableName, maxValueOfWhichColumn, selection, selectionArgs);
		closeDb();
		return maxVal;
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

	/**
	 * Used by swm diagnostic db to compress null satellite values
	 */
	public static String getConcatRowsIgnoreNullSatellite(List<String[]> getRowsOutput) {
		String concatRows = null;
		ArrayList<String> rowList = new ArrayList<String>();
		try {
			for (String[] row : getRowsOutput) {
				String[] tempRow = row;
				// if time is null then others also null
				if (tempRow[5] == null) {
					tempRow = new String[] { tempRow[0], tempRow[1], tempRow[7]};
				}
				rowList.add(StringUtils.joinArrayString(tempRow, "*"));
			}
			concatRows = (rowList.size() > 0) ? StringUtils.joinArrayString(rowList.toArray(new String[0]), "|") : null;
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
