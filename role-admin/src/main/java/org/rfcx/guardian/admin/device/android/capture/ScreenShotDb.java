package org.rfcx.guardian.admin.device.android.capture;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ScreenShotDb {

    static final String DATABASE = "screenshots";
    static final String C_CREATED_AT = "created_at";
    static final String C_TIMESTAMP = "timestamp";
    static final String C_FORMAT = "format";
    static final String C_DIGEST = "digest";
    static final String C_FILEPATH = "filepath";
    static final String C_WIDTH = "width";
    static final String C_HEIGHT = "height";
    static final String C_LAST_ACCESSED_AT = "last_accessed_at";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_TIMESTAMP, C_FORMAT, C_DIGEST, C_FILEPATH, C_WIDTH, C_HEIGHT, C_LAST_ACCESSED_AT};
    public final DbCaptured dbCaptured;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public ScreenShotDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbCaptured = new DbCaptured(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_TIMESTAMP).append(" TEXT")
                .append(", ").append(C_FORMAT).append(" TEXT")
                .append(", ").append(C_DIGEST).append(" TEXT")
                .append(", ").append(C_FILEPATH).append(" TEXT")
                .append(", ").append(C_WIDTH).append(" INTEGER")
                .append(", ").append(C_HEIGHT).append(" INTEGER")
                .append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbCaptured {

        final DbUtils dbUtils;
        public String FILEPATH;

        private final String TABLE = "captured";

        public DbCaptured(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String timestamp, String format, String digest, String width, String height, String filepath) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_TIMESTAMP, timestamp);
            values.put(C_FORMAT, format);
            values.put(C_DIGEST, digest);
            values.put(C_FILEPATH, filepath);
            values.put(C_WIDTH, Integer.parseInt(width));
            values.put(C_HEIGHT, Integer.parseInt(height));
            values.put(C_LAST_ACCESSED_AT, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public JSONArray getLatestRowAsJsonArray() {
            return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, null);
        }

        public int deleteSingleRowByTimestamp(String timestamp) {
            String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
            return 0;
        }

        public long updateLastAccessedAtByTimestamp(String timestamp) {
            String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
            long rightNow = (new Date()).getTime();
            this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_TIMESTAMP, timestampValue);
            return rightNow;
        }

    }

}
