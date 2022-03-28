package org.rfcx.guardian.guardian.asset.detections;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AudioDetectionDb {

    static final String DATABASE = "audio-detection";
    static final String C_CREATED_AT = "created_at";
    static final String C_CLASSIFICATION_TAG = "classification_tag";
    static final String C_CLASSIFIER_ID = "classifier_id";
    static final String C_CLASSIFIER_NAME = "classifier_name";
    static final String C_CLASSIFIER_VERSION = "classifier_version";
    static final String C_FILTER_ID = "filter_id";
    static final String C_AUDIO_ID = "audio_id";
    static final String C_BEGINS_AT = "begins_at";
    static final String C_WINDOW_SIZE = "window_size";
    static final String C_STEP_SIZE = "step_size";
    static final String C_CONFIDENCE_JSON = "confidence_json";
    static final String C_LAST_ACCESSED_AT = "last_accessed_at";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_CLASSIFICATION_TAG, C_CLASSIFIER_ID, C_CLASSIFIER_NAME, C_CLASSIFIER_VERSION, C_FILTER_ID, C_AUDIO_ID, C_BEGINS_AT, C_WINDOW_SIZE, C_STEP_SIZE, C_CONFIDENCE_JSON, C_LAST_ACCESSED_AT};
    public final DbUnfiltered dbUnfiltered;
    public final DbFiltered dbFiltered;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public AudioDetectionDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbUnfiltered = new DbUnfiltered(context);
        this.dbFiltered = new DbFiltered(context);
    }

    private String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_CLASSIFICATION_TAG).append(" TEXT")
                .append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
                .append(", ").append(C_CLASSIFIER_NAME).append(" TEXT")
                .append(", ").append(C_CLASSIFIER_VERSION).append(" TEXT")
                .append(", ").append(C_FILTER_ID).append(" TEXT")
                .append(", ").append(C_AUDIO_ID).append(" TEXT")
                .append(", ").append(C_BEGINS_AT).append(" TEXT")
                .append(", ").append(C_WINDOW_SIZE).append(" TEXT")
                .append(", ").append(C_STEP_SIZE).append(" TEXT")
                .append(", ").append(C_CONFIDENCE_JSON).append(" TEXT")
                .append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
                .append(")");
        return sbOut.toString();
    }

    public class DbUnfiltered {

        final DbUtils dbUtils;
        public String FILEPATH = "";

        private final String TABLE = "unfiltered";

        public DbUnfiltered(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String classificationTag, String classifierId, String classifierName, String classifierVersion, String filterId, String audioId, String beginsAt, String windowSize, String stepSize, String confidenceJson) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_CLASSIFICATION_TAG, classificationTag);
            values.put(C_CLASSIFIER_ID, classifierId);
            values.put(C_CLASSIFIER_NAME, classifierName);
            values.put(C_CLASSIFIER_VERSION, classifierVersion);
            values.put(C_FILTER_ID, filterId);
            values.put(C_AUDIO_ID, audioId);
            values.put(C_BEGINS_AT, beginsAt);
            values.put(C_WINDOW_SIZE, windowSize);
            values.put(C_STEP_SIZE, stepSize);
            values.put(C_CONFIDENCE_JSON, confidenceJson);
            values.put(C_LAST_ACCESSED_AT, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void deleteSingleRow(String classificationTag, String audioId) {
            this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_CLASSIFICATION_TAG, classificationTag, C_AUDIO_ID, audioId);
        }

    }

    public class DbFiltered {

        final DbUtils dbUtils;
        public String FILEPATH = "";

        private final String TABLE = "filtered";

        public DbFiltered(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
            FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
        }

        public int insert(String classificationTag, String classifierId, String classifierName, String classifierVersion, String filterId, String audioId, long beginsAt, long windowSize, long stepSize, String confidenceJson) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_CLASSIFICATION_TAG, classificationTag);
            values.put(C_CLASSIFIER_ID, classifierId);
            values.put(C_CLASSIFIER_NAME, classifierName);
            values.put(C_CLASSIFIER_VERSION, classifierVersion);
            values.put(C_FILTER_ID, filterId);
            values.put(C_AUDIO_ID, audioId);
            values.put(C_BEGINS_AT, beginsAt);
            values.put(C_WINDOW_SIZE, windowSize);
            values.put(C_STEP_SIZE, stepSize);
            values.put(C_CONFIDENCE_JSON, confidenceJson);
            values.put(C_LAST_ACCESSED_AT, 0);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public List<String[]> getLatestRowsNotAccessedSinceWithLimit(long notAccessedSince, int maxRows) {
            return this.dbUtils.getRowsWithNumericColumnHigherOrLowerThan(TABLE, ALL_COLUMNS, C_LAST_ACCESSED_AT, notAccessedSince, true, C_CREATED_AT, maxRows);
        }

        public List<String[]> getLatestRowsNotAccessedWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, C_LAST_ACCESSED_AT + " = ?", new String[]{"0"}, C_CREATED_AT, 0, maxRows);
        }

        public List<String[]> getLatestRowsWithLimit(int maxRows) {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, maxRows);
        }

        public long updateLastAccessedAtByCreatedAt(String createdAt) {
            long rightNow = (new Date()).getTime();
            this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_CREATED_AT, createdAt);
            return rightNow;
        }

        public void deleteSingleRow(String classificationTag, String audioId) {
            this.dbUtils.deleteRowsWithinQueryByTwoColumns(TABLE, C_CLASSIFICATION_TAG, classificationTag, C_AUDIO_ID, audioId);
        }

        public String getSimplifiedConcatRows() {
            String concatRows = null;
            ArrayList<String> rowList = new ArrayList<>();
            try {
                for (String[] row : getAllRows()) {
                    rowList.add(TextUtils.join("*", new String[]{row[1], row[3] + "-v" + row[4], row[7], "" + Math.round(Double.parseDouble(row[8]) * 1000), row[10]}));
                }
                concatRows = (rowList.size() > 0) ? TextUtils.join("|", rowList) : null;
            } catch (Exception e) {
                RfcxLog.logExc(RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "AudioDetectionDb"), e);
            }
            return concatRows;
        }

        public void clearRowsBefore(Date date) {
            this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
        }

    }


}
