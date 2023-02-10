package org.rfcx.guardian.admin.device.i2c.sentry;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class SentrySensorDb {

    static final String DATABASE = "sentry-sensor";
    static final String C_MEASURED_AT = "measured_at";
    static final String C_VALUE_1 = "value_1";
    static final String C_VALUE_2 = "value_2";
    static final String C_VALUE_3 = "value_3";
    static final String C_VALUE_4 = "value_4";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_VALUE_1, C_VALUE_2, C_VALUE_3, C_VALUE_4};
    public final DbBME688 dbBME688;
    public final DbInfineon dbInfineon;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public SentrySensorDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbBME688 = new DbBME688(context);
        this.dbInfineon = new DbInfineon(context);
    }

    public class DbBME688 {

        final DbUtils dbUtils;
        public String FILEPATH;

        private final String TABLE = "bme688";

        private static final String C_PRESSURE = "pressure";
        private static final String C_HUMIDITY = "humidity";
        private static final String C_TEMPERATURE = "temperature";
        private static final String C_GAS = "gas";

        private final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_PRESSURE, C_HUMIDITY, C_TEMPERATURE, C_GAS};

        private String createColumnString() {
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("CREATE TABLE ").append(TABLE)
                    .append("(").append(C_MEASURED_AT).append(" INTEGER")
                    .append(", ").append(C_PRESSURE).append(" TEXT")
                    .append(", ").append(C_HUMIDITY).append(" TEXT")
                    .append(", ").append(C_TEMPERATURE).append(" TEXT")
                    .append(", ").append(C_GAS).append(" TEXT")
                    .append(")");
            return sbOut.toString();
        }

        public DbBME688(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(Date measured_at, String pressure, String humidity, String temperature, String gas) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_PRESSURE, pressure);
            values.put(C_HUMIDITY, humidity);
            values.put(C_TEMPERATURE, temperature);
            values.put(C_GAS, gas);

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

        public String getConcatRowsIgnoreNull(String labelToPrepend) {
            return DbUtils.getConcatRowsIgnoreNullSensors(labelToPrepend, getAllRows());
        }

        public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
            return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
        }

    }

    public class DbInfineon {

        final DbUtils dbUtils;
        public String FILEPATH;

        private final String TABLE = "infineon";

        private static final String C_CO2 = "co2";

        private final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_CO2};

        private String createColumnString() {
            StringBuilder sbOut = new StringBuilder();
            sbOut.append("CREATE TABLE ").append(TABLE)
                    .append("(").append(C_MEASURED_AT).append(" INTEGER")
                    .append(", ").append(C_CO2).append(" INTEGER")
                    .append(")");
            return sbOut.toString();
        }

        public DbInfineon(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(Date measured_at, int co2) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_CO2, co2);

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

        public String getConcatRowsIgnoreNull(String labelToPrepend) {
            return DbUtils.getConcatRowsIgnoreNullSensors(labelToPrepend, getAllRows());
        }

        public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
            return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
        }

    }
}
