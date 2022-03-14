package org.rfcx.guardian.admin.device.android.system;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class DeviceDataTransferDb {

    static final String DATABASE = "data";
    static final String C_CREATED_AT = "created_at";
    static final String C_START_TIME = "start_time";
    static final String C_END_TIME = "end_time";
    static final String C_MOBILE_BYTES_RECEIVED_CURRENT = "mobile_bytes_received_current";
    static final String C_MOBILE_BYTES_SENT_CURRENT = "mobile_bytes_sent_current";
    static final String C_MOBILE_BYTES_RECEIVED_TOTAL = "mobile_bytes_received_total";
    static final String C_MOBILE_BYTES_SENT_TOTAL = "mobile_bytes_sent_total";
    static final String C_NETWORK_BYTES_RECEIVED_CURRENT = "network_bytes_received_current";
    static final String C_NETWORK_BYTES_SENT_CURRENT = "network_bytes_sent_current";
    static final String C_NETWORK_BYTES_RECEIVED_TOTAL = "network_bytes_received_total";
    static final String C_NETWORK_BYTES_SENT_TOTAL = "network_bytes_sent_total";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_START_TIME, C_END_TIME, C_MOBILE_BYTES_RECEIVED_CURRENT, C_MOBILE_BYTES_SENT_CURRENT, C_MOBILE_BYTES_RECEIVED_TOTAL, C_MOBILE_BYTES_SENT_TOTAL, C_NETWORK_BYTES_RECEIVED_CURRENT, C_NETWORK_BYTES_SENT_CURRENT, C_NETWORK_BYTES_RECEIVED_TOTAL, C_NETWORK_BYTES_SENT_TOTAL};
    public final DbTransferred dbTransferred;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public DeviceDataTransferDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbTransferred = new DbTransferred(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_START_TIME).append(" INTEGER")
                .append(", ").append(C_END_TIME).append(" INTEGER")
                .append(", ").append(C_MOBILE_BYTES_RECEIVED_CURRENT).append(" INTEGER")
                .append(", ").append(C_MOBILE_BYTES_SENT_CURRENT).append(" INTEGER")
                .append(", ").append(C_MOBILE_BYTES_RECEIVED_TOTAL).append(" INTEGER")
                .append(", ").append(C_MOBILE_BYTES_SENT_TOTAL).append(" INTEGER")
                .append(", ").append(C_NETWORK_BYTES_RECEIVED_CURRENT).append(" INTEGER")
                .append(", ").append(C_NETWORK_BYTES_SENT_CURRENT).append(" INTEGER")
                .append(", ").append(C_NETWORK_BYTES_RECEIVED_TOTAL).append(" INTEGER")
                .append(", ").append(C_NETWORK_BYTES_SENT_TOTAL).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbTransferred {

        final DbUtils dbUtils;
        public String FILEPATH;

        private final String TABLE = "transferred";

        public DbTransferred(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(Date created_at, Date start_time, Date end_time, long mobile_bytes_rx_current, long mobile_bytes_tx_current, long mobile_bytes_rx_total, long mobile_bytes_tx_total, long network_bytes_rx_current, long network_bytes_tx_current, long network_bytes_rx_total, long network_bytes_tx_total) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, created_at.getTime());
            values.put(C_START_TIME, start_time.getTime());
            values.put(C_END_TIME, end_time.getTime());
            values.put(C_MOBILE_BYTES_RECEIVED_CURRENT, mobile_bytes_rx_current);
            values.put(C_MOBILE_BYTES_SENT_CURRENT, mobile_bytes_tx_current);
            values.put(C_MOBILE_BYTES_RECEIVED_TOTAL, mobile_bytes_rx_total);
            values.put(C_MOBILE_BYTES_SENT_TOTAL, mobile_bytes_tx_total);
            values.put(C_NETWORK_BYTES_RECEIVED_CURRENT, network_bytes_rx_current);
            values.put(C_NETWORK_BYTES_SENT_CURRENT, network_bytes_tx_current);
            values.put(C_NETWORK_BYTES_RECEIVED_TOTAL, network_bytes_rx_total);
            values.put(C_NETWORK_BYTES_SENT_TOTAL, network_bytes_tx_total);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }
    }


}
