package org.rfcx.guardian.sentinel;

import org.rfcx.guardian.sentinel.service.ServiceMonitorIntentService;
import org.rfcx.guardian.utility.rfcx.RfcxDeviceId;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;
import org.rfcx.guardian.utility.rfcx.RfcxRole;
import org.rfcx.guardian.utility.service.RfcxServiceHandler;

import android.app.Application;
import android.content.Context;

public class RfcxGuardian extends Application {
	
	public String version;
	Context context;
	
	public static final String APP_ROLE = "Sentinel";

	private static final String TAG = "Rfcx-"+APP_ROLE+"-"+RfcxGuardian.class.getSimpleName();

	public RfcxDeviceId rfcxDeviceId = null; 
	public RfcxPrefs rfcxPrefs = null;
	public RfcxServiceHandler rfcxServiceHandler = null;
	
	@Override
	public void onCreate() {

		super.onCreate();

		this.rfcxDeviceId = new RfcxDeviceId(this, APP_ROLE);
		this.rfcxPrefs = new RfcxPrefs(this, APP_ROLE);
		this.rfcxServiceHandler = new RfcxServiceHandler(this, APP_ROLE);
		
		this.version = RfcxRole.getRoleVersion(getApplicationContext(), TAG);
		rfcxPrefs.writeVersionToFile(this.version);
		
		setDbHandlers();
		setServiceHandlers();
		
		initializeRoleServices();
	}
	
	public void onTerminate() {
		super.onTerminate();
	}
	
	public void appResume() {

	}
	
	public void appPause() {
		
	}

	public void initializeRoleServices() {
		
		if (!this.rfcxServiceHandler.hasRun("OnLaunchServiceSequence")) {
			this.rfcxServiceHandler.triggerServiceSequence(
				"OnLaunchServiceSequence", 
					new String[] { 
//						"ServiceMonitor"+"|"+"0"+"|"+this.rfcxPrefs.getPrefAsString("service_monitor_cycle_duration")
						}, 
				true);
		}
	}
	
	private void setDbHandlers() {
		int versionNumber = RfcxRole.getRoleVersionValue(this.version);
	}

	private void setServiceHandlers() {
		this.rfcxServiceHandler.addService("ServiceMonitor", ServiceMonitorIntentService.class);
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
