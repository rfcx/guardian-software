package org.rfcx.guardian.guardian.diagnostic;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DiagnosticDb {

    public DiagnosticDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.dbRecordedDiagnostic = new DbRecordedDiagnostic(context);
        this.dbSyncedDiagnostic = new DbSyncedDiagnostic(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "instructions";

    static final String C_CREATED_AT = "created_at";
    static final String C_RECORDED = "recorded";
    static final String C_AMOUNT_OF_TIME = "amount_of_time";
    static final String C_SYNCED = "synced";

    private static final String[] ALL_RECORDED_COLUMNS = new String[]{C_CREATED_AT, C_RECORDED, C_AMOUNT_OF_TIME};
    private static final String[] ALL_SYNCED_COLUMNS = new String[]{C_CREATED_AT, C_SYNCED, C_AMOUNT_OF_TIME};

    private String createRecordedColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_RECORDED).append(" INTEGER")
                .append(", ").append(C_AMOUNT_OF_TIME).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    private String createSyncedColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_SYNCED).append(" INTEGER")
                .append(", ").append(C_AMOUNT_OF_TIME).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbRecordedDiagnostic {

        final DbUtils dbUtils;

        private String TABLE = "recorded";

        public DbRecordedDiagnostic(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createRecordedColumnString(TABLE));
        }

        public int insert() {
            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_RECORDED, 0);
            values.put(C_AMOUNT_OF_TIME, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public void updateRecordedAndTime(int recorded, int amount) {
            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_RECORDED, recorded);
            values.put(C_AMOUNT_OF_TIME, amount);

            this.dbUtils.updateFirstRow(TABLE, values);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_RECORDED_COLUMNS, null, null, C_CREATED_AT, 0);
        }

    }

    public final DbRecordedDiagnostic dbRecordedDiagnostic;

    public class DbSyncedDiagnostic {

        final DbUtils dbUtils;

        private String TABLE = "synced";

        public DbSyncedDiagnostic(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createSyncedColumnString(TABLE));
        }

        public int insert() {
            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_SYNCED, 0);
            values.put(C_AMOUNT_OF_TIME, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public void updateSyncedAndTime(int recorded, int amount) {
            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_SYNCED, recorded);
            values.put(C_AMOUNT_OF_TIME, amount);

            this.dbUtils.updateFirstRow(TABLE, values);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_SYNCED_COLUMNS, null, null, C_CREATED_AT, 0);
        }

    }

    public final DbSyncedDiagnostic dbSyncedDiagnostic;
}
