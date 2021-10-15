package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceSystemDb {

    public DeviceSystemDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbCPU = new DbCPU(context);
        this.dbBattery = new DbBattery(context);
        this.dbTelephony = new DbTelephony(context);
        this.dbDateTimeOffsets = new DbDateTimeOffsets(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "device";
    static final String C_MEASURED_AT = "measured_at";
    static final String C_VALUE_1 = "value_1";
    static final String C_VALUE_2 = "value_2";
    private static final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_VALUE_1, C_VALUE_2};

    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private boolean DROP_TABLE_ON_UPGRADE = false;

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_MEASURED_AT).append(" INTEGER")
                .append(", ").append(C_VALUE_1).append(" TEXT")
                .append(", ").append(C_VALUE_2).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbCPU {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "cpu";

        public DbCPU(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(Date measured_at, int cpu_percent, int cpu_clock, int cpu_core_usage) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_VALUE_1, cpu_percent);
            values.put(C_VALUE_2, cpu_clock + "*" + cpu_core_usage);

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

    public final DbCPU dbCPU;


    public class DbBattery {

        final DbUtils dbUtils;

        private String TABLE = "battery";

        public DbBattery(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(Date measured_at, int battery_percent, int battery_temperature, int is_charging, int is_charged) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_VALUE_1, battery_percent + "*" + battery_temperature);
            values.put(C_VALUE_2, is_charging + "*" + is_charged);

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

    public final DbBattery dbBattery;


    public class DbTelephony {

        final DbUtils dbUtils;

        private String TABLE = "telephony";

        public DbTelephony(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(Date measured_at, int signal_strength, String network_type, String carrier_name) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            // this is obviously a hack...
            // ...to concat two values into a single column.
            // may want to change/consider later
            values.put(C_VALUE_1, signal_strength + "*" + network_type);
            values.put(C_VALUE_2, carrier_name.replaceAll("\\*", "-").replaceAll("\\|", "-"));

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

    public final DbTelephony dbTelephony;

    public class DbDateTimeOffsets {

        final DbUtils dbUtils;

        private String TABLE = "datetimeoffsets";

        public DbDateTimeOffsets(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(long measured_at, String source, long offset, String timezone) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at);
            values.put(C_VALUE_1, source);
            values.put(C_VALUE_2, offset + "*" + timezone);

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

    public final DbDateTimeOffsets dbDateTimeOffsets;

}
