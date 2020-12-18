package org.rfcx.guardian.guardian.api.methods.segment;

import android.content.ContentValues;
import android.content.Context;

import org.rfcx.guardian.utility.database.DbUtils;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.Date;
import java.util.List;

public class ApiSegmentDb {

	public ApiSegmentDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbGroups = new DbGroups(context);
		this.dbReceived = new DbReceived(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "segments";
	static final String C_CREATED_AT = "created_at";
	static final String C_GROUP_ID = "group_id";
	static final String C_SEGMENT_ID_OR_COUNT = "segment_id";
	static final String C_BODY_OR_CHECKSUM = "body";
	static final String C_PROTOCOL = "protocol";
	static final String C_TYPE = "type";
	static final String C_ATTEMPTS = "attempts";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	public static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_GROUP_ID, C_SEGMENT_ID_OR_COUNT, C_BODY_OR_CHECKSUM, C_PROTOCOL, C_TYPE, C_ATTEMPTS, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] {  }; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = false;
	
	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
			.append("(").append(C_CREATED_AT).append(" INTEGER")
			.append(", ").append(C_GROUP_ID).append(" TEXT")
			.append(", ").append(C_SEGMENT_ID_OR_COUNT).append(" TEXT")
			.append(", ").append(C_BODY_OR_CHECKSUM).append(" TEXT")
			.append(", ").append(C_PROTOCOL).append(" TEXT")
			.append(", ").append(C_TYPE).append(" TEXT")
			.append(", ").append(C_ATTEMPTS).append(" INTEGER")
			.append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
			.append(")");
		return sbOut.toString();
	}

	public class DbGroups {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "groups";

		public DbGroups(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(String groupId, int segmentCount, String fullMsgChecksum, String fullMsgType, String protocol) {
			return insert(groupId, ""+segmentCount, fullMsgChecksum, fullMsgType, protocol);
		}

		public int insert(String groupId, String segmentCount, String fullMsgChecksum, String fullMsgType, String protocol) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_GROUP_ID, groupId);
			values.put(C_SEGMENT_ID_OR_COUNT, segmentCount);
			values.put(C_BODY_OR_CHECKSUM, fullMsgChecksum);
			values.put(C_PROTOCOL, protocol);
			values.put(C_TYPE, fullMsgType);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT);
		}

		public String[] getSingleRowById(String groupId) {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, "substr("+C_GROUP_ID+",1,"+groupId.length()+") = ?", new String[] { groupId }, C_CREATED_AT, 0);
		}

		public int deleteSingleRowById(String groupId) {
			this.dbUtils.deleteRowsWithinQueryByTimestamp(TABLE, C_GROUP_ID, groupId);
			return 0;
		}

		public long updateLastAccessedAt(String groupId) {
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTimestamp(TABLE, C_LAST_ACCESSED_AT, rightNow, C_GROUP_ID, groupId);
			return rightNow;
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

	}
	public final DbGroups dbGroups;


	public class DbReceived {

		final DbUtils dbUtils;
		public String FILEPATH;

		private String TABLE = "received";

		public DbReceived(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
			FILEPATH = DbUtils.getDbFilePath(context, DATABASE, TABLE);
		}

		public int insert(String groupId, int segmentId, String segmentBody) {
			return insert(groupId, segmentId+"", segmentBody);
		}

		public int insert(String groupId, String segmentId, String segmentBody) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_GROUP_ID, groupId);
			values.put(C_SEGMENT_ID_OR_COUNT, segmentId);
			values.put(C_BODY_OR_CHECKSUM, segmentBody);
		//	values.put(C_PROTOCOL, protocol);
		//	values.put(C_TYPE, protocol);
			values.put(C_ATTEMPTS, 0);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllSegmentsForGroup(String groupId) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, "substr("+C_GROUP_ID+",1,"+ApiSegmentUtils.GROUP_ID_LENGTH+") = ?", new String[] { groupId }, C_SEGMENT_ID_OR_COUNT);
		}

		public String[] getSegmentByGroupAndId(String groupId, int segmentId) {
			String[] rtrnRow = DbUtils.placeHolderStringArray(ALL_COLUMNS.length);
			for (String[] segmentRow : getAllSegmentsForGroup(groupId)) {
				if (segmentRow[2].equalsIgnoreCase(""+segmentId)) {
					rtrnRow = segmentRow;
					break;
				}
			}
			return rtrnRow;
		}

		public int deleteSegmentsForGroup(String groupId) {
			this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_GROUP_ID, groupId);
			return 0;
		}

		public long updateLastAccessedAt(String groupId, String segmentId) {
			long rightNow = (new Date()).getTime();
			this.dbUtils.setDatetimeColumnValuesWithinQueryByTwoColumns(TABLE, C_LAST_ACCESSED_AT, rightNow, C_GROUP_ID, groupId, C_SEGMENT_ID_OR_COUNT, segmentId);
			return rightNow;
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

	}
	public final DbReceived dbReceived;

	
}
