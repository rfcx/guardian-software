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
	}

	private int VERSION = 1;
	static final String DATABASE = "instructions";

	static final String C_CREATED_AT = "created_at";
	static final String C_INSTRUCTION_ID = "instruction_id";
	static final String C_TYPE = "type";
	static final String C_EXECUTE_AT = "execute_at";
	static final String C_META = "meta";
	static final String C_ATTEMPTS = "attempts";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";

	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_INSTRUCTION_ID, C_TYPE, C_EXECUTE_AT, C_META, C_ATTEMPTS, C_LAST_ACCESSED_AT };

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_INSTRUCTION_ID).append(" TEXT")
			.append(", ").append(C_TYPE).append(" TEXT")
			.append(", ").append(C_EXECUTE_AT).append(" INTEGER")
			.append(", ").append(C_META).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}



	public class DbQueuedInstructions {

		final DbUtils dbUtils;

		private String TABLE = "queued";

		public DbQueuedInstructions(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE));
		}

		public int insert(String instructionId, String instructionType, long executeAtOrAfter, String instructionMeta) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_INSTRUCTION_ID, instructionId);
			values.put(C_TYPE, instructionType);
			values.put(C_EXECUTE_AT, executeAtOrAfter);
			values.put(C_META, instructionMeta);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public List<String[]> getRowsInOrderOfExecution() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_EXECUTE_AT +" ASC");
		}

		public int deleteSingleRowByInstructionId(String instructionId) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_INSTRUCTION_ID, instructionId);
			return 0;
		}

		public long updateLastAccessedAtByInstructionId(String instructionId) {
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_INSTRUCTION_ID, instructionId);
			return rightNow;
		}

	}
	public final DbQueuedInstructions dbQueuedInstructions;
	
}
