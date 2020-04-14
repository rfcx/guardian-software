package org.rfcx.guardian.guardian.instructions;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class InstructionsDb {

	public InstructionsDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.dbQueuedInstructions = new DbQueuedInstructions(context);
		this.dbExecutedInstructions = new DbExecutedInstructions(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "instructions";

	static final String C_CREATED_AT = "created_at";
	static final String C_GUID = "guid";
	static final String C_TYPE = "type";
	static final String C_COMMAND = "command";
	static final String C_EXECUTE_AT = "execute_at";
	static final String C_JSON = "json";
	static final String C_ATTEMPTS = "attempts";
	static final String C_TIMESTAMP_EXTRA = "timestamp_extra";

	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_GUID, C_TYPE, C_COMMAND, C_EXECUTE_AT, C_JSON, C_ATTEMPTS, C_TIMESTAMP_EXTRA };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_GUID).append(" TEXT")
			.append(", ").append(C_TYPE).append(" TEXT")
			.append(", ").append(C_COMMAND).append(" TEXT")
			.append(", ").append(C_EXECUTE_AT).append(" INTEGER")
			.append(", ").append(C_JSON).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_TIMESTAMP_EXTRA).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}



	public class DbQueuedInstructions {

		final DbUtils dbUtils;

		private String TABLE = "queued";

		public DbQueuedInstructions(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(String instructionGuid, String instructionType, String instructionCommand, long executeAtOrAfter, String metaJson) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_GUID, instructionGuid);
			values.put(C_TYPE, instructionType);
			values.put(C_COMMAND, instructionCommand);
			values.put(C_EXECUTE_AT, executeAtOrAfter);
			values.put(C_JSON, metaJson);
			values.put(C_ATTEMPTS, 0);
			values.put(C_TIMESTAMP_EXTRA, (new Date()).getTime());

			return this.dbUtils.insertRow(TABLE, values);
		}

		public int findByGuidOrCreate(String instructionGuid, String instructionType, String instructionCommand, long executeAtOrAfter, String metaJson) {

			if (getCountByGuid(instructionGuid) == 0) {
				ContentValues values = new ContentValues();
				values.put(C_CREATED_AT, (new Date()).getTime());
				values.put(C_GUID, instructionGuid);
				values.put(C_TYPE, instructionType);
				values.put(C_COMMAND, instructionCommand);
				values.put(C_EXECUTE_AT, executeAtOrAfter);
				values.put(C_JSON, metaJson);
				values.put(C_ATTEMPTS, 0);
				values.put(C_TIMESTAMP_EXTRA, (new Date()).getTime());
				this.dbUtils.insertRow(TABLE, values);
			}
			return getCountByGuid(instructionGuid);
		}

		public int getCountByGuid(String instructionGuid) {
			return this.dbUtils.getCount(TABLE, C_GUID+"=?",new String[] { instructionGuid });
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public List<String[]> getRowsInOrderOfExecution() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_EXECUTE_AT +" ASC");
		}

		public int deleteSingleRowByGuid(String instructionGuid) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_GUID, instructionGuid);
			return 0;
		}

		public void incrementSingleRowAttemptsByGuid(String instructionGuid) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_GUID, instructionGuid);
		}

	}
	public final DbQueuedInstructions dbQueuedInstructions;



	public class DbExecutedInstructions {

		final DbUtils dbUtils;

		private String TABLE = "executed";

		public DbExecutedInstructions(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(String instructionGuid, String instructionType, String instructionCommand, long executedAt, String responseJson, int attempts, long timestampExtra) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_GUID, instructionGuid);
			values.put(C_TYPE, instructionType);
			values.put(C_COMMAND, instructionCommand);
			values.put(C_EXECUTE_AT, executedAt);
			values.put(C_JSON, responseJson);
			values.put(C_ATTEMPTS, attempts);
			values.put(C_TIMESTAMP_EXTRA, timestampExtra);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public int findByGuidOrCreate(String instructionGuid, String instructionType, String instructionCommand, long executedAt, String responseJson, int attempts, long timestampExtra) {

			if (getCountByGuid(instructionGuid) == 0) {
				ContentValues values = new ContentValues();
				values.put(C_CREATED_AT, (new Date()).getTime());
				values.put(C_GUID, instructionGuid);
				values.put(C_TYPE, instructionType);
				values.put(C_COMMAND, instructionCommand);
				values.put(C_EXECUTE_AT, executedAt);
				values.put(C_JSON, responseJson);
				values.put(C_ATTEMPTS, attempts);
				values.put(C_TIMESTAMP_EXTRA, timestampExtra);
				this.dbUtils.insertRow(TABLE, values);
			}
			return getCountByGuid(instructionGuid);
		}

		public int getCountByGuid(String instructionGuid) {
			return this.dbUtils.getCount(TABLE, C_GUID+"=?",new String[] { instructionGuid });
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public List<String[]> getRowsInOrderOfExecution() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_EXECUTE_AT +" ASC");
		}

		public int deleteSingleRowByGuid(String instructionGuid) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_GUID, instructionGuid);
			return 0;
		}

		public void incrementSingleRowAttemptsByGuid(String instructionGuid) {
			this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_GUID, instructionGuid);
		}

	}
	public final DbExecutedInstructions dbExecutedInstructions;
	
}
