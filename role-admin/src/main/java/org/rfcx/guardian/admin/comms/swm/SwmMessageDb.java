package org.rfcx.guardian.admin.comms.swm;

import android.content.ContentValues;
import android.content.Context;

import org.json.JSONArray;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DbUtils;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SwmMessageDb {

	public SwmMessageDb(Context context, String appVersion) {
		this.VERSION = RfcxRole.getRoleVersionValue(appVersion);
		this.DROP_TABLE_ON_UPGRADE = ArrayUtils.doesStringArrayContainString(DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS, appVersion);
		this.dbSwmSent = new DbSwmSent(context);
		this.dbSwmQueued = new DbSwmQueued(context);
	}

	private int VERSION = 1;
	static final String DATABASE = "swm";
	static final String C_CREATED_AT = "created_at";
	static final String C_TIMESTAMP = "timestamp";
	static final String C_ADDRESS = "address";
	static final String C_BODY = "body";
	static final String C_GROUP_ID = "group_id";
	static final String C_MESSAGE_ID = "message_id";
	static final String C_SWM_MESSAGE_ID = "swm_message_id";
	static final String C_LAST_ACCESSED_AT = "last_accessed_at";
	private static final String[] ALL_COLUMNS = new String[] { C_CREATED_AT, C_TIMESTAMP, C_ADDRESS, C_BODY, C_GROUP_ID, C_MESSAGE_ID, C_SWM_MESSAGE_ID, C_LAST_ACCESSED_AT };

	static final String[] DROP_TABLES_ON_UPGRADE_TO_THESE_VERSIONS = new String[] { "0.9.1", "0.9.2", "0.9.3"}; // "0.6.43"
	private boolean DROP_TABLE_ON_UPGRADE = true;

	private String createColumnString(String tableName) {
		StringBuilder sbOut = new StringBuilder();
		sbOut.append("CREATE TABLE ").append(tableName)
                .append("(").append(C_CREATED_AT).append(" INTEGER")
                .append(", ").append(C_TIMESTAMP).append(" TEXT")
                .append(", ").append(C_ADDRESS).append(" TEXT")
                .append(", ").append(C_BODY).append(" TEXT")
                .append(", ").append(C_GROUP_ID).append(" TEXT")
                .append(", ").append(C_MESSAGE_ID).append(" TEXT")
                .append(", ").append(C_SWM_MESSAGE_ID).append(" TEXT")
                .append(", ").append(C_LAST_ACCESSED_AT).append(" INTEGER")
                .append(")");
		return sbOut.toString();
	}

	public class DbSwmSent {

		final DbUtils dbUtils;

		private String TABLE = "sent";

		public DbSwmSent(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(long timestamp, String address, String body, String groupId, String message_id, String swmMessageId) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp+"");
			values.put(C_ADDRESS, address);
			values.put(C_BODY, body);
			values.put(C_GROUP_ID, groupId);
			values.put(C_MESSAGE_ID, message_id);
			values.put(C_SWM_MESSAGE_ID, swmMessageId);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public JSONArray getSingleRowAsJsonArray() {
			return this.dbUtils.getRowsAsJsonArray(TABLE, ALL_COLUMNS, null, null, C_CREATED_AT, 0, 1);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public List<String[]> getRowsInOrderOfTimestamp() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_TIMESTAMP+" ASC");
		}

	}
	public final DbSwmSent dbSwmSent;

	public class DbSwmQueued {

		final DbUtils dbUtils;

		private String TABLE = "queued";

		public DbSwmQueued(Context context) {
			this.dbUtils = new DbUtils(context, DATABASE, TABLE, VERSION, createColumnString(TABLE), DROP_TABLE_ON_UPGRADE);
		}

		public int insert(long timestamp, String address, String body, String groupId, String message_id) {

			ContentValues values = new ContentValues();
			values.put(C_CREATED_AT, (new Date()).getTime());
			values.put(C_TIMESTAMP, timestamp+"");
			values.put(C_ADDRESS, address);
			values.put(C_BODY, body);
			values.put(C_GROUP_ID, groupId);
			values.put(C_MESSAGE_ID, message_id);
			values.put(C_LAST_ACCESSED_AT, 0);

			return this.dbUtils.insertRow(TABLE, values);
		}

		public List<String[]> getAllRows() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, null);
		}

		public int getCount() {
			return this.dbUtils.getCount(TABLE, null, null);
		}

		public List<String[]> getRowsInOrderOfTimestamp() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, null, null, C_TIMESTAMP+" ASC");
		}

		public List<String[]> getUnsentMessagesToSwarmInOrderOfTimestamp() {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, C_SWM_MESSAGE_ID + " IS NULL", null, C_TIMESTAMP+" ASC");
		}

		public List<String[]> getUnsentMessagesInOrderOfTimestampAndWithinGroupId(String groupId) {
			return this.dbUtils.getRows(TABLE, ALL_COLUMNS, "substr("+C_GROUP_ID+",1,4) = '" + groupId + "'", null, C_TIMESTAMP+" ASC");
		}

		public String[] getLatestRow() {
			return this.dbUtils.getSingleRow(TABLE, ALL_COLUMNS, null, null, C_TIMESTAMP+" DESC", 0);
		}

		public ArrayList<String> getGroupIdsBefore(Date date) {
			List<String[]> result = this.dbUtils.getRows(TABLE, new String[] { C_GROUP_ID }, C_CREATED_AT + "<=" + date.getTime() + " GROUP BY " + C_GROUP_ID, null, null);
			ArrayList<String> groupIds = new ArrayList<>();
			for (String[] item: result) {
				groupIds.add(item[0]);
			}
			return groupIds;
		}

		public void clearRowsBefore(Date date) {
			this.dbUtils.deleteRowsOlderThan(TABLE, C_CREATED_AT, date);
		}

		public void clearRowsByIds(List<String> ids) {
			this.dbUtils.deleteRowsWithinValuesByOneColumn(TABLE, C_MESSAGE_ID, ids.toArray(new String[0]));
		}

		public int deleteSingleRowByMessageId(String message_id) {
			this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_MESSAGE_ID, message_id);
			return 0;
		}

        public int deleteSingleRowBySwmMessageId(String swmMessageId) {
			this.dbUtils.deleteRowsWithinQueryByOneColumn(TABLE, C_SWM_MESSAGE_ID, swmMessageId);
			return 0;
        }

        public void updateSwmMessageIdByMessageId(String messageId, String swmMessageId) {
			this.dbUtils.updateRowByColumn(TABLE, C_SWM_MESSAGE_ID, swmMessageId, C_MESSAGE_ID, messageId);
		}

	}
	public final DbSwmQueued dbSwmQueued;

}
