package org.rfcx.guardian.sentinel;

import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;

import android.app.Application;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Sentinel";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	
	private boolean hasRun_OnLaunchServiceTrigger = false;
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceId = (new RfcxDeviceId()).init(getApplicationContext());
		this.rfcxPrefs = (new RfcxPrefs()).init(getApplicationContext(), APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		
		initializeRoleServices(getApplicationContext());
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}
	
	public void initializeRoleServices(Context context) {
		if (!this.hasRun_OnLaunchServiceTrigger) {
			try {
				// reboots system at 5 minutes before midnight every day
//				PendingIntent rebootIntentService = PendingIntent.getService(context, -1, new Intent(context, RebootIntentService.class), PendingIntent.FLAG_UPDATE_CURRENT);
//				AlarmManager rebootAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);		
//				rebootAlarmManager.setRepeating(AlarmManager.RTC, DateTimeUtils.nextOccurenceOf(23,55,0).getTimeInMillis(), 24*60*60*1000, rebootIntentService);
				this.hasRun_OnLaunchServiceTrigger = true;
			} catch (Exception e) {
				RfcxLog.logExc(TAG, e);
			}
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
	}
	
//	
//	// Get UsbManager from Android.
//	UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
//
//	// Find the first available driver.
//	UsbSerialDriver driver = UsbSerialProber.acquire(manager);
//
//	public void checkSerial() {
//		if (driver != null) {
//		  driver.open();
//		  try {
//		    driver.setBaudRate(115200);
//	
//		    byte buffer[] = new byte[16];
//		    int numBytesRead = driver.read(buffer, 1000);
//		    Log.d(TAG, "Read " + numBytesRead + " bytes.");
//		  } catch (IOException e) {
//		    // Deal with error.
//		  } finally {
//		    driver.close();
//		  } 
//		}
//	}
    
}
