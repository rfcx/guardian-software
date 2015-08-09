package org.rfcx.guardian.system.content;

import java.util.Calendar;
import java.util.Date;

import org.rfcx.guardian.system.RfcxGuardian;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SystemContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+SystemContentProvider.class.getSimpleName();

	private RfcxGuardian app = null;
	private Context context = null;
	
	private static final String AUTHORITY = org.rfcx.guardian.utility.Constants.RfcxContentProvider.system.AUTHORITY;
	private static final String ENDPOINT = org.rfcx.guardian.utility.Constants.RfcxContentProvider.system.ENDPOINT;
	
	private static final int ENDPOINT_LIST = 1;
	private static final int ENDPOINT_ID = 2;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT, ENDPOINT_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT+"/#", ENDPOINT_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		checkSetApplicationContext();
		
		String[] vBattery = app.deviceStateDb.dbBattery.getConcatRows();
		String[] vCpu = app.deviceStateDb.dbCPU.getConcatRows();
		String[] vPower = app.deviceStateDb.dbPower.getConcatRows();
		String[] vNetwork = app.deviceStateDb.dbNetwork.getConcatRows();
		String[] vOffline = app.deviceStateDb.dbOffline.getConcatRows();
		String[] vLightMeter = app.deviceStateDb.dbLightMeter.getConcatRows();
		String[] vDataTransferred = app.dataTransferDb.dbTransferred.getConcatRows();
		
		MatrixCursor cursor = new MatrixCursor(org.rfcx.guardian.utility.Constants.RfcxContentProvider.system.PROJECTION);
		
		cursor.addRow(new Object[] { 
				Calendar.getInstance().getTimeInMillis(),
				(vBattery[0] != "0") ? vBattery[1] : null, 	// battery
				(vCpu[0] != "0") ? vCpu[1] : null, 			// cpu
				(vPower[0] != "0") ? vPower[1] : null, 		// power
				(vNetwork[0] != "0") ? vNetwork[1] : null,	// network
				(vOffline[0] != "0") ? vOffline[1] : null, 	// offline
				(vLightMeter[0] != "0") ? vLightMeter[1] : null, // lightmeter
				(vDataTransferred[0] != "0") ? vDataTransferred[1] : null  // data_transfer
			});
		
		return cursor;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		checkSetApplicationContext();
		
		if (URI_MATCHER.match(uri) == ENDPOINT_ID) {
			Date deleteBefore = new Date(Long.parseLong(uri.getLastPathSegment()));
			
			app.deviceStateDb.dbBattery.clearRowsBefore(deleteBefore);
			app.deviceStateDb.dbCPU.clearRowsBefore(deleteBefore);
			app.deviceStateDb.dbPower.clearRowsBefore(deleteBefore);
			app.deviceStateDb.dbNetwork.clearRowsBefore(deleteBefore);
			app.deviceStateDb.dbOffline.clearRowsBefore(deleteBefore);
			app.deviceStateDb.dbLightMeter.clearRowsBefore(deleteBefore);
			app.dataTransferDb.dbTransferred.clearRowsBefore(deleteBefore);
			
			return 0;
		} else {
			return 0;
		}
		
		
	}
	
	private void checkSetApplicationContext() {
		if (this.context == null) { this.context = getContext(); }
		if (this.app == null) { this.app = (RfcxGuardian) this.context.getApplicationContext(); }
	}
	
	@Override
	public boolean onCreate() {
		return true;
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return 0;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return null;
	}
	
}
