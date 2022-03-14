package org.rfcx.guardian.guardian.device.android;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceSystemDb {

    static final String DATABASE = "device";
    static final String C_MEASURED_AT = "measured_at";
    static final String C_VALUE_1 = "value_1";
    static final String C_VALUE_2 = "value_2";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_VALUE_1, C_VALUE_2};
    public final DbMqttBroker dbMqttBroker;
    public final DbDateTimeOffsets dbDateTimeOffsets;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceSystemDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbMqttBroker = new DbMqttBroker(context);
        this.dbDateTimeOffsets = new DbDateTimeOffsets(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_MEASURED_AT).append(" INTEGER")
                .append(", ").append(C_VALUE_1).append(" TEXT")
                .append(", ").append(C_VALUE_2).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbMqttBroker {

        final DbUtils dbUtils;

        private final String TABLE = "mqttbroker";

        public DbMqttBroker(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(Date measured_at, long connection_latency, long subscription_latency, String protocol, String host, int port) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, measured_at.getTime());
            values.put(C_VALUE_1, connection_latency + "*" + subscription_latency);
            values.put(C_VALUE_2, (protocol + "://" + host + ":" + port).replaceAll("\\*", "-").replaceAll("\\|", "-"));

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

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

    }

    public class DbDateTimeOffsets {

        final DbUtils dbUtils;

        private final String TABLE = "datetimeoffsets";

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

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

    }

}
