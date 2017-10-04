package org.rfcx.guardian.utility.rfcx;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import android.content.UriMatcher;
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
				"control", new String[] { "command", "result", "received_at" });
			roleFuncProj.get(role).put(
				"database", new String[] { "table", "query", "result", "received_at" });
		}
		return roleFuncProj;
	}

	public static MatrixCursor getProjectionCursor(String role, String function, Object[] values) {
		MatrixCursor cursor = new MatrixCursor(getProjection(role, function));
		cursor.addRow(values);
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
	
	public static boolean uriMatch(Uri uri, String role, String function, String command) {
		int uriMatch = getUriMatcher(role, function).match(uri);
		int[] uriMatchIds = getUriMatchId(role, function);
		boolean commandIsIncluded = ((command != null) && (!command.isEmpty()));
		return (		!commandIsIncluded 
				&& 	(uriMatch == uriMatchIds[0])
				)
			|| 	(	commandIsIncluded 
				&& 	(uriMatch == uriMatchIds[1]) 
				&& 	(uri.getLastPathSegment().toLowerCase(Locale.US)).equals(command.toLowerCase(Locale.US))
				); 
	}

	public static String getAuthority(String role) {
		return (new StringBuilder()).append("org.rfcx.guardian.").append(role.toLowerCase(Locale.US)).toString();
	}
	
	public static String[] getProjection(String role, String function) {
		return initRoleFuncProj().get(role.toLowerCase(Locale.US)).get(function.toLowerCase(Locale.US));
	}
	
}
