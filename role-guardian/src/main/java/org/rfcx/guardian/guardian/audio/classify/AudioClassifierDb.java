package org.rfcx.guardian.guardian.audio.classify;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioClassifierDb {

	public AudioClassifierDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbActive = new DbActive(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio-classifier";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_CLASSIFIER_ID = "classifier_id";
	static final String C_INPUT_SAMPLE_RATE = "input_sample_rate";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_FILEPATH = "filepath";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_CLASSIFIER_ID, C_INPUT_SAMPLE_RATE, C_FORMAT, C_DIGEST, C_FILEPATH };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
			.append(", ").append(C_INPUT_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(")");
		return sbOut.toString();
	}
	

	public class DbActive {

		final DbUtils dbUtils;

		private String TABLE = "active";
		
		public DbActive(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}
		
		public int insert(String classifierId, int inputSampleRate, String format, String digest, String filepath) {
			
			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_CLASSIFIER_ID, classifierId);
			values.put(C_INPUT_SAMPLE_RATE, inputSampleRate);
			values.put(C_FORMAT, format);
			values.put(C_DIGEST, digest);
			values.put(C_FILEPATH, filepath);
			
			return this.dbUtils.insertRow(TABLE, values);
		}
		
		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}
		
		public void deleteSingleRow(String classifierId) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_CLASSIFIER_ID, classifierId);
		}
		
		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}
		
//		public String[] getSingleRowByAudioId(String audioId) {
//			String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
//			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+ C_CLASSIFIER_ID +",1,"+timestamp.length()+") = ?", new String[] { timestamp }, null, 0);
//		}
		
	}
	public final DbActive dbActive;

	
}
