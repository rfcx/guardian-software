package org.rfcx.guardian.guardian.instructions;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class InstructionsDb {

    public InstructionsDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbQueued = new DbQueued(context);
        this.dbExecuted = new DbExecuted(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "instructions";

    static final String C_CREATED_AT = "created_at";
    static final String C_INSTR_ID = "instr_id";
    static final String C_TYPE = "type";
    static final String C_COMMAND = "command";
    static final String C_EXECUTE_AT = "execute_at";
    static final String C_JSON = "json";
    static final String C_ATTEMPTS = "attempts";
    static final String C_TIMESTAMP_EXTRA = "timestamp_extra";
    static final String C_RECEIVED_BY = "received_by";
    static final String C_LAST_ACCESSED_AT = "last_accessed_at";
    static final String C_ORIGIN = "origin";

    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_INSTR_ID, C_TYPE, C_COMMAND, C_EXECUTE_AT, C_JSON, C_ATTEMPTS, C_TIMESTAMP_EXTRA, C_RECEIVED_BY, C_LAST_ACCESSED_AT, C_ORIGIN};

    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private boolean DROP_TABLE_ON_UPGRADE = false;

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_INSTR_ID).append(" TEXT")
                .append(", ").append(C_TYPE).append(" TEXT")
                .append(", ").append(C_COMMAND).append(" TEXT")
                .append(", ").append(C_EXECUTE_AT).append(" INTEGER")
                .append(", ").append(C_JSON).append(" TEXT")
                .append(", ").append(C_ATTEMPTS).append(" INTEGER")
                .append(", ").append(C_TIMESTAMP_EXTRA).append(" INTEGER")
                .append(", ").append(C_RECEIVED_BY).append(" TEXT")
                .append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
                .append(", ").append(C_ORIGIN).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }


    public class DbQueued {

        final DbUtils dbUtils;

        private String TABLE = "queued";

        public DbQueued(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int findByIdOrCreate(String instructionId, String instructionType, String instructionCommand, long executeAtOrAfter, String metaJson, String origin) {

            if (getCountById(instructionId) == 0) {
                ContentValues values = new ContentValues();
                values.put(C_CREATED_AT, (new Date()).getTime());
                values.put(C_INSTR_ID, instructionId);
                values.put(C_TYPE, instructionType);
                values.put(C_COMMAND, instructionCommand);
                values.put(C_EXECUTE_AT, executeAtOrAfter);
                values.put(C_JSON, metaJson);
                values.put(C_ATTEMPTS, 0);
                values.put(C_ORIGIN, origin);
                values.put(C_TIMESTAMP_EXTRA, (new Date()).getTime());
                values.put(C_LAST_ACCESSED_AT, (new Date()).getTime());
                this.dbUtils.insertRow(TABLE, values);
            }
            return getCountById(instructionId);
        }

        public int getCountById(String instructionId) {
            return this.dbUtils.getCount(TABLE, C_INSTR_ID + "=?", new String[]{instructionId});
        }

        public List<String[]> getRowsInOrderOfExecution() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_EXECUTE_AT + " ASC");
        }

        public int deleteSingleRowById(String instructionId) {
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_INSTR_ID, instructionId);
            return 0;
        }

        public void incrementSingleRowAttemptsById(String instructionId) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_INSTR_ID, instructionId);
        }

        public long updateLastAccessedAtById(String instructionId) {
            long rightNow = (new Date()).getTime();
            this.dbUtils.setDatetimeColumnValuesWithinQueryByOneColumn(TABLE, C_LAST_ACCESSED_AT, rightNow, C_INSTR_ID, instructionId);
            return rightNow;
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

    }

    public final DbQueued dbQueued;


    public class DbExecuted {

        final DbUtils dbUtils;

        private String TABLE = "executed";

        public DbExecuted(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int findByIdOrCreate(String instructionId, String instructionType, String instructionCommand, long executedAt, String responseJson, int attempts, long timestampExtra, String origin) {

            if (getCountById(instructionId) == 0) {
                ContentValues values = new ContentValues();
                values.put(C_CREATED_AT, (new Date()).getTime());
                values.put(C_INSTR_ID, instructionId);
                values.put(C_TYPE, instructionType);
                values.put(C_COMMAND, instructionCommand);
                values.put(C_EXECUTE_AT, executedAt);
                values.put(C_JSON, responseJson);
                values.put(C_ATTEMPTS, attempts);
                values.put(C_ORIGIN, origin);
                values.put(C_TIMESTAMP_EXTRA, timestampExtra);
                values.put(C_LAST_ACCESSED_AT, (new Date()).getTime());
                this.dbUtils.insertRow(TABLE, values);
            }
            return getCountById(instructionId);
        }

        public int getCountById(String instructionId) {
            return this.dbUtils.getCount(TABLE, C_INSTR_ID + "=?", new String[]{instructionId});
        }

        public List<String[]> getRowsInOrderOfExecution() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_EXECUTE_AT + " ASC");
        }

        public int deleteSingleRowById(String instructionId) {
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_INSTR_ID, instructionId);
            return 0;
        }

        public void incrementSingleRowAttemptsById(String instructionId) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_INSTR_ID, instructionId);
        }

        public long updateLastAccessedAtById(String instructionId) {
            long rightNow = (new Date()).getTime();
            this.dbUtils.setDatetimeColumnValuesWithinQueryByOneColumn(TABLE, C_LAST_ACCESSED_AT, rightNow, C_INSTR_ID, instructionId);
            return rightNow;
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

    }

    public final DbExecuted dbExecuted;

}
