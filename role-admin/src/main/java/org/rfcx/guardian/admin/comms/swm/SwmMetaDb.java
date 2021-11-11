package org.rfcx.guardian.admin.comms.swm;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class SwmMetaDb {

    public SwmMetaDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbSwmDiagnostic = new DbSwmDiagnostic(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "swm-meta";
    static final String C_CREATED_AT = "created_at";
    static final String C_RSSI_BACKGROUND = "rssi_background";
    static final String C_RSSI_SAT = "rssi_sat";
    static final String C_SNR = "snr";
    static final String C_FDEV = "fdev";
    static final String C_TIME = "time";
    static final String C_SAT_ID = "sat_id";
    static final String C_UNSENT_MESSAGE_COUNT = "unsent_message_count";
    private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_RSSI_BACKGROUND, C_RSSI_SAT, C_SNR, C_FDEV, C_TIME, C_SAT_ID, C_UNSENT_MESSAGE_COUNT };

    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
    private boolean DROP_TABLE_ON_UPGRADE = false;

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_RSSI_BACKGROUND).append(" INTEGER")
                .append(", ").append(C_RSSI_SAT).append(" INTEGER")
                .append(", ").append(C_SNR).append(" INTEGER")
                .append(", ").append(C_FDEV).append(" INTEGER")
                .append(", ").append(C_TIME).append(" TEXT")
                .append(", ").append(C_SAT_ID).append(" TEXT")
                .append(", ").append(C_UNSENT_MESSAGE_COUNT).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbSwmDiagnostic {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "diagnostic";

        public DbSwmDiagnostic(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(Integer rssiBackground, Integer rssiSat, Integer snr, Integer fdev, String time, String satId, Integer unsentMessageNumbers) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_RSSI_BACKGROUND, rssiBackground);
            values.put(C_RSSI_SAT, rssiSat);
            values.put(C_SNR, snr);
            values.put(C_FDEV, fdev);
            values.put(C_TIME, time);
            values.put(C_SAT_ID, satId);
            values.put(C_UNSENT_MESSAGE_COUNT, unsentMessageNumbers);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public JSONArray getLatestRowAsJsonArray() {
            return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, null);
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

        public String getConcatRowsIgnoreNull() {
            return DbUtils.getConcatRowsIgnoreNullSatellite(getAllRows());
        }

        public String getConcatRowsWithLabelPrepended(String labelToPrepend) {
            return DbUtils.getConcatRowsWithLabelPrepended(labelToPrepend, getAllRows());
        }

    }
    public final DbSwmDiagnostic dbSwmDiagnostic;
}
