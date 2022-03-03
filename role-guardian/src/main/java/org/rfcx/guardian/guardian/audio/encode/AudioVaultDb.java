package org.rfcx.guardian.guardian.audio.encode;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioVaultDb {

	public AudioVaultDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbVault = new DbVault(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "audio";
	
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_FORMAT = "format";
	static final String C_DIGEST = "digest";
	static final String C_SAMPLE_RATE = "samplerate";
	static final String C_BITRATE = "bitrate";
	static final String C_CODEC = "codec";
	static final String C_DURATION = "duration";
	static final String C_CREATION_DURATION = "creation_duration";
	static final String C_ENCODE_PURPOSE = "encode_purpose";
	static final String C_FILEPATH = "filepath";
	static final String C_INPUT_SAMPLE_RATE = "input_samplerate";
	static final String C_ATTEMPTS = "attempts";
	
	private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_SAMPLE_RATE, C_BITRATE, C_CODEC, C_DURATION, C_CREATION_DURATION, C_ENCODE_PURPOSE, C_FILEPATH, C_INPUT_SAMPLE_RATE, C_ATTEMPTS };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private static String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP).append(" TEXT")
			.append(", ").append(C_FORMAT).append(" TEXT")
			.append(", ").append(C_DIGEST).append(" TEXT")
			.append(", ").append(C_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_BITRATE).append(" INTEGER")
			.append(", ").append(C_CODEC).append(" TEXT")
			.append(", ").append(C_DURATION).append(" INTEGER")
			.append(", ").append(C_CREATION_DURATION).append(" INTEGER")
			.append(", ").append(C_ENCODE_PURPOSE).append(" TEXT")
			.append(", ").append(C_FILEPATH).append(" TEXT")
			.append(", ").append(C_INPUT_SAMPLE_RATE).append(" INTEGER")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}

	public class DbVault {

		final DbUtils dbUtils;

		private String TABLE = "vault";

		public DbVault(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(String rowId, long duration, long recordCount, long filesize) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, rowId);
			values.put(C_SAMPLE_RATE, duration);
			values.put(C_DURATION, recordCount);
			values.put(C_CREATION_DURATION, filesize);
			values.put(C_ENCODE_PURPOSE, "vault");
			values.put(C_ATTEMPTS, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getRowsById(String rowId) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, C_TIMESTAMP+" = ?", new String[] { rowId }, C_TIMESTAMP, 0, 1);
		}

		public int getCountById(String rowId) {
			return getRowsById(rowId).size();
		}

		public void incrementSingleRowRecordCount(String rowId, int incrementAmount) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+"+incrementAmount, TABLE, C_DURATION, C_TIMESTAMP, rowId);
		}

		public long getCumulativeRecordCountForAllRows() {
			return this.dbUtils.getSumOfColumn(TABLE, C_DURATION, null, null);
		}

		public void incrementSingleRowFileSize(String rowId, long incrementAmount) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+"+incrementAmount, TABLE, C_CREATION_DURATION, C_TIMESTAMP, rowId);
		}

		public long getCumulativeFileSizeForAllRows() {
			return this.dbUtils.getSumOfColumn(TABLE, C_CREATION_DURATION, null, null);
		}

		public void incrementSingleRowDuration(String rowId, long incrementAmount) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+"+incrementAmount, TABLE, C_SAMPLE_RATE, C_TIMESTAMP, rowId);
		}

		public long getCumulativeDurationForAllRows() {
			return this.dbUtils.getSumOfColumn(TABLE, C_SAMPLE_RATE, null, null);
		}



	}
	public final DbVault dbVault;
	
}
