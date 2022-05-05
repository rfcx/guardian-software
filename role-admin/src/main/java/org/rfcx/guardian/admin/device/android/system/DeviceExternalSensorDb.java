package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceExternalSensorDb {

    static final String DATABASE = "device";
    static final String C_MEASURED_AT = "measured_at";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{};
    public final DbBME688 dbBME688;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceExternalSensorDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbBME688 = new DbBME688(context);
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
            values.put(C_HUMIDITY, pressure);
            values.put(C_TEMPERATURE, pressure);
            values.put(C_GAS, pressure);

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
