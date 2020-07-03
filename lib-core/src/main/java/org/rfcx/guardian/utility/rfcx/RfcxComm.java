package org.rfcx.guardian.utility.rfcx;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentResolver;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class RfcxComm {

	private static final String logTag = RfcxLog.generateLogTag("Utils", RfcxComm.class);
	
	private static Map<String, Map<String, String[]>> initRoleFuncProj() {
		
		Map<String, Map<String, String[]>> roleFuncProj = new HashMap<String, Map<String, String[]>>();
		
		// all roles have a few pre-defined content provider definitions
		for (String role : RfcxRole.ALL_ROLES) {
			roleFuncProj.put(role, new HashMap<String, String[]>());
			roleFuncProj.get(role).put(
				"version", new String[] { "app_role", "app_version" });
			roleFuncProj.get(role).put(
				"prefs", new String[] { "pref_key", "pref_value" });
			roleFuncProj.get(role).put(
				"prefs_resync", new String[] { "pref_key", "received_at" });
			roleFuncProj.get(role).put(
				"prefs_set", new String[] { "pref_key", "pref_value", "result", "received_at" });
			roleFuncProj.get(role).put(
				"identity", new String[] { "identity_key", "identity_value" });
			roleFuncProj.get(role).put(
				"identity_set", new String[] { "identity_key", "identity_value", "received_at" });
			roleFuncProj.get(role).put(
				"identity_resync", new String[] { "identity_key", "received_at" });
			roleFuncProj.get(role).put(
				"instructions", new String[] { "instructions_json", "received_at" });
			roleFuncProj.get(role).put(
				"status", new String[] { "target", "result", "received_at" });
			roleFuncProj.get(role).put(
				"process", new String[] { "name", "pid", "uid" });
			roleFuncProj.get(role).put(
				"ping", new String[] { "sent_at" });
			roleFuncProj.get(role).put(
				"control", new String[] { "command", "result", "received_at" });
			roleFuncProj.get(role).put(
				"sms_queue", new String[] { "send_at|address|message", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database_get_all_rows", new String[] { "table", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database_get_latest_row", new String[] { "table", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database_delete_row", new String[] { "table|id", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database_delete_rows_before", new String[] { "table|timestamp", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database_set_last_accessed_at", new String[] { "table|id", "result", "received_at" });
			roleFuncProj.get(role).put(
					"configuration", new String[] { "target", "result", "received_at" });
			roleFuncProj.get(role).put(
					"diagnostic", new String[] { "target", "result", "received_at" });
		}
		return roleFuncProj;
	}
	
	public static JSONArray getQueryContentProvider(String role, String function, String query, ContentResolver contentResolver) {
		JSONArray getQueryResults = new JSONArray();
		try {
			Cursor queryCursor = contentResolver.query( getUri( role, function, query ), getProjection( role, function ), null, null, null );
			if ((queryCursor != null) && (queryCursor.getCount() > 0) && queryCursor.moveToFirst()) { do {
				getQueryResults = new JSONArray( queryCursor.getString( queryCursor.getColumnIndex("result") ) );
			} while (queryCursor.moveToNext()); queryCursor.close(); }
		} catch (JSONException e) {
			RfcxLog.logExc(logTag, e);
		}
		return getQueryResults;
	}

	public static int deleteQueryContentProvider(String role, String function, String query, ContentResolver contentResolver) {
		int deleteQueryResult = 0;
		try {
			Cursor queryCursor = contentResolver.query( getUri( role, function, query ), getProjection( role, function ), null, null, null );
			if ((queryCursor != null) && (queryCursor.getCount() > 0) && queryCursor.moveToFirst()) { do {
				deleteQueryResult = (int) Integer.parseInt( queryCursor.getString( queryCursor.getColumnIndex("result") ) );
			} while (queryCursor.moveToNext()); queryCursor.close(); }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return deleteQueryResult;
	}
	
	public static int updateQueryContentProvider(String role, String function, String query, ContentResolver contentResolver) {
		int updateQueryResult = 0;
		try {
			Cursor queryCursor = contentResolver.query( getUri( role, function, query ), getProjection( role, function ), null, null, null );
//			if ((queryCursor != null) && (queryCursor.getCount() > 0) && queryCursor.moveToFirst()) { do {
//				updateQueryResult = (int) Integer.parseInt( queryCursor.getString( queryCursor.getColumnIndex("result") ) );
//			} while (queryCursor.moveToNext()); queryCursor.close(); }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return updateQueryResult;
	}
	
	public static MatrixCursor getProjectionCursor(String role, String function, Object[] values) {
		MatrixCursor cursor = new MatrixCursor(getProjection(role, function));
		if (values != null) { cursor.addRow(values); }
		return cursor;
	}

	
	public static Uri getUri(String role, String function, String command) {
		StringBuilder uri = (new StringBuilder())
				.append("content://")
				.append(getAuthority(role.toLowerCase(Locale.US)))
				.append("/")
				.append(function.toLowerCase(Locale.US));
		if (command != null) { uri.append("/").append(command.toLowerCase(Locale.US)); }
		return Uri.parse(uri.toString());
	}
	
	public static int[] getUriMatchId(String role, String function) {
		
		Map<String, Map<String, String[]>> roleFuncProj = initRoleFuncProj();
		Map<String, Map<String, int[]>> roleFuncUriMatchId = new HashMap<String, Map<String, int[]>>();
		int uriMatchIdIterator = 0;
		
		for (String eachRole : RfcxRole.ALL_ROLES) {
			roleFuncUriMatchId.put(eachRole, new HashMap<String, int[]>());
			for (Entry roleFuncEntry : roleFuncProj.get(eachRole).entrySet()) {
				uriMatchIdIterator = uriMatchIdIterator+2;
				roleFuncUriMatchId.get(eachRole).put(roleFuncEntry.getKey().toString(), new int[] { (uriMatchIdIterator-1), uriMatchIdIterator });
			}
		}
		return roleFuncUriMatchId.get(role.toLowerCase(Locale.US)).get(function.toLowerCase(Locale.US));
	}
	
	private static UriMatcher getUriMatcher(String role, String function) {
		
		String _role = role.toLowerCase(Locale.US);
		String _function = function.toLowerCase(Locale.US);
		int[] uriMatchIds = getUriMatchId(role, function);
		
		UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		
		uriMatcher.addURI(getAuthority(_role), _function, uriMatchIds[0]);
		uriMatcher.addURI(getAuthority(_role), (new StringBuilder()).append(_function).append("/*").toString(), uriMatchIds[1]);
		
		return uriMatcher;
	}
	
	public static boolean uriMatch(Uri uri, String role, String function, String value) {
		int uriMatch = getUriMatcher(role, function).match(uri);
		int[] uriMatchIds = getUriMatchId(role, function);
		boolean valueIsIncluded = ((value != null) && (!value.isEmpty()));
		return (		!valueIsIncluded 
				&& 	(uriMatch == uriMatchIds[0])
				)
			|| 	(	valueIsIncluded 
				&& 	(uriMatch == uriMatchIds[1]) 
				&& 	(uri.getLastPathSegment().toLowerCase(Locale.US)).equals(value.toLowerCase(Locale.US))
				)
			|| 	(	valueIsIncluded 
				&& 	(uriMatch == uriMatchIds[1]) 
				&& 	value.equals("*")
				); 
	}

	public static String getAuthority(String role) {
		return (new StringBuilder()).append("org.rfcx.guardian.").append(role.toLowerCase(Locale.US)).toString();
	}
	
	public static String[] getProjection(String role, String function) {
		return initRoleFuncProj().get(role.toLowerCase(Locale.US)).get(function.toLowerCase(Locale.US));
	}
	
}
