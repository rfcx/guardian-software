package admin.contentprovider;

import org.rfcx.guardian.utility.rfcx.RfcxComm;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import admin.RfcxGuardian;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class AdminContentProvider extends ContentProvider {
	
	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, AdminContentProvider.class);
	
	private static final String appRole = RfcxGuardian.APP_ROLE;

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
		
			// get role "version" endpoints
			
			if (RfcxComm.uriMatch(uri, appRole, "version", null)) {
				return RfcxComm.getProjectionCursor(appRole, "version", new Object[] { appRole, RfcxRole.getRoleVersion(app.getApplicationContext(), logTag) });

			// "prefs" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", null)) {
				MatrixCursor cursor = RfcxComm.getProjectionCursor(appRole, "prefs", null);
				for (String prefKey : app.rfcxPrefs.listPrefsKeys()) {
					cursor.addRow(new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
				}
				return cursor;
				
			} else if (RfcxComm.uriMatch(uri, appRole, "prefs", "*")) {
				String prefKey = uri.getLastPathSegment();
				return RfcxComm.getProjectionCursor(appRole, "prefs", new Object[] { prefKey, app.rfcxPrefs.getPrefAsString(prefKey) });
				
			// "control" function endpoints
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "reboot")) {
				app.rfcxServiceHandler.triggerService("RebootTrigger", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "reboot", null, System.currentTimeMillis() });
				
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "screenshot")) {
				app.rfcxServiceHandler.triggerService("ScreenShotJob", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "screenshot", null, System.currentTimeMillis() });
			
			} else if (RfcxComm.uriMatch(uri, appRole, "control", "airplanemode_off")) {
				app.rfcxServiceHandler.triggerService("AirplaneModeOff", true);
				return RfcxComm.getProjectionCursor(appRole, "control", new Object[] { "airplanemode_off", null, System.currentTimeMillis() });
			
			}
			
			
			
			
			return null;
						
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
		return null;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return 0;
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public String getType(Uri uri) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		return null;
	}
	
}
