package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceSpaceDb {

    static final String DATABASE = "space";
    static final String C_LABEL = "label";
    static final String C_MEASURED_AT = "measured_at";
    static final String C_VALUE_1 = "value_1";
    static final String C_VALUE_2 = "value_2";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_LABEL, C_MEASURED_AT, C_VALUE_1, C_VALUE_2};
    public final DbStorage dbStorage;
    public final DbMemory dbMemory;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceSpaceDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbStorage = new DbStorage(context);
        this.dbMemory = new DbMemory(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_LABEL).append(" TEXT")
                .append(", ").append(C_MEASURED_AT).append(" INTEGER")
                .append(", ").append(C_VALUE_1).append(" TEXT")
                .append(", ").append(C_VALUE_2).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbStorage {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "storage";

        public DbStorage(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String label, Date measured_at, long bytes_used, long bytes_available) {

            ContentValues values = new ContentValues();
            values.put(C_LABEL, label);
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_VALUE_1, bytes_used);
            values.put(C_VALUE_2, bytes_available);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_MEASURED_AT, date);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }

    }

    public class DbMemory {

        final DbUtils dbUtils;

        private String TABLE = "memory";

        public DbMemory(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String label, Date measured_at, long bytes_used, long bytes_available, long bytes_threshold) {

            ContentValues values = new ContentValues();
            values.put(C_LABEL, label);
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_VALUE_1, bytes_used);
            values.put(C_VALUE_2, bytes_available + "*" + bytes_threshold);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_MEASURED_AT, date);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }

    }


}
