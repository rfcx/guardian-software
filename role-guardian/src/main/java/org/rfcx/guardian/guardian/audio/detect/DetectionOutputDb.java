package org.rfcx.guardian.guardian.audio.detect;

import java.util.Date;
import java.util.List;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentValues;
import android.content.Context;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class DetectionOutputDb {

    public DetectionOutputDb(Context context, String appVersion) {
        this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
        this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
        this.dbDetectionOutput = new DbDetectionOutput(context);
    }

    private int VERSION = 1;
    static final String DATABASE = "detection";

    static final String C_CREATED_AT = "created_at";
    static final String C_FILENAME = "file_name";
    static final String C_FILEPART = "file_part";
    static final String C_OUTPUT = "file_output";

    private static final String[] ALL_COLUMNS = new String[] {  C_CREATED_AT, C_FILENAME, C_FILEPART, C_OUTPUT };

    static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { }; // "0.6.43"
    private boolean DROP_TABLE_ON_UPGRADE = false;

    private static String createColumnString(String tableName) {
        StringBuilder sbOut = new StringBuilder();
        sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_FILENAME).append(" TEXT")
                .append(", ").append(C_FILEPART).append(" TEXT")
                .append(", ").append(C_OUTPUT).append(" TEXT")
                .append(")");
        return sbOut.toString();
    }


    public class DbDetectionOutput {

        final DbUtils dbUtils;

        private String TABLE = "detection_output";

        public DbDetectionOutput(Context context) {
            this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
        }

        public int insert(String fileName, int part, String output) {

            ContentValues values = new ContentValues();
            values.put(C_CREATED_AT, (new Date()).getTime());
            values.put(C_FILENAME, fileName);
            values.put(C_FILEPART, part);
            values.put(C_OUTPUT, output);

            return this.dbUtils.insertRow(TABLE, values);
        }

        public List<String[]> getAllRows() {
            return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
        }
    }
    public final DbDetectionOutput dbDetectionOutput;

}
