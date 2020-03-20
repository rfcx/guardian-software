package org.rfcx.guardian.admin.device.android.control;

import android.app.Service;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.RfcxGuardian;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BluetoothTetherEnableService extends Service {

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, BluetoothTetherEnableService.class);

	private static final String SERVICE_NAME = "BluetoothTetherEnable";

	private RfcxGuardian app;

	private boolean runFlag = false;
	private BluetoothTetherEnable bluetoothTetherEnable;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.bluetoothTetherEnable = new BluetoothTetherEnable();
		app = (RfcxGuardian) getApplication();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: " + logTag);
		this.runFlag = true;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, true);
		try {
			this.bluetoothTetherEnable.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
		this.bluetoothTetherEnable.interrupt();
		this.bluetoothTetherEnable = null;
	}

	Object instance = null;
	Method setTetheringOn = null;
	Method isTetheringOn = null;
	Object mutex = new Object();

	private class BluetoothTetherEnable extends Thread {

		public BluetoothTetherEnable() {
			super("BluetoothTetherEnableService-BluetoothTetherEnable");
		}

		@Override
		public void run() {
			BluetoothTetherEnableService bluetoothTetherEnableInstance = BluetoothTetherEnableService.this;

			app = (RfcxGuardian) getApplication();
			Context context = app.getApplicationContext();

			try {
				app.rfcxServiceHandler.reportAsActive(SERVICE_NAME);

				try {

					String sClassName = "android.bluetooth.BluetoothPan";
					Class<?> classBluetoothPan = Class.forName(sClassName);

					Constructor<?> ctor = classBluetoothPan.getDeclaredConstructor(Context.class, BluetoothProfile.ServiceListener.class);
					ctor.setAccessible(true);
					//  Set Tethering ON
					Class[] paramSet = new Class[1];
					paramSet[0] = boolean.class;

					synchronized (mutex) {
						setTetheringOn = classBluetoothPan.getDeclaredMethod("setBluetoothTethering", paramSet);
						isTetheringOn = classBluetoothPan.getDeclaredMethod("isTetheringOn", null);
						instance = ctor.newInstance(context, new BluetoothTetherSvcListener(context));
					}
				} catch (ClassNotFoundException e) {
					RfcxLog.logExc(logTag, e);
				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
				}

			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			} finally {
				bluetoothTetherEnableInstance.runFlag = false;
				app.rfcxServiceHandler.setRunState(SERVICE_NAME, false);
				app.rfcxServiceHandler.stopService(SERVICE_NAME);
			}
		}
	}


	public class BluetoothTetherSvcListener implements BluetoothProfile.ServiceListener {

		private final Context context;

		public BluetoothTetherSvcListener(final Context context) {
			this.context = context;
		}

		@Override
		public void onServiceConnected(final int profile, final BluetoothProfile proxy) {

			try {
				synchronized (mutex) {
					if (!(Boolean)isTetheringOn.invoke(instance, null)) {
						setTetheringOn.invoke(instance, true);
						if ((Boolean)isTetheringOn.invoke(instance, null)) {
							Log.v(logTag, "Bluetooth Tethering has been enabled.");
						}
						else {
							Log.v(logTag, "Bluetooth Tethering could NOT be enabled.");
						}
					}
				}
			} catch (InvocationTargetException e) {
				RfcxLog.logExc(logTag, e);
			} catch (IllegalAccessException e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		@Override
		public void onServiceDisconnected(final int profile) {
		}


	}

}
