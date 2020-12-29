package org.rfcx.guardian.guardian.asset;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioClassificationDb {

	public AudioClassificationDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbDetected = new DbDetected(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio-classification";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_CLASSIFIER_ID = "classifier_id";
	static final String C_FILTER_ID = "filter_id";
	static final String C_CLASSIFICATION_TAG = "classification_tag";
	static final String C_CLASSIFICATION_JOB_ID = "classification_job_id";
	static final String C_CONFIDENCE_VALUE = "confidence_value";
	static final String C_WINDOW_DURATION = "window_duration";

	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_CLASSIFIER_ID, C_FILTER_ID, C_CLASSIFICATION_TAG, C_CLASSIFICATION_JOB_ID, C_CONFIDENCE_VALUE, C_WINDOW_DURATION };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
			.append(", ").append(C_FILTER_ID).append(" TEXT")
			.append(", ").append(C_CLASSIFICATION_TAG).append(" TEXT")
			.append(", ").append(C_CLASSIFICATION_JOB_ID).append(" TEXT")
			.append(", ").append(C_CONFIDENCE_VALUE).append(" REAL")
			.append(", ").append(C_WINDOW_DURATION).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbDetected {

		final DbUtils dbUtils;
		public String FILEPATH = "";

		private String TABLE = "detected";
		
		public DbDetected(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}
		
		public int insert(String timestamp, String classifierId, String filterId, String classificationTag, String classificationJobId, float confidenceValue, int windowDuration) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp);
			values.put(C_CLASSIFIER_ID, classifierId);
			values.put(C_FILTER_ID, filterId);
			values.put(C_CLASSIFICATION_TAG, classificationTag);
			values.put(C_CLASSIFICATION_JOB_ID, classificationJobId);
			values.put(C_CONFIDENCE_VALUE, confidenceValue);
			values.put(C_WINDOW_DURATION, windowDuration);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
//		public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
//			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
//		}
//
//		public String[] getLatestRow() {
//			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
//		}
//
//		public List<String[]> getLatestRowsWithLimit(int maxRows) {
//			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
//		}
//
//		public List<String[]> getLatestRowsWithLimitExcludeCreatedAt(int maxRows) {
//			List<String[]> rowsWithAllFields = this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
//			List<String[]> rowsWithoutCreatedAt = new ArrayList<String[]>();
//			for (String[] singleRow : rowsWithAllFields) {
//				rowsWithoutCreatedAt.add(new String[] { singleRow[1], singleRow[2] });
//			}
//			return rowsWithoutCreatedAt;
//		}
//
//		public int deleteSingleRowByTimestamp(String timestamp) {
//			String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
//			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
//			return 0;
//		}
//
//		public String getConcatRows() {
//			return DbUtils.getConcatRows(getAllRows());
//		}
//
//		public String getConcatRowsWithLimit(int maxRows) {
//			return DbUtils.getConcatRows(getLatestRowsWithLimit(maxRows));
//		}

	}
	public final DbDetected dbDetected;
	
	
}
