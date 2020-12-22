package org.rfcx.guardian.guardian.api.protocols;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;

import org.rfcx.guardian.guardian.socket.SocketManager;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.misc.StringUtils;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.network.MqttUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.rfcx.guardian.guardian.RfcxGuardian;

public class ApiMqttUtils implements MqttCallback {

	public ApiMqttUtils(Context context) {

		this.app = (RfcxGuardian) context.getApplicationContext();

		this.mqttCheckInClient = new MqttUtils(context, RfcxGuardian.APP_ROLE, this.app.rfcxGuardianIdentity.getGuid());

		this.mqttTopic_Subscribe_Command = "grd/"+this.app.rfcxGuardianIdentity.getGuid()+"/cmd";
		this.mqttTopic_Publish_CheckIn = "grd/"+this.app.rfcxGuardianIdentity.getGuid()+"/chk";
		this.mqttTopic_Publish_Ping = "grd/"+this.app.rfcxGuardianIdentity.getGuid()+"/png";

		this.mqttCheckInClient.addSubscribeTopic(this.mqttTopic_Subscribe_Command);

		setOrResetBrokerConfig();
		this.mqttCheckInClient.setCallback(this);
		getSetCheckInPublishTimeOutLength();
		initializeFailedCheckInThresholds();

		confirmOrCreateConnectionToBroker(true);
	}

	private static final String logTag = RfcxLog.generateLogTag(RfcxGuardian.APP_ROLE, "ApiMqttUtils");

	private RfcxGuardian app;
	private MqttUtils mqttCheckInClient = null;

	private String mqttTopic_Subscribe_Command = null;
	private String mqttTopic_Publish_CheckIn = null;
	private String mqttTopic_Publish_Ping = null;

	private long checkInPublishTimeOutLength = 0;
	private long checkInPublishCompletedAt = System.currentTimeMillis();

	private int inFlightCheckInAttemptCounter = 0;
	private int inFlightCheckInAttemptCounterLimit = 5;

	private int[] failedCheckInThresholds = new int[0];
	private boolean[] failedCheckInThresholdsReached = new boolean[0];

	public void setOrResetBrokerConfig() {
		String[] authUserPswd = app.rfcxPrefs.getPrefAsString("api_mqtt_auth_creds").split(",");
		String authUser = !app.rfcxPrefs.getPrefAsBoolean("enable_mqtt_auth") ? null : authUserPswd[0];
		String authPswd = !app.rfcxPrefs.getPrefAsBoolean("enable_mqtt_auth") ? null : authUserPswd[1];
		this.mqttCheckInClient.setOrResetBroker(
			this.app.rfcxPrefs.getPrefAsString("api_mqtt_protocol"),
			this.app.rfcxPrefs.getPrefAsInt("api_mqtt_port"),
			this.app.rfcxPrefs.getPrefAsString("api_mqtt_host"),
			this.app.rfcxGuardianIdentity.getKeystorePassphrase(),
			!authUser.equalsIgnoreCase("[guid]") ? authUser : app.rfcxGuardianIdentity.getGuid(),
			!authPswd.equalsIgnoreCase("[token]") ? authPswd : app.rfcxGuardianIdentity.getAuthToken()
		);
	}

	public long getSetCheckInPublishTimeOutLength() {
		long timeOutLength = 2 * this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 1000;
		if (this.checkInPublishTimeOutLength < timeOutLength) {
			this.checkInPublishTimeOutLength = timeOutLength;
			this.mqttCheckInClient.setActionTimeout(timeOutLength);
		}
		return this.checkInPublishTimeOutLength;
	}

	private byte[] packageMqttCheckInPayload(String checkInJsonString, String checkInAudioFilePath) throws IOException, JSONException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		String[] screenShotMeta = app.assetUtils.getLatestExternalAssetMeta("screenshots", this.checkInPublishTimeOutLength);
		if ((screenShotMeta[0] != null) && !FileUtils.exists(screenShotMeta[0])) {
			app.assetUtils.purgeSingleAsset("screenshot", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), screenShotMeta[2]);
		}

		String[] logFileMeta = app.assetUtils.getLatestExternalAssetMeta("logs", this.checkInPublishTimeOutLength);
		if ((logFileMeta[0] != null) && !FileUtils.exists(logFileMeta[0])) {
			app.assetUtils.purgeSingleAsset("log", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), logFileMeta[2]);
		}

        String[] photoFileMeta = app.assetUtils.getLatestExternalAssetMeta("photos", this.checkInPublishTimeOutLength);
        if ((photoFileMeta[0] != null) && !FileUtils.exists(photoFileMeta[0])) {
			app.assetUtils.purgeSingleAsset("photo", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), photoFileMeta[2]);
        }

		String[] videoFileMeta = app.assetUtils.getLatestExternalAssetMeta("videos", this.checkInPublishTimeOutLength);
		if ((videoFileMeta[0] != null) && !FileUtils.exists(videoFileMeta[0])) {
			app.assetUtils.purgeSingleAsset("video", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), videoFileMeta[2]);
		}

        // Build JSON blob from included assets
		byte[] jsonBlobAsBytes = StringUtils.stringToGZippedByteArray(app.apiCheckInJsonUtils.buildCheckInJson(checkInJsonString, screenShotMeta, logFileMeta, photoFileMeta, videoFileMeta));
		String jsonBlobMetaSection = String.format(Locale.US, "%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(jsonBlobAsBytes);

		byte[] audioFileAsBytes = new byte[0];
		if (FileUtils.exists(checkInAudioFilePath)) {
			audioFileAsBytes = FileUtils.fileAsByteArray(checkInAudioFilePath);
		}
		String audioFileMetaSection = String.format(Locale.US, "%012d", audioFileAsBytes.length);
		byteArrayOutputStream.write(audioFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(audioFileAsBytes);

		byte[] screenShotFileAsBytes = new byte[0];
		if (FileUtils.exists(screenShotMeta[0])) {
			screenShotFileAsBytes = FileUtils.fileAsByteArray(screenShotMeta[0]);
		}
		String screenShotFileMetaSection = String.format(Locale.US, "%012d", screenShotFileAsBytes.length);
		byteArrayOutputStream.write(screenShotFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(screenShotFileAsBytes);

		byte[] logFileAsBytes = new byte[0];
		if (FileUtils.exists(logFileMeta[0])) {
			logFileAsBytes = FileUtils.fileAsByteArray(logFileMeta[0]);
		}
		String logFileMetaSection = String.format(Locale.US, "%012d", logFileAsBytes.length);
		byteArrayOutputStream.write(logFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(logFileAsBytes);

        byte[] photoFileAsBytes = new byte[0];
        if (FileUtils.exists(photoFileMeta[0])) {
            photoFileAsBytes = FileUtils.fileAsByteArray(photoFileMeta[0]);
        }
        String photoFileMetaSection = String.format(Locale.US, "%012d", photoFileAsBytes.length);
        byteArrayOutputStream.write(photoFileMetaSection.getBytes(StandardCharsets.UTF_8));
        byteArrayOutputStream.write(photoFileAsBytes);

		byte[] videoFileAsBytes = new byte[0];
		if (FileUtils.exists(videoFileMeta[0])) {
			videoFileAsBytes = FileUtils.fileAsByteArray(videoFileMeta[0]);
		}
		String videoFileMetaSection = String.format(Locale.US, "%012d", videoFileAsBytes.length);
		byteArrayOutputStream.write(videoFileMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(videoFileAsBytes);

		byteArrayOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}

	public void sendMqttCheckIn(String[] checkInDbEntry) {

		String audioId = checkInDbEntry[1].substring(0, checkInDbEntry[1].lastIndexOf("."));
		String audioPath = checkInDbEntry[4];
		String audioJson = checkInDbEntry[2];

		try {

			if (FileUtils.exists(audioPath)) {

				byte[] checkInPayload = packageMqttCheckInPayload(audioJson, audioPath);

				app.apiCheckInHealthUtils.updateInFlightCheckInOnSend(audioId, checkInDbEntry);
				this.inFlightCheckInAttemptCounter++;

				app.apiCheckInDb.dbQueued.incrementSingleRowAttempts(audioId);
				long msgSendStart = publishMessageOnConfirmedConnection(this.mqttTopic_Publish_CheckIn, 1,true, checkInPayload);

				app.apiCheckInHealthUtils.setInFlightCheckInStats(audioId, msgSendStart, 0, checkInPayload.length);
				this.inFlightCheckInAttemptCounter = 0;

			} else {
				app.assetUtils.purgeSingleAsset("audio", app.rfcxGuardianIdentity.getGuid(), app.getApplicationContext(), audioId);
			}

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e);
			handleMqttCheckInPublicationExceptions(e, audioId);
		}
	}


	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) {

		byte[] messagePayload = mqttMessage.getPayload();
		Log.i(logTag, "Received "+FileUtils.bytesAsReadableString(messagePayload.length)+" on '"+messageTopic+"' at "+DateTimeUtils.getDateTime());

		// this is a command message receive from the API
		if (messageTopic.equalsIgnoreCase(this.mqttTopic_Subscribe_Command)) {
			app.apiCommandUtils.processApiCommandJson(StringUtils.gZippedByteArrayToUnGZippedString(messagePayload), "mqtt");
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {

		try {

			if (deliveryToken.getTopics().length > 0) {

				String msgTopic = deliveryToken.getTopics()[0];
				Log.i(logTag, "Completed delivery to '"+msgTopic+"' at "+DateTimeUtils.getDateTime());

				if (msgTopic.equalsIgnoreCase(this.mqttTopic_Publish_CheckIn)) {
					app.apiCheckInUtils.moveCheckInEntryToSentDatabase(app.apiCheckInHealthUtils.getInFlightCheckInAudioId());
					long publishDuration = Math.abs(DateTimeUtils.timeStampDifferenceFromNowInMilliSeconds( app.apiCheckInHealthUtils.getCurrentInFlightCheckInStatsEntry()[0] ));
					app.apiCheckInHealthUtils.setInFlightCheckInStats(app.apiCheckInHealthUtils.getInFlightCheckInAudioId(), 0, publishDuration, 0);
					this.checkInPublishCompletedAt = System.currentTimeMillis();
					String publishDurationReadable = DateTimeUtils.milliSecondDurationAsReadableString(publishDuration, true);
					SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHED, publishDurationReadable);
					Log.i(logTag, "CheckIn delivery time: " + publishDurationReadable);
				}

			} else {
				Log.e(logTag, "Message was delivered, but the topic could not be determined.");
			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "deliveryComplete");
		}
	}

	@Override
	public void connectionLost(Throwable cause) {
		try {
			Log.e(logTag, "Connection lost. "
							+ DateTimeUtils.timeStampDifferenceFromNowAsReadableString( app.apiCheckInHealthUtils.getCurrentInFlightCheckInStatsEntry()[0] )
							+ " since last CheckIn publication was launched");
		} catch (Exception e) {
			RfcxLog.logExc(logTag, e, "connectionLost");
		}

		RfcxLog.logThrowable(logTag, cause);

		confirmOrCreateConnectionToBroker(false);
	}

	public void confirmOrCreateConnectionToBroker(boolean overrideDelayBetweenAttempts) {

		long minDelayBetweenConnectionAttempts = 10000;

		if (overrideDelayBetweenAttempts || (mqttCheckInClient.mqttBrokerConnectionLastAttemptedAt < (app.deviceConnectivity.lastConnectedAt() - minDelayBetweenConnectionAttempts))) {
			try {

				setOrResetBrokerConfig();

				mqttCheckInClient.confirmOrCreateConnectionToBroker(this.app.deviceConnectivity.isConnected());

				if (mqttCheckInClient.mqttBrokerConnectionLatency > 0) {

					Log.v(logTag, "MQTT Broker Latency: Connection: "+mqttCheckInClient.mqttBrokerConnectionLatency+" ms, Subscription: "+mqttCheckInClient.mqttBrokerSubscriptionLatency+" ms");

					app.deviceSystemDb.dbMqttBrokerConnections.insert(new Date(),
													mqttCheckInClient.mqttBrokerConnectionLatency,
													mqttCheckInClient.mqttBrokerSubscriptionLatency,
													app.rfcxPrefs.getPrefAsString("api_mqtt_protocol"),
													app.rfcxPrefs.getPrefAsString("api_mqtt_host"),
													app.rfcxPrefs.getPrefAsInt("api_mqtt_port"));

					app.rfcxServiceHandler.triggerService("ApiCheckInJob", false);
				}
			} catch (MqttException e) {
				RfcxLog.logExc(logTag, e, "confirmOrCreateConnectionToBroker");
			}
		} else {
//			Log.e(logTag, "Last broker connection attempt was less than " + DateTimeUtils.milliSecondDurationAsReadableString(minDelayBetweenConnectionAttempts) + " ago");
		}
	}

	private long publishMessageOnConfirmedConnection(String publishTopic, int publishQoS, boolean trackDuration, byte[] messageByteArray) throws MqttException {
		confirmOrCreateConnectionToBroker(true);
		if (publishTopic.equalsIgnoreCase(this.mqttTopic_Publish_CheckIn)) { SocketManager.INSTANCE.sendCheckInTestMessage(SocketManager.CheckInState.PUBLISHING, null); }
		return this.mqttCheckInClient.publishMessage(publishTopic, publishQoS, trackDuration, messageByteArray);
	}

	public boolean isConnectedToBroker() {
		return mqttCheckInClient.isConnected();
	}

	// Ping Messages

	public void sendMqttPing(boolean includeAllExtraFields, String[] includeExtraFields) {

		try {

			publishMessageOnConfirmedConnection(this.mqttTopic_Publish_Ping, 1,false, packageMqttPingPayload(app.apiPingJsonUtils.buildPingJson(includeAllExtraFields, includeExtraFields, true)));

		} catch (Exception e) {

			RfcxLog.logExc(logTag, e, "sendMqttPing");
			handleMqttPingPublicationExceptions(e);

			/////
			try {
			app.apiSegmentUtils.constructSegmentsGroupForQueue("png", "sms", app.apiPingJsonUtils.buildPingJson(includeAllExtraFields, includeExtraFields, true), null);
			} catch (Exception ex) {
				RfcxLog.logExc(logTag, ex, "sendSmsPing");
			}
			/////////

		}
	}

	public void sendMqttPing() {
		sendMqttPing(true, new String[]{});
	}

	private byte[] packageMqttPingPayload(String pingJsonString) throws UnsupportedEncodingException, IOException, JSONException {

		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		// Build JSON blob
		byte[] jsonBlobAsBytes = StringUtils.stringToGZippedByteArray(pingJsonString);
		String jsonBlobMetaSection = String.format(Locale.US, "%012d", jsonBlobAsBytes.length);
		byteArrayOutputStream.write(jsonBlobMetaSection.getBytes(StandardCharsets.UTF_8));
		byteArrayOutputStream.write(jsonBlobAsBytes);

		byteArrayOutputStream.close();

		return byteArrayOutputStream.toByteArray();
	}

	private void handleMqttCheckInPublicationExceptions(Exception inputExc, String audioId) {

		try {
			String excStr = RfcxLog.getExceptionContentAsString(inputExc);

			boolean isTimedOut = excStr.contains("Timed out waiting for a response from the server");
			boolean tooManyPublishes = excStr.contains("Too many publishes in progress");
			boolean badUserNameOrPswd = excStr.contains("Bad user name or password");
			boolean unknownHost = excStr.contains("UnknownHostException");
			boolean brokenPipe = excStr.contains("Broken pipe");
			boolean noRouteToHost = excStr.contains("No route to host");
			boolean unresolvedHost = excStr.contains("Host is unresolved");
			boolean unableToConnect = excStr.contains("Unable to connect to server");

			boolean brokerConnectionLost = excStr.contains("java.io.IOException: Connection is lost.");
			boolean unexpectedError = excStr.contains("Message: Unexpected error");

			if ( unknownHost || brokenPipe || noRouteToHost || unresolvedHost || unableToConnect || tooManyPublishes || isTimedOut ) {

				if (!isTimedOut) {
					Log.v(logTag, "Connection has failed " + this.inFlightCheckInAttemptCounter + " times (max: " + this.inFlightCheckInAttemptCounterLimit + ")");
					app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
				}

				if (this.inFlightCheckInAttemptCounter >= this.inFlightCheckInAttemptCounterLimit){
					Log.v(logTag, "Max Connection Failure Loop Reached: Airplane Mode will be toggled.");
					app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getApplicationContext().getContentResolver());
					this.inFlightCheckInAttemptCounter = 0;
				}

				if (isTimedOut || (tooManyPublishes && (this.inFlightCheckInAttemptCounter > 1))) {
					Log.v(logTag, "Kill ApiCheckInJob Service, Close MQTT Connection & Re-Connect");
					app.rfcxServiceHandler.stopService("ApiCheckInQueue");
					this.mqttCheckInClient.closeConnection();
					confirmOrCreateConnectionToBroker(true);
				}

			} else if ( badUserNameOrPswd || brokerConnectionLost || unexpectedError ) {

				String logErrorMsg = "";
				if (badUserNameOrPswd) { logErrorMsg = "Broker Credentials Rejected."; }
				else if (brokerConnectionLost) { logErrorMsg = "Broker Connection Lost."; }
				else if (unexpectedError) { logErrorMsg = "Unexpected Error."; }

				// This might be something we should remove if we find out that 'Connection Lost" isn't always due to the broker itself having problems
				// This line assumes that the issue is NOT with the Guardian's internet connection
				if (app.deviceConnectivity.isConnected()) {
					initializeFailedCheckInThresholds();
				}

				long additionalDelay = Math.round(this.app.rfcxPrefs.getPrefAsLong("audio_cycle_duration") * 0.667);
				Log.e(logTag, logErrorMsg+" Delaying "+additionalDelay+" seconds before trying again...");
				Thread.sleep(additionalDelay*1000);

			}

		} catch (Exception e) {
			RfcxLog.logExc(logTag, e);
		}
	}

    private void handleMqttPingPublicationExceptions(Exception inputExc) {

        try {
            String excStr = RfcxLog.getExceptionContentAsString(inputExc);

            if (excStr.contains("Too many publishes in progress")) {
//                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
//                app.rfcxServiceHandler.triggerService("ApiCheckInJob", true);

            } else if (	excStr.contains("UnknownHostException")
                    ||	excStr.contains("Broken pipe")
                    ||	excStr.contains("Timed out waiting for a response from the server")
                    ||	excStr.contains("No route to host")
                    ||	excStr.contains("Host is unresolved")
            ) {
//                Log.i(logTag, "Connection has failed "+this.inFlightCheckInAttemptCounter +" times (max: "+this.inFlightCheckInAttemptCounterLimit +")");
//                app.apiCheckInDb.dbQueued.decrementSingleRowAttempts(audioId);
//                if (this.inFlightCheckInAttemptCounter >= this.inFlightCheckInAttemptCounterLimit) {
//                    Log.d(logTag, "Max Connection Failure Loop Reached: Airplane Mode will be toggled.");
//                    app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle", app.getApplicationContext().getContentResolver());
//                    this.inFlightCheckInAttemptCounter = 0;
//                }
            }
        } catch (Exception e) {
            RfcxLog.logExc(logTag, e, "handlePingPublicationExceptions");
        }
    }



	public void initializeFailedCheckInThresholds() {

		String[] checkInThresholdsStr = TextUtils.split(app.rfcxPrefs.getPrefAsString("checkin_failure_thresholds"), ",");

		int[] checkInThresholds = new int[checkInThresholdsStr.length];
		boolean[] checkInThresholdsReached = new boolean[checkInThresholdsStr.length];

		for (int i = 0; i < checkInThresholdsStr.length; i++) {
			try {
				checkInThresholds[i] = Integer.parseInt(checkInThresholdsStr[i]);
				checkInThresholdsReached[i] = false;
			} catch (Exception e) {
				RfcxLog.logExc(logTag, e);
			}
		}

		this.failedCheckInThresholds = checkInThresholds;
		this.failedCheckInThresholdsReached = checkInThresholdsReached;

		this.checkInPublishCompletedAt = System.currentTimeMillis();

		Log.v(logTag, "Resetting CheckIn Failure Thresholds");
	}

	public void updateFailedCheckInThresholds() {

		if (this.failedCheckInThresholds.length > 0) {

			int minsSinceSuccess = (int) Math.floor(((System.currentTimeMillis() - this.checkInPublishCompletedAt) / 1000) / 60);
			//		int minsSinceConnected = (int) Math.floor(((System.currentTimeMillis() - app.deviceConnectivity.lastConnectedAt()) / 1000) / 60);

			if (	// ...we haven't yet reached the first threshold for bad connectivity
					(minsSinceSuccess < this.failedCheckInThresholds[0])
							// OR... we are explicitly in offline mode
							|| app.apiCheckInHealthUtils.isApiCheckInDisabled(false)
							// OR... checkins are explicitly paused due to low battery level
							|| !app.apiCheckInUtils.isBatteryChargeSufficientForCheckIn()
				// OR... this is likely the first checkin after a period of disconnection
				//	|| (app.deviceConnectivity.isConnected() && (minsSinceConnected < this.failedCheckInThresholds[0]))
			) {
				for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
					this.failedCheckInThresholdsReached[i] = false;
				}
			} else {
				int j = 0;
				for (int toggleThreshold : this.failedCheckInThresholds) {
					if ((minsSinceSuccess >= toggleThreshold) && !this.failedCheckInThresholdsReached[j]) {
						this.failedCheckInThresholdsReached[j] = true;
						if (toggleThreshold == this.failedCheckInThresholds[this.failedCheckInThresholds.length - 1]) {
							// last threshold
							if (!app.deviceConnectivity.isConnected() && !app.deviceMobilePhone.hasSim()) {
								Log.d(logTag, "Failure Threshold Reached: Forced reboot due to missing SIM card (" + toggleThreshold
										+ " minutes since last successful CheckIn)");
								app.deviceControlUtils.runOrTriggerDeviceControl("reboot",
										app.getApplicationContext().getContentResolver());
							} else {
								Log.d(logTag, "Failure Threshold Reached: Forced Relaunch (" + toggleThreshold
										+ " minutes since last successful CheckIn)");
								app.deviceControlUtils.runOrTriggerDeviceControl("relaunch",
										app.getApplicationContext().getContentResolver());

								for (int i = 0; i < this.failedCheckInThresholdsReached.length; i++) {
									this.failedCheckInThresholdsReached[i] = false;
								}
								this.inFlightCheckInAttemptCounter = 0;
							}
						} else { //} else if (!app.deviceConnectivity.isConnected()) {
							// any threshold // and not connected
							Log.d(logTag, "Failure Threshold Reached: Airplane Mode (" + toggleThreshold
									+ " minutes since last successful CheckIn)");
							app.deviceControlUtils.runOrTriggerDeviceControl("airplanemode_toggle",
									app.getApplicationContext().getContentResolver());
							this.inFlightCheckInAttemptCounter = 0;
						}
						break;
					}
					j++;
				}
			}
		}
	}

}
