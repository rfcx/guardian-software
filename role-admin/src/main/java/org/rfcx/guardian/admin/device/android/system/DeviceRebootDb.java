package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceRebootDb {

    static final String DATABASE = "reboot";
    static final String C_CREATED_AT = "created_at";
    static final String C_REBOOTED_AT = "rebooted_at";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_REBOOTED_AT};
    public final DbRebootComplete dbRebootComplete;
    public final DbRebootAttempt dbRebootAttempt;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceRebootDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbRebootComplete = new DbRebootComplete(context);
        this.dbRebootAttempt = new DbRebootAttempt(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_REBOOTED_AT).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbRebootComplete {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "reboots";

        public DbRebootComplete(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(long rebootedAt) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_REBOOTED_AT, rebootedAt);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

    }

    public class DbRebootAttempt {

        final DbUtils dbUtils;

        private String TABLE = "attempts";

        public DbRebootAttempt(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(long rebootedAt) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_REBOOTED_AT, rebootedAt);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

    }


}
