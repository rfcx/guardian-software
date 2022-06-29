package org.rfcx.guardian.guardian.asset.classifier;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class AudioClassifierDb {

    static final String DATABASE = "audio-classifier";
    static final String C_CREATED_AT = "created_at";
    static final String C_CLASSIFIER_ID = "classifier_id";
    static final String C_CLASSIFIER_NAME = "classifier_name";
    static final String C_CLASSIFIER_VERSION = "version";
    static final String C_FORMAT = "format";
    static final String C_DIGEST = "digest";
    static final String C_FILEPATH = "filepath";
    static final String C_INPUT_SAMPLE_RATE = "input_sample_rate";
    static final String C_INPUT_GAIN = "input_gain";
    static final String C_WINDOW_SIZE = "window_size";
    static final String C_STEP_SIZE = "step_size";
    static final String C_CLASSES = "classes";
    static final String C_THRESHOLD = "threshold";
    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[]{"0.6.81"}; // "0.6.43"
    private static final String[] ALL_COLUMNS = new String[]{C_CREATED_AT, C_CLASSIFIER_ID, C_CLASSIFIER_NAME, C_CLASSIFIER_VERSION, C_FORMAT, C_DIGEST, C_FILEPATH, C_INPUT_SAMPLE_RATE, C_INPUT_GAIN, C_WINDOW_SIZE, C_STEP_SIZE, C_CLASSES, C_THRESHOLD};
    public final DbActive dbActive;
    private int VERSION = 1;
    private boolean DROP_TABLE_ON_UPGRADE = false;

    public AudioClassifierDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = true; //ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbActive = new DbActive(context);
    }

    private static String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_CLASSIFIER_ID).append(" TEXT")
                .append(", ").append(C_CLASSIFIER_NAME).append(" TEXT")
                .append(", ").append(C_CLASSIFIER_VERSION).append(" TEXT")
                .append(", ").append(C_FORMAT).append(" TEXT")
                .append(", ").append(C_DIGEST).append(" TEXT")
                .append(", ").append(C_FILEPATH).append(" TEXT")
                .append(", ").append(C_INPUT_SAMPLE_RATE).append(" INTEGER")
                .append(", ").append(C_INPUT_GAIN).append(" TEXT")
                .append(", ").append(C_WINDOW_SIZE).append(" TEXT")
                .append(", ").append(C_STEP_SIZE).append(" TEXT")
                .append(", ").append(C_CLASSES).append(" TEXT")
                .append(", ").append(C_THRESHOLD).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }

    public class DbActive {

        final DbUtils dbUtils;

        private final String TABLE = "active";

        public DbActive(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String classifierId, String classifierName, String version, String format, String digest, String filepath, int inputSampleRate, double inputGain, String windowSize, String stepSize, String classes, String threshold) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_CLASSIFIER_ID, classifierId);
            values.put(C_CLASSIFIER_NAME, classifierName);
            values.put(C_CLASSIFIER_VERSION, version);
            values.put(C_FORMAT, format);
            values.put(C_DIGEST, digest);
            values.put(C_FILEPATH, filepath);
            values.put(C_INPUT_SAMPLE_RATE, inputSampleRate);
            values.put(C_INPUT_GAIN, inputGain);
            values.put(C_WINDOW_SIZE, windowSize);
            values.put(C_STEP_SIZE, stepSize);
            values.put(C_CLASSES, classes);
            values.put(C_THRESHOLD, threshold);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }

        public void deleteSingleRow(String assetId) {
            this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_CLASSIFIER_ID, assetId);
        }

        public int getCount() {
            return this.dbUtils.getCount(TABLE, null, null);
        }

        public int getCountByAssetId(String assetId) {
            return this.dbUtils.getCount(TABLE, C_CLASSIFIER_ID + "=?", new String[]{assetId});
        }

        public int getMaxSampleRateAmongstAllRows() {
            return (int) this.dbUtils.getMaxValueOfColumn(TABLE, C_INPUT_SAMPLE_RATE, null, null);
        }
    }


}
