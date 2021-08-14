package org.rfcx.guardian.utility.rfcx;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.rfcx.guardian.utility.asset.RfcxAssetCleanup;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.FileUtils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

public class RfcxComm {

	private static final String logTag = RfcxLog.generateLogTag("Utils", RfcxComm.class);

	public static final class TCP_PORTS {

		public static final class ADMIN {
			public static final int ADB = 7329;
			public static final int SSH = 22;
			public static final class SOCKET {
				public static final int JSON = 9998;
			}
		}

		public static final class GUARDIAN {
			public static final class SOCKET {
				public static final int JSON = 9999;
				public static final int CAST = 9997;
			}
		}
	}

	public static final String fileProviderAssetDirUriNamespacePrepend = "/files_";
	
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
				"prefs_set", new String[] { "pref_key", "pref_value", "received_at" });
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
				"keycode", new String[] { "keycode_command", "received_at" });
//			roleFuncProj.get(role).put(
//				"system_settings_set", new String[] { "group key", "value", "received_at" });
			roleFuncProj.get(role).put(
				"clock_set", new String[] { "command", "value", "received_at" });
			roleFuncProj.get(role).put(
				"gpio_set", new String[] { "address", "value", "received_at" });
			roleFuncProj.get(role).put(
				"gpio_get", new String[] { "address", "value", "received_at" });
			roleFuncProj.get(role).put(
				"control", new String[] { "command", "result", "received_at" });
			roleFuncProj.get(role).put(
				"classify_queue", new String[] { "audio_id|classifier_id", "result", "received_at" });
			roleFuncProj.get(role).put(
				"detections_create", new String[] { "detections", "result", "received_at" });
			roleFuncProj.get(role).put(
				"sms_queue", new String[] { "send_at|address|message", "result", "received_at" });
			roleFuncProj.get(role).put(
				"sbd_queue", new String[] { "send_at|address|message", "result", "received_at" });
			roleFuncProj.get(role).put(
				"swm_queue", new String[] { "send_at|address|message", "result", "received_at" });
			roleFuncProj.get(role).put(
				"segment_receive_sms", new String[] { "segment_payload", "result", "received_at" });
			roleFuncProj.get(role).put(
				"segment_receive_sbd", new String[] { "segment_payload", "result", "received_at" });
			roleFuncProj.get(role).put(
				"segment_receive_swm", new String[] { "segment_payload", "result", "received_at" });
			roleFuncProj.get(role).put(
				"get_momentary_values", new String[] { "value", "result", "received_at" });
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
					"microphone_test", new String[] { "target", "result", "received_at" });
			roleFuncProj.get(role).put(
					"diagnostic", new String[] { "target", "result", "received_at" });
			roleFuncProj.get(role).put(
					"signal", new String[] { "result" });
			roleFuncProj.get(role).put(
					"sentinel_values", new String[] { "result" });
		}
		return roleFuncProj;
	}
	
	public static JSONArray getQuery(String role, String function, String query, ContentResolver contentResolver) {
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

	public static int deleteQuery(String role, String function, String query, ContentResolver contentResolver) {
		int deleteQueryResult = 0;
		try {
			Cursor queryCursor = contentResolver.query( getUri( role, function, query ), getProjection( role, function ), null, null, null );
			if ((queryCursor != null) && (queryCursor.getCount() > 0) && queryCursor.moveToFirst()) { do {
				deleteQueryResult = Integer.parseInt( queryCursor.getString( queryCursor.getColumnIndex("result") ) );
			} while (queryCursor.moveToNext()); queryCursor.close(); }
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return deleteQueryResult;
	}
	
	public static int updateQuery(String role, String function, String query, ContentResolver contentResolver) {
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

	public static boolean getFileRequest(Uri fileUri, String outputFileAbsoluteFilePath, ContentResolver contentResolver) {

		try {

			FileUtils.initializeDirectoryRecursively(outputFileAbsoluteFilePath.substring(0, outputFileAbsoluteFilePath.lastIndexOf("/")), false);

			InputStream inputStream = contentResolver.openInputStream(fileUri);
			OutputStream outputStream = new FileOutputStream(outputFileAbsoluteFilePath);

			byte[] buf = new byte[1024]; int len;
			while ((len = inputStream.read(buf)) > 0) { outputStream.write(buf, 0, len); }

			inputStream.close();
			outputStream.close();

			FileUtils.chmod(outputFileAbsoluteFilePath, "rw", "rw");

			return FileUtils.exists(outputFileAbsoluteFilePath);

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}

		FileUtils.delete(outputFileAbsoluteFilePath);
		return false;
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
		if (command != null) { uri.append("/").append(command); }
		return Uri.parse(uri.toString());
	}

	public static Uri getUri(String role, String function) {
		return getUri(role, function, null);
	}

	public static Uri getFileUri(String role, String filePathRelativeToFilesDir) {
		StringBuilder uri = (new StringBuilder())
				.append("content://")
				.append(getAuthority(role.toLowerCase(Locale.US)))
				.append(fileProviderAssetDirUriNamespacePrepend)
				.append(RfcxAssetCleanup.conciseFilePath(filePathRelativeToFilesDir, role));
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
		uriMatcher.addURI(getAuthority(_role), _function + "/*", uriMatchIds[1]);
		
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
		String _role = role.toLowerCase(Locale.US);
		return ((ArrayUtils.doesStringArrayContainString(RfcxRole.ALL_ROLES, _role)) ? "org.rfcx.guardian." : "") + _role;
	}
	
	public static String[] getProjection(String role, String function) {
		return initRoleFuncProj().get(role.toLowerCase(Locale.US)).get(function.toLowerCase(Locale.US));
	}


	public static ParcelFileDescriptor serveAssetFileRequest(Uri uri, String mode, Context context, String role, String logTag) {

		try {

			String assetUriPath = uri.getEncodedPath().substring(fileProviderAssetDirUriNamespacePrepend.length());
			String assetFilePath = context.getFilesDir().getAbsolutePath() + "/" + assetUriPath;
			String conciseAssetFilePath = RfcxAssetCleanup.conciseFilePath(assetFilePath, role);
			FileUtils.initializeDirectoryRecursively(assetFilePath.substring(0, assetFilePath.lastIndexOf("/")), false);
			File assetFile = new File(assetFilePath);

			Log.v(logTag, "File share request for asset "+conciseAssetFilePath);

			int imode = 0;
			if (mode.contains("w")) {
				imode |= ParcelFileDescriptor.MODE_WRITE_ONLY;
				if (!FileUtils.exists(assetFile)) {
					assetFile.createNewFile();
				}
			}

			if (mode.contains("r")) imode |= ParcelFileDescriptor.MODE_READ_ONLY;
			if (mode.contains("+")) imode |= ParcelFileDescriptor.MODE_APPEND;

			if (FileUtils.exists(assetFile)) {
				return ParcelFileDescriptor.open(assetFile, imode);
			} else {
				Log.e(logTag, "Requested asset does not exist: "+conciseAssetFilePath);
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "GuardianContentProvider - FileProvider");

		}
		return null;

	}




	public static String urlEncode(String unEncodedString) {
		String rtrnStr = unEncodedString;
		try {
			rtrnStr = URLEncoder.encode(rtrnStr, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			RfcxLog.logExc(logTag, e);
		}
		return rtrnStr;
	}

	public static String urlDecode(String encodedString) {
		String rtrnStr = encodedString;
		try {
			rtrnStr = URLDecoder.decode(encodedString, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			RfcxLog.logExc(logTag, e);
		}
		return rtrnStr;
	}
	
}
