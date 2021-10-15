package org.rfcx.guardian.classify.utils;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioDetectionDb {

    public AudioDetectionDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbQueued = new DbQueued(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "audio-detection";
    static final String C_CREATED_AT = "created_at";
    static final String C_DETECTION_JSON = "detection_json";
    static final String C_LAST_ACCESSED_AT = "last_accessed_at";
    static final String C_ATTEMPTS = "attempts";

    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_DETECTION_JSON, C_LAST_ACCESSED_AT, C_ATTEMPTS};

    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private boolean DROP_TABLE_ON_UPGRADE = false;

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_DETECTION_JSON).append(" TEXT")
                .append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
                .append(", ").append(C_ATTEMPTS).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbQueued {

        final DbUtils dbUtils;
        public String FILEPATH = "";

        private String TABLE = "queued";

        public DbQueued(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String detectionJson) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_DETECTION_JSON, detectionJson);
            values.put(C_LAST_ACCESSED_AT, 0);
            values.put(C_ATTEMPTS, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void deleteSingleRow(String createdAt) {
            this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_CREATED_AT, createdAt);
        }

        public void incrementSingleRowAttempts(String createdAt) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByOneColumn("+1", TABLE, C_ATTEMPTS, C_CREATED_AT, createdAt);
        }

    }

    public final DbQueued dbQueued;


}
