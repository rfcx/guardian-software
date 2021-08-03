package org.rfcx.guardian.guardian.api.methods.ping;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.rfcx.guardian.guardian.RfcxGuardian;
import org.rfcx.guardian.utility.misc.ArrayUtils;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import org.rfcx.guardian.utility.rfcx.RfcxPrefs;

public class ApiPingCycleService extends Service {

	public static final String SERVICE_NAME = "ApiPingCycle";

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiPingCycleService");
	
	private RfcxGuardian app;
	
	private boolean runFlag = false;
	private ApiPingCycleSvc apiPingCycleSvc;

	public static final long CYCLE_DURATION = ( 20 * 1000 );

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		this.apiPingCycleSvc = new ApiPingCycleSvc();
		app = (RfcxGuardian) getApplication();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		Log.v(logTag, "Starting service: "+logTag);
		this.runFlag = true;
		app.rfcxSvc.setRunState(SERVICE_NAME, true);
		try {
			this.apiPingCycleSvc.start();
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
		this.apiPingCycleSvc.interrupt();
		this.apiPingCycleSvc = null;
	}
	
	
	private class ApiPingCycleSvc extends Thread {
		
		public ApiPingCycleSvc() { super("ApiPingCycleService-ApiPingCycleSvc"); }
		
		@Override
		public void run() {
			ApiPingCycleService apiPingCycleInstance = ApiPingCycleService.this;
			
			app = (RfcxGuardian) getApplication();

			while (apiPingCycleInstance.runFlag) {

				try {

					app.rfcxSvc.reportAsActive(SERVICE_NAME);

					if (app.apiPingUtils.repeatingPingLastAttemptedAt == 0) {

						Thread.sleep(ApiPingUtils.delayInitialRepeatingPingCycleByThisManyMs);
						app.apiPingUtils.updateRepeatingPingCycleDuration();
						app.apiPingUtils.repeatingPingLastAttemptedAt = System.currentTimeMillis();

					} else {

						long msSinceLastAttempt = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(app.apiPingUtils.repeatingPingLastAttemptedAt));

						if (msSinceLastAttempt >= app.apiPingUtils.repeatingPingCycleDuration) {

							Log.v(logTag, "Ping Cycle Launched ("+DateTimeUtils.milliSecondDurationAsReadableString(msSinceLastAttempt)+" since last attempt)");

							app.apiPingUtils.repeatingPingLastAttemptedAt = System.currentTimeMillis();

							if (!app.apiPingUtils.isScheduledPingAllowedAtThisTimeOfDay()) {

								app.apiPingUtils.repeatingPingLastCompletedOrSkippedAt = System.currentTimeMillis();
								Log.e(logTag, "Repeating Ping blocked due to time of day.");

							} else {

								String[] includePingFields = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PING_CYCLE_FIELDS).split(",");

								app.apiPingUtils.repeatingPingLastQueuedAt = System.currentTimeMillis();

								if ( app.apiPingUtils.sendPing(
										ArrayUtils.doesStringArrayContainString(includePingFields, "all"),
										includePingFields,
										(ArrayUtils.doesStringArrayContainString(includePingFields, "meta") || ArrayUtils.doesStringArrayContainString(includePingFields, "detections")) ? app.rfcxPrefs.getPrefAsInt(RfcxPrefs.Pref.CHECKIN_META_SEND_BUNDLE_LIMIT) : 0,
										"all",
										true
									)) {
									app.apiPingUtils.repeatingPingLastCompletedOrSkippedAt = System.currentTimeMillis();

								} else {

									// If the ping tries but fails (across all allowed protocols), then what behavior should we have?
									// Should we skip until next time, or attempt to resend (after, say airplane mode toggling, etc)
//									Log.e(logTag, "Ping publication failed. Delaying an extra "+CYCLE_DURATION+"ms and trying again...");
//									Thread.sleep(CYCLE_DURATION);
								}

							}

						}

						Thread.sleep(CYCLE_DURATION);
					}

				} catch (Exception e) {
					RfcxLog.logExc(logTag, e);
					app.rfcxSvc.setRunState(SERVICE_NAME, false);
					apiPingCycleInstance.runFlag = false;
				}
			}

			app.rfcxSvc.setRunState(SERVICE_NAME, false);
			apiPingCycleInstance.runFlag = false;
			Log.v(logTag, "Stopping service: "+logTag);
		}
	}

	
}
