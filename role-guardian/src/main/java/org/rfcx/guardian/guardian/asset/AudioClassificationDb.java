package org.rfcx.guardian.guardian.asset;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioClassificationDb {

	public AudioClassificationDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbUnfiltered = new DbUnfiltered(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio-classification";
	static final String C_CREATED_AT = "created_at";
	static final String C_CLASSIFICATION_ID = "classification_id";
	static final String C_CLASS_TAG = "class_tag";
	static final String C_CLASSIFIER_ID = "classifier_id";
	static final String C_FILTER_ID = "filter_id";
	static final String C_AUDIO_ID = "audio_id";
	static final String C_BEGINS_AT = "begins_at";
	static final String C_CONFIDENCE_JSON = "confidence_json";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";

	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_CLASSIFICATION_ID, C_CLASS_TAG, C_CLASSIFIER_ID, C_FILTER_ID, C_AUDIO_ID, C_BEGINS_AT, C_CONFIDENCE_JSON, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_CLASSIFICATION_ID).append(" TEXT")
			.append(", ").append(C_CLASS_TAG).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
			.append(", ").append(C_FILTER_ID).append(" TEXT")
			.append(", ").append(C_AUDIO_ID).append(" TEXT")
			.append(", ").append(C_BEGINS_AT).append(" TEXT")
			.append(", ").append(C_CONFIDENCE_JSON).append(" TEXT")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	
	public class DbUnfiltered {

		final DbUtils dbUtils;
		public String FILEPATH = "";

		private String TABLE = "unfiltered";
		
		public DbUnfiltered(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}
		
		public int insert(String classificationId, String classTag, String classifierId, String audioId, long beginsAt, String confidenceJson) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_CLASSIFICATION_ID, classificationId);
			values.put(C_CLASS_TAG, classTag);
			values.put(C_CLASSIFIER_ID, classifierId);
			values.put(C_AUDIO_ID, audioId);
			values.put(C_BEGINS_AT, beginsAt);
			values.put(C_CONFIDENCE_JSON, confidenceJson);
			values.put(C_LAST_ACCESSED_AT, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

	}
	public final DbUnfiltered dbUnfiltered;
	
	
}
