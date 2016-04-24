package org.rfcx.guardian.connect;

import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;
import org.rfcx.guardian.utility.RfcxConstants;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Rfcx-"+RfcxConstants.ROLE_NAME+"-"+RfcxGuardian.class.getSimpleName();

	public String version;
	Context context;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "connect";
	public final String targetAppRole = "updater";
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		
		setAppVersion();
		
		initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {
		rfcxGuardianPrefs.checkAndSet(this);
	}
	
	public void appPause() {
	}
	
	@Override
	public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Log.d(TAG, "Preference changed: "+key);
		rfcxGuardianPrefs.checkAndSet(this);
	}
	
	private void setAppVersion() {
		try {
			this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.trim();
			rfcxGuardianPrefs.writeVersionToFile(this.version);
		} catch (NameNotFoundException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : RfcxConstants.NULL_EXC);
		}
	}

	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext(), this.sharedPrefs)).getDeviceToken();
		}
		return this.deviceToken;
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				
				this.hasRun_OnLaunchServiceTrigger = true;
			} catch (Exception e) {
				Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : RfcxConstants.NULL_EXC);
			}
		}
	}
	
	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
    
	public void testContentResolver() {
				
//		Cursor cursor = this.getContentResolver().query(
//					Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT),
//		    		RfcxConstants.RfcxContentProvider.system.PROJECTION_SCREENSHOT,
//		            null,
//		            null,
//		            null);
		
//		
//		Cursor cursor = getContentResolver().query(Uri.parse("content://sms/"), null, null, null, null);
//		if (cursor.moveToFirst()) {
//		   do {
//			  for (int i = 0; i < cursor.getColumnCount(); i++) {
//				  Log.d(TAG, cursor.getColumnName(i)+": "+cursor.getString(i));
//			  }
//			  Log.d(TAG,"---------------------------");
//		   } while (cursor.moveToNext());
//		}
		
		SmsManager smsManager = SmsManager.getDefault();
		smsManager.sendTextMessage("+14153359205", null, "sms message", null, null);
		
		
//		long timeStamp = Calendar.getInstance().getTimeInMillis();
//		
//		int del = getContentResolver().delete(
//				Uri.parse("content://sms/"+"12"),
//	            null,
//	            null);
//		ContentValues contentValues = new ContentValues();
//		Uri insertSomething=
//				getContentResolver().insert(Uri.parse(RfcxConstants.RfcxContentProvider.system.URI_SCREENSHOT), contentValues);
		
	}
	
}
