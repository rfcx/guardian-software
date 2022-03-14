package org.rfcx.guardian.guardian.api.methods.checkin;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiCheckInDb {

    static final String DATABASE = "checkin";
    static final String C_CREATED_AT = "created_at";
    static final String C_AUDIO = "audio";
    static final String C_JSON = "json";
    static final String C_ATTEMPTS = "attempts";
    static final String C_FILEPATH = "filepath";
    static final String C_AUDIO_DURATION = "audio_duration";
    static final String C_AUDIO_FILESIZE = "audio_filesize";
    public static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_AUDIO, C_JSON, C_ATTEMPTS, C_FILEPATH, C_AUDIO_DURATION, C_AUDIO_FILESIZE};
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    public final DbQueued dbQueued;
    public final DbSkipped dbSkipped;
    public final DbStashed dbStashed;
    public final DbSent dbSent;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public ApiCheckInDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbQueued = new DbQueued(context);
        this.dbSent = new DbSent(context);
        this.dbSkipped = new DbSkipped(context);
        this.dbStashed = new DbStashed(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_AUDIO).append(" TEXT")
                .append(", ").append(C_JSON).append(" TEXT")
                .append(", ").append(C_ATTEMPTS).append(" TEXT")
                .append(", ").append(C_FILEPATH).append(" TEXT")
                .append(", ").append(C_AUDIO_DURATION).append(" INTEGER")
                .append(", ").append(C_AUDIO_FILESIZE).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbQueued {

        final DbUtils dbUtils;

        private final String TABLE = "queued";

        public DbQueued(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String audio, String json, String attempts, String filepath, String audioDuration, String audioFileSize) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_AUDIO, audio);
            values.put(C_JSON, json);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_FILEPATH, filepath);
            values.put(C_AUDIO_DURATION, Long.parseLong(audioDuration));
            values.put(C_AUDIO_FILESIZE, Long.parseLong(audioFileSize));

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
        }

        public List<String[]> getLatestRowsWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

        public void deleteSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
        }

        public String[] getSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr(" + C_AUDIO + ",1," + timestamp.length() + ") = ?", new String[]{timestamp}, null, 0);
        }

        public void incrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public void decrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public void deleteAllRows() {
            this.dbUtils.deleteAllRows(TABLE);
        }

        public long getCumulativeFileSizeForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_FILESIZE, null, null);
        }

        public long getCumulativeDurationForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_DURATION, null, null);
        }

    }

    public class DbSkipped {

        final DbUtils dbUtils;

        private final String TABLE = "skipped";

        public DbSkipped(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String created_at, String audio, String json, String attempts, String filepath, String audioDuration, String audioFileSize) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, created_at);
            values.put(C_AUDIO, audio);
            values.put(C_JSON, json);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_FILEPATH, filepath);
            values.put(C_AUDIO_DURATION, Long.parseLong(audioDuration));
            values.put(C_AUDIO_FILESIZE, Long.parseLong(audioFileSize));

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
        }

        public List<String[]> getLatestRowsWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

        public void deleteSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
        }

        public String[] getSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr(" + C_AUDIO + ",1," + timestamp.length() + ") = ?", new String[]{timestamp}, null, 0);
        }

        public void incrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public void decrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public void deleteAllRows() {
            this.dbUtils.deleteAllRows(TABLE);
        }

        public long getCumulativeFileSizeForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_FILESIZE, null, null);
        }

        public long getCumulativeDurationForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_DURATION, null, null);
        }

    }

    public class DbStashed {

        final DbUtils dbUtils;

        private final String TABLE = "stashed";

        public DbStashed(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String audio, String json, String attempts, String filepath, String audioDuration, String audioFileSize) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
            values.put(C_AUDIO, audio);
            values.put(C_JSON, json);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_FILEPATH, filepath);
            values.put(C_AUDIO_DURATION, Long.parseLong(audioDuration));
            values.put(C_AUDIO_FILESIZE, Long.parseLong(audioFileSize));

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
        }

        public List<String[]> getLatestRowsWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

        public void deleteSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
        }

        public String[] getSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr(" + C_AUDIO + ",1," + timestamp.length() + ") = ?", new String[]{timestamp}, null, 0);
        }

        public void incrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public void decrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public void deleteAllRows() {
            this.dbUtils.deleteAllRows(TABLE);
        }

        public long getCumulativeFileSizeForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_FILESIZE, null, null);
        }

        public long getCumulativeDurationForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_DURATION, null, null);
        }

    }

    public class DbSent {

        final DbUtils dbUtils;

        private final String TABLE = "sent";

        public DbSent(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String audio, String json, String attempts, String filepath, String audioDuration, String audioFileSize) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, DateTimeUtils.getDateTime());
            values.put(C_AUDIO, audio);
            values.put(C_JSON, json);
            values.put(C_ATTEMPTS, attempts);
            values.put(C_FILEPATH, filepath);
            values.put(C_AUDIO_DURATION, Long.parseLong(audioDuration));
            values.put(C_AUDIO_FILESIZE, Long.parseLong(audioFileSize));

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getRowsWithOffset(int rowOffset, int rowLimit) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, rowOffset, rowLimit);
        }

        public JSONArray getSingleRowAsJsonArray() {
            return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, 1);
        }

        public String[] getLatestRow() {
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0);
        }

        public List<String[]> getLatestRowsWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

        public void deleteSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_AUDIO, timestamp);
        }

        public String[] getSingleRowByAudioAttachmentId(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr(" + C_AUDIO + ",1," + timestamp.length() + ") = ?", new String[]{timestamp}, null, 0);
        }

        public void incrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("+1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public void decrementSingleRowAttempts(String audioId) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.adjustNumericColumnValuesWithinQueryByTimestamp("-1", TABLE, C_ATTEMPTS, C_AUDIO, timestamp);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public void deleteAllRows() {
            this.dbUtils.deleteAllRows(TABLE);
        }

        public long getCumulativeFileSizeForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_FILESIZE, null, null);
        }

        public long getCumulativeDurationForAllRows() {
            return this.dbUtils.getSumOfColumn(TABLE, C_AUDIO_DURATION, null, null);
        }

        public void updateFilePathByAudioAttachmentId(String audioId, String filePath) {
            String timestamp = audioId.contains(".") ? audioId.substring(0, audioId.lastIndexOf(".")) : audioId;
            this.dbUtils.updateStringColumnValuesWithinQueryByTimestamp(TABLE, C_FILEPATH, filePath, C_AUDIO, timestamp);
        }

    }

}
