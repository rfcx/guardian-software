package org.rfcx.guardian.connect;

import org.rfcx.guardian.utility.DeviceGuid;
import org.rfcx.guardian.utility.DeviceToken;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.UserDictionary;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application implements OnSharedPreferenceChangeListener {

	private static final String TAG = "Rfcx-"+org.rfcx.guardian.utility.Constants.ROLE_NAME+"-"+RfcxGuardian.class.getSimpleName();

	public String version;
	Context context;
	public boolean verboseLog = true;
	
	private String deviceId = null;
	private String deviceToken = null;
	
	public static final String thisAppRole = "connect";
	public final String targetAppRole = "updater";
	
	private RfcxGuardianPrefs rfcxGuardianPrefs = new RfcxGuardianPrefs();
	public SharedPreferences sharedPrefs = rfcxGuardianPrefs.createPrefs(this);
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		rfcxGuardianPrefs.initializePrefs();
		rfcxGuardianPrefs.checkAndSet(this);
		
		setAppVersion();
		
		initializeRoleIntentServices(getApplicationContext());
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
		if (this.verboseLog) { Log.d(TAG, "Preference changed: "+key); }
		rfcxGuardianPrefs.checkAndSet(this);
	}
	
	private void setAppVersion() {
		try {
			this.version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName.trim();
			rfcxGuardianPrefs.writeVersionToFile(this.version);
		} catch (NameNotFoundException e) {
			Log.e(TAG,(e!=null) ? e.getMessage() : org.rfcx.guardian.utility.Constants.NULL_EXC);
		}
	}

	public String getDeviceId() {
		if (this.deviceId == null) {
			this.deviceId = (new DeviceGuid(getApplicationContext(), this.sharedPrefs)).getDeviceId();
			if (this.verboseLog) { Log.d(TAG,"Device GUID: "+this.deviceId); }
			rfcxGuardianPrefs.writeGuidToFile(deviceId);
		}
		return this.deviceId;
	}
	
	public String getDeviceToken() {
		if (this.deviceToken == null) {
			this.deviceToken = (new DeviceToken(getApplicationContext(), this.sharedPrefs)).getDeviceToken();
			rfcxGuardianPrefs.writeTokenToFile(deviceToken);
		}
		return this.deviceToken;
	}
	
	public void initializeRoleIntentServices(Context context) {
		try {
			
			
		} catch (Exception e) {
			Log.e(TAG,(e!=null) ? (e.getMessage() +" ||| "+ TextUtils.join(" | ", e.getStackTrace())) : org.rfcx.guardian.utility.Constants.NULL_EXC);
		}
	}
	
	public String getPref(String prefName) {
		return this.sharedPrefs.getString(prefName, null);
	}
	
	public boolean setPref(String prefName, String prefValue) {
		return this.sharedPrefs.edit().putString(prefName,prefValue).commit();
	}
    
	public void testContentResolver() {
		
		
		
		
		
		
		
		ContentResolver resolver = getContentResolver();
		
		String[] projection = new String[]{BaseColumns._ID, "meta_json"};
		Cursor cursor =
		      resolver.query(Uri.parse("content://org.rfcx.guardian.system/meta"),//UserDictionary.Words.CONTENT_URI,
		            projection,
		            null,
		            null,
		            null);
		if (cursor.moveToFirst()) {
		   do {
		      long id = cursor.getLong(0);
		      String meta = cursor.getString(1);
		      
		      Log.d(TAG, "Meta: "+meta);
		      
		   } while (cursor.moveToNext());
		}
		
	}
	
}
