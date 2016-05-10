package org.rfcx.guardian.system.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.rfcx.guardian.system.RfcxGuardian;
import org.rfcx.guardian.system.device.DeviceScreenShot;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.device.DeviceDiskUsage;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class SystemContentProvider extends ContentProvider {
	
	private static final String TAG = "Rfcx-"+RfcxGuardian.APP_ROLE+"-"+SystemContentProvider.class.getSimpleName();
	
	private static final String AUTHORITY = RfcxRole.RoleApi.system.AUTHORITY;
	private static final String ENDPOINT_META = RfcxRole.RoleApi.system.ENDPOINT_META;
	private static final String ENDPOINT_SCREENSHOT = RfcxRole.RoleApi.system.ENDPOINT_SCREENSHOT;
	
	private static final int ENDPOINT_META_LIST = 1;
	private static final int ENDPOINT_META_ID = 2;
	private static final int ENDPOINT_SCREENSHOT_LIST = 3;
	private static final int ENDPOINT_SCREENSHOT_ID = 4;

	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_META, ENDPOINT_META_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_META+"/#", ENDPOINT_META_ID);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_SCREENSHOT, ENDPOINT_SCREENSHOT_LIST);
		URI_MATCHER.addURI(AUTHORITY, ENDPOINT_SCREENSHOT+"/#", ENDPOINT_SCREENSHOT_ID);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_META_LIST) {
			
				MatrixCursor cursor = new MatrixCursor(RfcxRole.RoleApi.system.PROJECTION_META);
				String[] vBattery = app.deviceStateDb.dbBattery.getConcatRows();
				String[] vCpu = app.deviceStateDb.dbCPU.getConcatRows();
				String[] vPower = app.deviceStateDb.dbPower.getConcatRows();
				String[] vNetwork = app.deviceStateDb.dbNetwork.getConcatRows();
				String[] vOffline = app.deviceStateDb.dbOffline.getConcatRows();
				String[] vLightMeter = app.deviceStateDb.dbLightMeter.getConcatRows();
				String[] vDataTransferred = app.dataTransferDb.dbTransferred.getConcatRows();
				String[] vAccelerometer = app.deviceStateDb.dbAccelerometer.getConcatRows();
				
				List<String> diskUsage = new ArrayList<String>();
				for (String[] usageStat : DeviceDiskUsage.allDiskStats()) {
					diskUsage.add(TextUtils.join("*", usageStat));
				}
				
				cursor.addRow(new Object[] { 
						(vBattery[0] != "0") ? vBattery[1] : null, 	// battery
						(vCpu[0] != "0") ? vCpu[1] : null, 			// cpu
						(vPower[0] != "0") ? vPower[1] : null, 			// power
						(vNetwork[0] != "0") ? vNetwork[1] : null,				// network
						(vOffline[0] != "0") ? vOffline[1] : null, 				// offline
						(vLightMeter[0] != "0") ? vLightMeter[1] : null, 		// lightmeter
						(vDataTransferred[0] != "0") ? vDataTransferred[1] : null,  // data_transfer
						TextUtils.join("|", diskUsage),							// disk_usage
						(vAccelerometer[0] != "0") ? vAccelerometer[1] : null 	// accelerometer
						
					});
				return cursor;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_SCREENSHOT_LIST) {
				
				MatrixCursor cursor = new MatrixCursor(RfcxRole.RoleApi.system.PROJECTION_SCREENSHOT);
				
				for (String[] screenShotRow : app.screenShotDb.dbCaptured.getAllCaptured()) {
					cursor.addRow(new Object[] { 
							screenShotRow[0], screenShotRow[1], screenShotRow[2], screenShotRow[3], screenShotRow[4]});
				}
				return cursor;
			}
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
		}
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_META_ID) {
				
				Date deleteBefore = new Date(Long.parseLong(uri.getLastPathSegment()));
				
				app.deviceStateDb.dbBattery.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbCPU.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbPower.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbNetwork.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbOffline.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbLightMeter.clearRowsBefore(deleteBefore);
				app.deviceStateDb.dbAccelerometer.clearRowsBefore(deleteBefore);
				app.dataTransferDb.dbTransferred.clearRowsBefore(deleteBefore);
				
				return 1;
				
			} else if (URI_MATCHER.match(uri) == ENDPOINT_SCREENSHOT_ID) {
				
				long screenShotTimestamp = Long.parseLong(uri.getLastPathSegment());
				String[] screenShotInfo = app.screenShotDb.dbCaptured.getSingleRowByTimestamp(""+screenShotTimestamp);
				app.screenShotDb.dbCaptured.deleteSingleRowByTimestamp(screenShotInfo[1]);
				(new File(screenShotInfo[4])).delete();
		
				return 1;
			}
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
		}
		return 0;
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
		
		RfcxGuardian app = (RfcxGuardian) getContext().getApplicationContext();
		
		try {
			if (URI_MATCHER.match(uri) == ENDPOINT_META_LIST) {
				return null;
			} else if (URI_MATCHER.match(uri) == ENDPOINT_SCREENSHOT_LIST) {
				app.triggerService("ScreenShot", true);
		//		String screenShotId = (new DeviceScreenShot()).saveScreenShot(app.getApplicationContext());
		//		return Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT+"/"+screenShotId);
			}
		} catch (Exception e) {
			RfcxLog.logExc(TAG, e);
		}
		return null;
	}
	
}
