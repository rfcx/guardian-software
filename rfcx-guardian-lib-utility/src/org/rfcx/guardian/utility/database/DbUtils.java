package org.rfcx.guardian.utility.database;

import java.util.ArrayList;
import java.util.List;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class DbUtils {
	
	private static final String logTag = "Rfcx-Utils-"+DbUtils.class.getSimpleName();

	private static final int DEFAULT_ROWOFFSET = 0;
	private static final int DEFAULT_ROWLIMIT = 1000;
	private static final String DEFAULT_ORDER = "DESC";
	
	public static List<String[]> getRows(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy, int rowOffset, int rowLimit) {
		
		ArrayList<String[]> rowList = new ArrayList<String[]>();
		try { 
			Cursor cursor = db.query(tableName, tableColumns, selection, selectionArgs, null, null, (orderBy != null) ? orderBy+" "+DEFAULT_ORDER : null, ""+(rowOffset+rowLimit));
			if ((cursor.getCount() > rowOffset) && cursor.moveToPosition(rowOffset)) {
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
	
	public static List<String[]> getRows(SQLiteDatabase db, String tableName, String[] tableColumns, String selection, String[] selectionArgs, String orderBy) {
		return getRows(db, tableName, tableColumns, selection, selectionArgs, orderBy, DEFAULT_ROWOFFSET, DEFAULT_ROWLIMIT);
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

	public static int getCount(SQLiteDatabase db, String tableName, String selection, String[] selectionArgs) {
		int count = 0;
		try { 
			for (String[] singleRow : getRows(db, tableName, new String[] { "COUNT(*)" }, selection, selectionArgs, null, 0, 1)) {
				count = (int) Integer.parseInt(singleRow[0]);
				break;
			}
		} catch (Exception e) { 
			RfcxLog.logExc(logTag, e);
		}
		return count;
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
		}
		return rtrnStr;
	}
	
	private static String[] placeHolderStringArray(int columnCount) {
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
		}
		return rtrnStr;
	}
	
}
