package org.rfcx.guardian.guardian.asset;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AssetExchangeLogDb {

    static final String DATABASE = "asset-exchange-log";
    static final String C_CREATED_AT = "created_at";
    static final String C_ASSET_TYPE = "asset_type";
    static final String C_TIMESTAMP = "timestamp";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_ASSET_TYPE, C_TIMESTAMP};
    public final DbPurged dbPurged;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public AssetExchangeLogDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbPurged = new DbPurged(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_ASSET_TYPE).append(" TEXT")
                .append(", ").append(C_TIMESTAMP).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbPurged {

        final DbUtils dbUtils;
        public String FILEPATH = "";

        private final String TABLE = "purged";

        public DbPurged(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String asset_type, String timestamp) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_ASSET_TYPE, asset_type);
            values.put(C_TIMESTAMP, timestamp);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getLatestRowsWithLimitExcludeCreatedAt(int maxRows) {
            List<String[]> rowsWithAllFields = this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
            List<String[]> rowsWithoutCreatedAt = new ArrayList<String[]>();
            for (String[] singleRow : rowsWithAllFields) {
                rowsWithoutCreatedAt.add(new String[]{singleRow[1], singleRow[2]});
            }
            return rowsWithoutCreatedAt;
        }

        public int deleteSingleRowByTimestamp(String timestamp) {
            String timestampValue = timestamp.contains(".") ? timestamp.substring(0, timestamp.lastIndexOf(".")) : timestamp;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_TIMESTAMP, timestampValue);
            return 0;
        }

    }


}
