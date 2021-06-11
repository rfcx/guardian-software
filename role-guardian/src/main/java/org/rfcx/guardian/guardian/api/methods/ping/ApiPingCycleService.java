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

						if (DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds(app.apiPingUtils.repeatingPingLastAttemptedAt) >= app.apiPingUtils.repeatingPingCycleDuration) {

							app.apiPingUtils.repeatingPingLastAttemptedAt = System.currentTimeMillis();

							if (app.apiPingUtils.isScheduledPingAllowedAtThisTimeOfDay()) {

								String[] includePingFields = app.rfcxPrefs.getPrefAsString(RfcxPrefs.Pref.API_PING_CYCLE_FIELDS).split(",");

								app.apiPingUtils.repeatingPingLastQueuedAt = System.currentTimeMillis();

								app.apiPingUtils.sendPing(
										ArrayUtils.doesStringArrayContainString(includePingFields, "all"),
										includePingFields,
										ArrayUtils.doesStringArrayContainString(includePingFields, "meta") ? 1 : 0,
										"all",
										app.apiPingUtils.repeatingPingLastCompletedAt > 0
								);
								app.apiPingUtils.repeatingPingLastCompletedAt = System.currentTimeMillis();

							} else {

								Log.e(logTag, "Repeating Ping blocked due to time of day.");
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
