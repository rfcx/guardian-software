package org.rfcx.guardian.admin.device.sentinel;

import org.rfcx.guardian.admin.RfcxGuardian;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.admin.device.android.system.DeviceUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class DeviceSentinelService extends Service {

	public static final String SERVICE_NAME = "DeviceSentinel";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "DeviceSentinelService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private DeviceSentinelSvc deviceSentinelSvc;

	private int referenceCycleDuration = 1;

	private int innerLoopIncrement = 1;
	private int innerLoopsPerCaptureCycle = 1;
	private long innerLoopDelayRemainderInMilliseconds = 0;

	private double innerLoopsPerCaptureCycle_Power = 0;
	private double innerLoopsPerCaptureCycle_Accelerometer = 0;
	private double innerLoopsPerCaptureCycle_Compass = 0;

	// Sampling adds to the duration of the overall capture cycle, so we cut it short slightly based on an EMPIRICALLY DETERMINED percentage
	// This can help ensure, for example, that a 60 second capture loop actually returns values with an interval of 60 seconds, instead of 61 or 62 seconds
	private double captureCycleLastDurationPercentageMultiplier = 0.98;
	private long captureCycleLastStartTime = 0;
	private long[] captureCycleMeasuredDurations = new long[] { 0, 0, 0 };
	private double[] captureCyclePercentageMultipliers = new double[] { 0, 0, 0 };

	private int outerLoopIncrement = 0;
	private int outerLoopCaptureCount = 0;

	private boolean isSentinelPowerCaptureAllowed = true;
	private boolean isSentinelAccelCaptureAllowed = true;
	private boolean isSentinelCompassCaptureAllowed = true;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.deviceSentinelSvc = new DeviceSentinelSvc();
		app = (RfcxGuardian) getApplication();
		
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.deviceSentinelSvc.start();
		} catch (IllegalThreadStateException e) {
			RfcxLog.logExc(logTag, e);
		}
		return START_NOT_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.runFlag = false;
		app.rfcxSvc.setRunState(SERVICE_NAME, false);
		this.deviceSentinelSvc.interrupt();
		this.deviceSentinelSvc = null;
	}
	
	
	private class DeviceSentinelSvc extends Thread {
		
		public DeviceSentinelSvc() {
			super("DeviceSentinelService-DeviceSentinelSvc");
		}
		
		@Override
		public void run() {
			DeviceSentinelService deviceSentinelService = DeviceSentinelService.this;

			app = (RfcxGuardian) getApplication();

			while (deviceSentinelService.runFlag) {

				try {

					confirmOrSetCaptureParameters();

					if (innerLoopDelayRemainderInMilliseconds > 0) {
						Thread.sleep(innerLoopDelayRemainderInMilliseconds);
					}

					// Inner Loop Behavior
					innerLoopIncrement = triggerOrSkipInnerLoopBehavior(innerLoopIncrement, innerLoopsPerCaptureCycle);

					if (innerLoopIncrement == innerLoopsPerCaptureCycle) {

						app.rfcxSvc.reportAsActive(SERVICE_NAME);

						// Outer Loop Behavior
						outerLoopIncrement = triggerOrSkipOuterLoopBehavior(outerLoopIncrement, outerLoopCaptureCount);

					}

				} catch (InterruptedException e) {
					deviceSentinelService.runFlag = false;
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					RfcxLog.logExc(logTag, e);
				}

			}
			Log.v(logTag, "Stopping service: " + logTag);
		}		
	}

	private int triggerOrSkipInnerLoopBehavior(int innerLoopIncrement, int innerLoopsPerCaptureCycle) {

		innerLoopIncrement++;
		if (innerLoopIncrement > innerLoopsPerCaptureCycle) {
			innerLoopIncrement = 1;
		}

		//	Log.e(logTag, "RUN INNER LOOP BEHAVIOR...");

		if ((innerLoopIncrement % this.innerLoopsPerCaptureCycle_Power == 0) && this.isSentinelPowerCaptureAllowed) {
			app.sentinelPowerUtils.updateSentinelPowerValues();
		}

		if ((innerLoopIncrement % this.innerLoopsPerCaptureCycle_Accelerometer == 0) && this.isSentinelAccelCaptureAllowed) {
			app.sentinelAccelUtils.updateSentinelAccelValues();
		}

		if ((innerLoopIncrement % this.innerLoopsPerCaptureCycle_Compass == 0) && this.isSentinelCompassCaptureAllowed) {
			app.sentinelCompassUtils.updateSentinelCompassValues();
		}

		return innerLoopIncrement;
	}

	private int triggerOrSkipOuterLoopBehavior(int outerLoopIncrement, int outerLoopCaptureCount) {

		outerLoopIncrement++;
		if (outerLoopIncrement > outerLoopCaptureCount) {
			outerLoopIncrement = 1;
		}


		// run this on every loop, if allowed
		if (this.isSentinelPowerCaptureAllowed) {
			app.sentinelPowerUtils.saveSentinelPowerValuesToDatabase(true);
		}


		// run these on specific outer loop iterations
		if (outerLoopIncrement == outerLoopCaptureCount) {

			if (this.isSentinelAccelCaptureAllowed) {
				app.sentinelAccelUtils.saveSentinelAccelValuesToDatabase(true);
			}

			if (this.isSentinelCompassCaptureAllowed) {
				app.sentinelCompassUtils.saveSentinelCompassValuesToDatabase(true);
				app.sentinelCompassUtils.setOrResetSentinelCompassChip();
			}

		}

		if (app.deviceUtils.isReducedCaptureModeChanging(this.referenceCycleDuration)) {

			if (this.isSentinelPowerCaptureAllowed) {
				app.sentinelPowerUtils.setOrResetSentinelPowerChip();
			}
		}

		return outerLoopIncrement;
	}

	private boolean confirmOrSetCaptureParameters() {

		if ((app != null) && (innerLoopIncrement == 1)) {

			this.captureCycleLastStartTime = System.currentTimeMillis();

			this.isSentinelPowerCaptureAllowed = /*!this.isReducedCaptureModeActive && */app.sentinelPowerUtils.isCaptureAllowed();
			this.isSentinelAccelCaptureAllowed = !app.deviceUtils.isReducedCaptureModeActive && app.sentinelAccelUtils.isCaptureAllowed();
			this.isSentinelCompassCaptureAllowed = !app.deviceUtils.isReducedCaptureModeActive && app.sentinelCompassUtils.isCaptureAllowed();

			int audioCycleDuration = app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.AUDIO_CYCLE_DURATION);

			// when audio capture is disabled (for any number of reasons), we continue to capture system stats...
			// however, we slow the capture cycle by the multiple indicated in SentinelUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf
			int prefsReferenceCycleDuration = app.deviceUtils.isReducedCaptureModeActive ? (audioCycleDuration * SentinelUtils.inReducedCaptureModeExtendCaptureCycleByFactorOf) : audioCycleDuration;

			if (this.referenceCycleDuration != prefsReferenceCycleDuration) {

				this.referenceCycleDuration = prefsReferenceCycleDuration;
				this.innerLoopsPerCaptureCycle = DeviceUtils.getInnerLoopsPerCaptureCycle(prefsReferenceCycleDuration);
				this.outerLoopCaptureCount = DeviceUtils.getOuterLoopCaptureCount(prefsReferenceCycleDuration);

				long samplingOperationDuration = 0;
				this.innerLoopDelayRemainderInMilliseconds = DeviceUtils.getInnerLoopDelayRemainder(prefsReferenceCycleDuration, this.captureCycleLastDurationPercentageMultiplier, samplingOperationDuration);

				this.innerLoopsPerCaptureCycle_Power = 1;
				this.innerLoopsPerCaptureCycle_Compass = Math.ceil((double) this.innerLoopsPerCaptureCycle / SentinelCompassUtils.samplesTakenPerCaptureCycle);
				this.innerLoopsPerCaptureCycle_Accelerometer = Math.ceil((double) this.innerLoopsPerCaptureCycle / SentinelAccelUtils.samplesTakenPerCaptureCycle);

				app.sentinelPowerUtils.setOrResetSentinelPowerChip();
				app.sentinelCompassUtils.setOrResetSentinelCompassChip();

				Log.d(logTag, "SentinelStats Capture" + (app.deviceUtils.isReducedCaptureModeActive ? " (currently limited)" : "") + ": " +
						"Snapshots (all metrics) taken every " + Math.round((double) DeviceUtils.getCaptureCycleDuration(prefsReferenceCycleDuration) / 1000) + " seconds.");
			}

		} else {
			return false;
		}

		return true;
	}

}
