package org.rfcx.guardian.guardian.audio.classify;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioClassifyDb {

	public AudioClassifyDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbQueued = new DbQueued(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio-classify";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_AUDIO_ID = "audio_id";
	static final String C_CLASSIFIER_ID = "classifier_id";
	static final String C_CLASSIFIER_VERSION = "classifier_version";
	static final String C_ORIGINAL_SAMPLE_RATE = "original_sample_rate";
	static final String C_CLASSIFIER_SAMPLE_RATE = "input_sample_rate";
	static final String C_AUDIO_FILEPATH = "audio_filepath";
	static final String C_CLASSIFIER_FILEPATH = "classifier_filepath";
	static final String C_CLASSIFIER_WINDOW_SIZE = "classifier_window_size";
	static final String C_CLASSIFIER_STEP_SIZE = "classifier_step_size";
	static final String C_CLASSIFIER_CLASSIFICATIONS = "classifier_classes";
	static final String C_ATTEMPTS = "attempts";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_AUDIO_ID, C_CLASSIFIER_ID, C_CLASSIFIER_VERSION, C_ORIGINAL_SAMPLE_RATE, C_CLASSIFIER_SAMPLE_RATE, C_AUDIO_FILEPATH, C_CLASSIFIER_FILEPATH, C_CLASSIFIER_WINDOW_SIZE, C_CLASSIFIER_STEP_SIZE, C_CLASSIFIER_CLASSIFICATIONS, C_ATTEMPTS };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_AUDIO_ID).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_VERSION).append(" TEXT")
			.append(", ").append(C_ORIGINAL_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_CLASSIFIER_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_AUDIO_FILEPATH).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_FILEPATH).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_WINDOW_SIZE).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_STEP_SIZE).append(" TEXT")
			.append(", ").append(C_CLASSIFIER_CLASSIFICATIONS).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbQueued {

		final DbUtils dbUtils;

		private String TABLE = "queued";
		
		public DbQueued(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String audioId, String classifierId, String classifierVersion, int originalSampleRate, int classifierSampleRate, String audioFilepath, String classifierFilepath, String windowSize, String stepSize, String classes) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_AUDIO_ID, audioId);
			values.put(C_CLASSIFIER_ID, classifierId);
			values.put(C_CLASSIFIER_VERSION, classifierVersion);
			values.put(C_ORIGINAL_SAMPLE_RATE, originalSampleRate);
			values.put(C_CLASSIFIER_SAMPLE_RATE, classifierSampleRate);
			values.put(C_AUDIO_FILEPATH, audioFilepath);
			values.put(C_CLASSIFIER_FILEPATH, classifierFilepath);
			values.put(C_CLASSIFIER_WINDOW_SIZE, windowSize);
			values.put(C_CLASSIFIER_STEP_SIZE,stepSize );
			values.put(C_CLASSIFIER_CLASSIFICATIONS, classes);
			values.put(C_ATTEMPTS, 0);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public void deleteSingleRow(String audioId, String classifierId) {
			this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_AUDIO_ID, audioId, C_CLASSIFIER_ID, classifierId);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public void incrementSingleRowAttempts(String audioId, String classifierId) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTwoColumns("+1", TABLE, C_ATTEMPTS, C_AUDIO_ID, audioId, C_CLASSIFIER_ID, classifierId);
		}
		
	}
	public final DbQueued dbQueued;

	
}
