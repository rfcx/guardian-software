package setup.contentprovider;

import rfcx.utility.rfcx.RfcxLog;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import setup.RfcxGuardian;

public class SetupContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, SetupContentProvider.class);

	private static final String appRole = RfcxGuardian.APP_ROLE;

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
//		try {
//			
//			// get role "version" endpoints
//			
//			if (RfcxComm.uriMatch(uri, appRole, "version", null)) {
//				return RfcxComm.getProjectionCursor(appRole, "version", new Object[] { appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag) });
//			
//			// "prefs" function endpoints
//			
//			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) {
//				MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
//				for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
//					cursor.addRow(new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
//				}
//				return cursor;
//				
//			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) {
//				String prefKey = uri.getLastPathSegment();
//				return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
//				
//			}
//			
//			
//		} catch (Exception e) {
//			RfcxLog.logExc(logTag, e);
//		}
		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			
//			if (URI_MATCHER.match(uri) == ENDPOINT_PREFS_ID) {
//				String prefKey = uri.getLastPathSegment();
//				app.setPref(prefKey, values.getAsString(prefKey));
//				return 1;
//			}
			
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}


	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}
	
}
