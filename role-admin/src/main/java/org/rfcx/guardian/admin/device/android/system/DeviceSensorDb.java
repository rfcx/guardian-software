package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceSensorDb {

    static final String DATABASE = "device";
    static final String C_MEASURED_AT = "measured_at";
    static final String C_VALUE_1 = "value_1";
    static final String C_VALUE_2 = "value_2";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_MEASURED_AT, C_VALUE_1, C_VALUE_2};
    public final DbGeoPosition dbGeoPosition;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceSensorDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbGeoPosition = new DbGeoPosition(context);
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

    public class DbGeoPosition {

        final DbUtils dbUtils;

        private final String TABLE = "geoposition";

        public DbGeoPosition(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(double measured_at, double latitude, double longitude, double accuracy, double altitude) {

            ContentValues values = new ContentValues();
            values.put(C_MEASURED_AT, Math.round(measured_at));
            values.put(C_VALUE_1, latitude + "," + longitude);
            values.put(C_VALUE_2, Math.round(accuracy) + "," + Math.round(altitude));

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
