package org.rfcx.guardian.utility;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.UUID;

public class DeviceUuid {
	
	private static final String TAG = DeviceUuid.class.getSimpleName();
	protected static final String PREFS_DEVICE_ID = "device_uuid";
    protected static UUID uuid;

    public DeviceUuid(Context context, SharedPreferences prefs) {
        if( uuid ==null ) {
            synchronized (DeviceUuid.class) {
                if(uuid == null) {
                	final String prefsDeviceId = prefs.getString(PREFS_DEVICE_ID,null);
                    if ((prefsDeviceId != null) && (prefsDeviceId.length() > 10)) {
                    	Log.d(TAG, "UUID fetched from prefs: "+ prefsDeviceId);
                        uuid = UUID.fromString(prefsDeviceId);
                    } else{
                    	uuid = getExistingUuidFromRfcxUpdater();
                    	if (uuid == null) uuid = UUID.randomUUID();
                    	prefs.edit().putString(PREFS_DEVICE_ID, uuid.toString() ).commit();
                    	Log.d(TAG, "Device UUID saved to prefs: "+ uuid.toString());
                    }
                }
            }
        }
    }


    public UUID getDeviceUuid() {
        return uuid;
    }
    
    private UUID getExistingUuidFromRfcxUpdater() {
    	try {
    		File uuidFile = new File("/data/data/org.rfcx.src_android_updater/files","uuid.txt");
    		if (uuidFile.exists()) {
				FileInputStream input = new FileInputStream(uuidFile);
				StringBuffer fileContent = new StringBuffer("");
				byte[] buffer = new byte[36];
				while (input.read(buffer) != -1) {
				    fileContent.append(new String(buffer));
				}
	    		String uuidString = fileContent.toString();
	    		Log.d(TAG, "Fetched UUID from RfcxUpdater: "+uuidString);
	    		return UUID.fromString(uuidString);
    		} else {
    			Log.e(TAG, "No previous UUID saved by RfcxUpdater...");
    		}
    	} catch (FileNotFoundException e) {
			e.printStackTrace();
    	} catch (IOException e) {
			e.printStackTrace();
		}
    	return null;
    }
}
