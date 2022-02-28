package org.rfcx.guardian.guardian.api.methods.download;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AssetDownloadDb {

    static final String DATABASE = "downloads";
    static final String C_CREATED_AT = "created_at";
    static final String C_ASSET_TYPE = "asset_type";
    static final String C_ASSET_ID = "asset_id";
    static final String C_PAYLOAD_CHECKSUM = "checksum";
    static final String C_PROTOCOL = "protocol";
    static final String C_URI = "uri";
    static final String C_FILESIZE = "filesize";
    static final String C_FILETYPE = "filetype";
    static final String C_META_JSON_BLOB = "meta_json_blob";
    static final String C_ATTEMPTS = "attempts";
    static final String C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION = "last_accessed_at";
    public static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_ASSET_TYPE, C_ASSET_ID, C_PAYLOAD_CHECKSUM, C_PROTOCOL, C_URI, C_FILESIZE, C_FILETYPE, C_META_JSON_BLOB, C_ATTEMPTS, C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION};
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    public final DbQueued dbQueued;
    public final DbCompleted dbCompleted;
    public final DbSkipped dbSkipped;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public AssetDownloadDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true;//ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbQueued = new DbQueued(context);
        this.dbCompleted = new DbCompleted(context);
        this.dbSkipped = new DbSkipped(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_ASSET_TYPE).append(" TEXT")
                .append(", ").append(C_ASSET_ID).append(" TEXT")
                .append(", ").append(C_PAYLOAD_CHECKSUM).append(" TEXT")
                .append(", ").append(C_PROTOCOL).append(" TEXT")
                .append(", ").append(C_URI).append(" TEXT")
                .append(", ").append(C_FILESIZE).append(" INTEGER")
                .append(", ").append(C_FILETYPE).append(" TEXT")
                .append(", ").append(C_META_JSON_BLOB).append(" TEXT")
                .append(", ").append(C_ATTEMPTS).append(" INTEGER")
                .append(", ").append(C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbQueued {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "queued";

        public DbQueued(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String assetType, String assetId, String checksum, String protocol, String uri, long fileSize, String fileType, String metaJsonBlob) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_ASSET_TYPE, assetType);
            values.put(C_ASSET_ID, assetId);
            values.put(C_PAYLOAD_CHECKSUM, checksum);
            values.put(C_PROTOCOL, protocol);
            values.put(C_URI, uri);
            values.put(C_FILESIZE, fileSize);
            values.put(C_FILETYPE, fileType);
            values.put(C_META_JSON_BLOB, metaJsonBlob);
            values.put(C_ATTEMPTS, 0);
            values.put(C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public String[] getByAssetId(String id) {
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr(" + C_ASSET_ID + ",1," + id.length() + ") = ?", new String[]{id}, C_CREATED_AT, 0);
        }

        public void deleteSingleRow(String assetType, String assetId) {
            this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

        public void incrementSingleRowAttempts(String assetType, String assetId) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTwoColumns("+1", TABLE, C_ATTEMPTS, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

        public long updateLastAccessedAt(String assetType, String assetId) {
            long rightNow = (new Date()).getTime();
            this.dbUtils.setDatetimeColumnValuesWithinQueryByTwoColumns(TABLE, C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION, rightNow, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
            return rightNow;
        }

    }

    public class DbCompleted {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "completed";

        public DbCompleted(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String assetType, String assetId, String checksum, String protocol, String uri, long fileSize, String fileType, String metaJsonBlob, int attempts, long downloadDuration) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_ASSET_TYPE, assetType);
            values.put(C_ASSET_ID, assetId);
            values.put(C_PAYLOAD_CHECKSUM, checksum);
            values.put(C_PROTOCOL, protocol);
            values.put(C_URI, uri);
            values.put(C_FILESIZE, fileSize);
            values.put(C_FILETYPE, fileType);
            values.put(C_META_JSON_BLOB, metaJsonBlob);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION, downloadDuration);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void deleteSingleRow(String assetType, String assetId) {
            this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

        public void incrementSingleRowAttempts(String assetType, String assetId) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTwoColumns("+1", TABLE, C_ATTEMPTS, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

    }

    public class DbSkipped {

        final DbUtils dbUtils;
        public String FILEPATH;

        private String TABLE = "skipped";

        public DbSkipped(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String assetType, String assetId, String checksum, String protocol, String uri, long fileSize, String fileType, String metaJsonBlob, int attempts, long lastAccessedAt) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_ASSET_TYPE, assetType);
            values.put(C_ASSET_ID, assetId);
            values.put(C_PAYLOAD_CHECKSUM, checksum);
            values.put(C_PROTOCOL, protocol);
            values.put(C_URI, uri);
            values.put(C_FILESIZE, fileSize);
            values.put(C_FILETYPE, fileType);
            values.put(C_META_JSON_BLOB, metaJsonBlob);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_LAST_ACCESSED_AT_OR_DOWNLOAD_DURATION, lastAccessedAt);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void deleteSingleRow(String assetType, String assetId) {
            this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

        public void incrementSingleRowAttempts(String assetType, String assetId) {
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTwoColumns("+1", TABLE, C_ATTEMPTS, C_ASSET_TYPE, assetType, C_ASSET_ID, assetId);
        }

    }
}
