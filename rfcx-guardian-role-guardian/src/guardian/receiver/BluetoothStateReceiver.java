package guardian.receiver;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import guardian.RfcxGuardian;

public class BluetoothStateReceiver extends BroadcastReceiver {

	private String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothStateReceiver.class);
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		RfcxGuardian app = (RfcxGuardian) context.getApplicationContext();
		
		final String intentAction = intent.getAction();

        if (intentAction.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        		
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            
            switch (bluetoothState) {
	            case BluetoothAdapter.STATE_OFF:
	            		app.deviceBluetooth.setOn();
	                break;
	            case BluetoothAdapter.STATE_TURNING_OFF:
	            		app.deviceBluetooth.setOn();
	                break;
            }
            
        }
		
	}

}
