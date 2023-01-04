package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiCheckInArchiveDb {

    static final String DATABASE = "checkin";
    static final String C_ARCHIVED_AT = "archived_at";
    static final String C_ARCHIVE_BEGINS_AT = "archive_begins_at";
    static final String C_ARCHIVE_ENDS_AT = "archive_ends_at";
    static final String C_RECORD_COUNT = "record_count";
    static final String C_DURATION_SETTING = "duration_setting";
    static final String C_SKIP_SETTING = "skip_setting";
    static final String C_FILESIZE = "filesize";
    static final String C_FILEPATH = "filepath";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{ "1.1.8" }; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_ARCHIVED_AT, C_ARCHIVE_BEGINS_AT, C_ARCHIVE_ENDS_AT, C_RECORD_COUNT, C_DURATION_SETTING, C_SKIP_SETTING, C_FILESIZE, C_FILEPATH};
    public final DbArchive dbArchive;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public ApiCheckInArchiveDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbArchive = new DbArchive(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_ARCHIVED_AT).append(" INTEGER")
                .append(", ").append(C_ARCHIVE_BEGINS_AT).append(" INTEGER")
                .append(", ").append(C_ARCHIVE_ENDS_AT).append(" INTEGER")
                .append(", ").append(C_RECORD_COUNT).append(" INTEGER")
                .append(", ").append(C_DURATION_SETTING).append(" INTEGER")
                .append(", ").append(C_SKIP_SETTING).append(" INTEGER")
                .append(", ").append(C_FILESIZE).append(" INTEGER")
                .append(", ").append(C_FILEPATH).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbArchive {

        final DbUtils dbUtils;

        private final String TABLE = "archive";

        public DbArchive(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(Date archived_at, Date archive_begins_at, Date archive_ends_at, int record_count, int duration, int skip, long filesize, String filepath) {

            ContentValues values = new ContentValues();
            values.put(C_ARCHIVED_AT, archived_at.getTime());
            values.put(C_ARCHIVE_BEGINS_AT, archive_begins_at.getTime());
            values.put(C_ARCHIVE_ENDS_AT, archive_ends_at.getTime());
            values.put(C_RECORD_COUNT, record_count);
            values.put(C_DURATION_SETTING, duration);
            values.put(C_SKIP_SETTING, skip);
            values.put(C_FILESIZE, filesize);
            values.put(C_FILEPATH, filepath);

            return this.dbUtils.insertRow(TABLE, values);
        }

        private List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_ARCHIVED_AT, date);
        }

        public String getConcatRows() {
            return DbUtils.getConcatRows(getAllRows());
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public long getInnerRecordCumulativeCount() {
            return this.dbUtils.getSumOfColumn(TABLE, C_RECORD_COUNT, null, null);
        }

        public long getCumulativeFileSizeForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_FILESIZE, null, null);
        }


    }

}
