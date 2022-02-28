package org.rfcx.guardian.utility.device.control;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class DeviceBluetooth {

    private static final String logTag = RfcxLog.generateLogTag("Utils", "DeviceBluetooth");
    final Object panMutex = new Object();
    Object panInstance = null;
    Method isPanOn = null;
    Method setPanOn = null;
    Method setPanOff = null;
    boolean isPanBeingEnabled;

    // Network Name controls
    private BluetoothAdapter bluetoothAdapter;

    // Tethering controls
    private Context context;
    public DeviceBluetooth(Context context) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
    }

    private boolean isBluetoothEnabled() {
        if (bluetoothAdapter != null) {
            int bluetoothState = bluetoothAdapter.getState();
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return false;
                case BluetoothAdapter.STATE_ON:
                case BluetoothAdapter.STATE_TURNING_ON:
                    return true;
            }
        }
        return false;
    }

    public void setPowerOn() {
        if (!isBluetoothEnabled()) {
            Log.v(logTag, "Activating Bluetooth Power");
            bluetoothAdapter.enable();
            setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
        }
    }

    public void setPowerOff() {
        if (isBluetoothEnabled()) {
            Log.v(logTag, "Deactivating Bluetooth Power");
            bluetoothAdapter.disable();
        }
    }

    public void setPanName(String panName) {
        if (isBluetoothEnabled()) {
            bluetoothAdapter.setName(panName);
            Log.v(logTag, "Bluetooth Network Name: '" + bluetoothAdapter.getName() + "'");
        }
    }

    public void setPanOn() {
        this.isPanBeingEnabled = true;
        setPan();
        setBluetoothScanMode(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE);
    }

    public void setPanOff() {
        this.isPanBeingEnabled = false;
        setPowerOff(); // this is kind of cheating...
//		setPan();
    }

    private void setBluetoothScanMode(int scanMode) {
        if (isBluetoothEnabled()) {
            Method method = null;

            try {
                method = bluetoothAdapter.getClass().getMethod("setScanMode", int.class);
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }

            try {
                method.invoke(bluetoothAdapter, scanMode);
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }
    }

    private void setPan() {

        try {

            String sClassName = "android.bluetooth.BluetoothPan";
            @SuppressLint("PrivateApi") Class<?> classBluetoothPan = Class.forName(sClassName);

            Constructor<?> tetherConstructor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
            tetherConstructor.setAccessible(true);

            Class[] enableTetheringParamSet = new Class[1];
            enableTetheringParamSet[0] = boolean.class;

            // THIS IS PROBABLY NOT RIGHT —— NEED TO SET REAL PARAMS FOR DISABLING TETHERING
            Class[] disableTetheringParamSet = new Class[1];
            disableTetheringParamSet[0] = boolean.class;
            // THIS IS PROBABLY NOT RIGHT —— NEED TO SET REAL PARAMS FOR DISABLING TETHERING

            synchronized (panMutex) {
                isPanOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", null);

                if (this.isPanBeingEnabled) {
                    setPanOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", enableTetheringParamSet);
                } else {
                    // THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
                    setPanOff = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", disableTetheringParamSet);
                    // THIS IS PROBABLY NOT RIGHT —— NEED TO KNOW PARAMS FOR DISABLING TETHERING
                }

                panInstance = tetherConstructor.newInstance(context, new BluetoothTetherServiceListener(this.isPanBeingEnabled));
            }

        } catch (Exception e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    public class BluetoothTetherServiceListener implements BluetoothProfile.ServiceListener {

        private final boolean isPanBeingEnabled;

        public BluetoothTetherServiceListener(final boolean isPanBeingEnabled) {
            this.isPanBeingEnabled = isPanBeingEnabled;
        }

        @Override
        public void onServiceConnected(final int profile, final BluetoothProfile proxy) {

            try {
                synchronized (panMutex) {
                    if (!(Boolean) isPanOn.invoke(panInstance, null)) {
                        Log.v(logTag, "Bluetooth Tethering is disabled");
                        if (this.isPanBeingEnabled) {
                            Log.v(logTag, "Attempting to activate Bluetooth Tethering");

                            setPanOn.invoke(panInstance, true);
                            if ((Boolean) isPanOn.invoke(panInstance, null)) {
                                Log.v(logTag, "Bluetooth Tethering has been activated");
                            } else {
                                Log.e(logTag, "Failed to activate Bluetooth Tethering");
                            }
                        }
                    } else if ((Boolean) isPanOn.invoke(panInstance, null)) {
                        Log.v(logTag, "Bluetooth Tethering is enabled");
                        if (!this.isPanBeingEnabled) {
                            Log.v(logTag, "Attempting to de-activate Bluetooth Tethering");
                            setPanOff.invoke(panInstance, true);
                            if (!(Boolean) isPanOn.invoke(panInstance, null)) {
                                Log.v(logTag, "Bluetooth Tethering has been de-activated");
                            } else {
                                Log.e(logTag, "Failed to de-activate Bluetooth Tethering");
                            }
                        }
                    }
                }
            } catch (Exception e) {
                RfcxLog.logExc(logTag, e);
            }
        }

        @Override
        public void onServiceDisconnected(final int profile) {
        }


    }

}
