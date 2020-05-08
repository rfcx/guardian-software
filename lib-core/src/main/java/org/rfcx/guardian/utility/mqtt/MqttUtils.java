package org.rfcx.guardian.utility.mqtt;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.content.Context;
import android.util.Log;

import org.rfcx.guardian.utility.R;
import org.rfcx.guardian.utility.datetime.DateTimeUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

public class MqttUtils implements MqttCallback {

	public MqttUtils(Context context, String appRole, String guardianGuid) {
		this.context = context;
		this.logTag = RfcxLog.generateLogTag(appRole, "MqttUtils");
		this.mqttClientId = "rfcx-guardian-" + guardianGuid.toLowerCase(Locale.US) + "-" + appRole.toLowerCase(Locale.US);
	}

	private Context context;
	private String logTag;
	
	private int mqttQos = 2; // QoS - Send message exactly once
	private String mqttClientId = null;
	private int mqttBrokerPort = 1883;
	private String mqttBrokerProtocol = "tcp";
	private String mqttBrokerAddress = null;
	private String mqttBrokerUri = null;
	private String keystorePassphrase = "tr33PROtect10n"; // TODO make this a preference for added security
	private MqttClient mqttClient = null;
	private List<String> mqttTopics_Subscribe = new ArrayList<String>();;
	private MqttCallback mqttCallback = this;
	
	private long mqttActionTimeout = 0;
	
	private long msgSendStart = System.currentTimeMillis();
	
	private MqttConnectOptions getConnectOptions() throws MqttSecurityException {
		
		MqttConnectOptions options = new MqttConnectOptions();
		
		// More info on options here:
		// https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html

		options.setCleanSession(true); // If false, both the client and server will maintain state across restarts of the client, the server and the connection.
		options.setConnectionTimeout(20); // in seconds
		options.setKeepAliveInterval(40); // in seconds
		options.setAutomaticReconnect(false); // automatically attempt to reconnect to the server if the connection is lost
		options.setMaxInflight(1); // limits how many messages can be sent without receiving acknowledgments

		if (this.mqttBrokerProtocol.equalsIgnoreCase("ssl")) {
			// Requires res/raw/server.bks (see bin/convert-mqtt-certs.sh)
			InputStream keystore = context.getResources().openRawResource(R.raw.server);
			options.setSocketFactory(getSSLSocketFactory(keystore, keystorePassphrase));
		}
		
		return options;
	}
	
	public void setActionTimeout(long timeToWaitInMillis) {
		// PLEASE NOTE:
		// In the event of a timeout the action carries on running in the background until it completes. 
		// The timeout is used on methods that block while the action is in progress.
		// https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#setTimeToWait-long-
		this.mqttActionTimeout = timeToWaitInMillis;
		Log.v(logTag, "MQTT client action timeout set to: "+DateTimeUtils.milliSecondDurationAsReadableString(this.mqttActionTimeout, true));
	}
	
	public void addSubscribeTopic(String subscribeTopic) {
		this.mqttTopics_Subscribe.add(subscribeTopic);
	}
	
	public long mqttBrokerConnectionLastAttemptedAt = System.currentTimeMillis();
	public long mqttBrokerConnectionLatency = 0;
	public long mqttBrokerSubscriptionLatency = 0;
	
	public long publishMessage(String publishTopic, byte[] messageByteArray) throws MqttPersistenceException, MqttException {
		if (confirmOrCreateConnectionToBroker(true)) {
			Log.i(logTag, "Publishing " + messageByteArray.length + " bytes to '" + publishTopic + "' at " + DateTimeUtils.getDateTime(new Date()));
			this.msgSendStart = System.currentTimeMillis();
			this.mqttClient.publish(publishTopic, buildMessage(messageByteArray));

		} else {
			Log.e(logTag, "Message could not be sent because connection could not be created...");
		}
		return this.msgSendStart;
	}
	
	public void setOrResetBroker(String protocol, int port, String address) {
		mqttBrokerProtocol = protocol;
		mqttBrokerPort = port;
		mqttBrokerAddress = address;

		String newUri = mqttBrokerProtocol + "://" + mqttBrokerAddress + ":" + mqttBrokerPort;
		if (!newUri.equals(mqttBrokerUri) && (mqttClient != null) && mqttClient.isConnected()) {
			closeConnection();
		}
		mqttBrokerUri = newUri;
	}
	
	public boolean confirmOrCreateConnectionToBroker(boolean allowBasedOnDeviceConnectivity) throws MqttException {
		
		mqttBrokerConnectionLastAttemptedAt = System.currentTimeMillis();
		mqttBrokerConnectionLatency = 0;
		mqttBrokerSubscriptionLatency = 0;

		if (allowBasedOnDeviceConnectivity && ((this.mqttClient == null) || !this.mqttClient.isConnected())) {
				
			this.mqttClient = new MqttClient(this.mqttBrokerUri, this.mqttClientId, new MemoryPersistence());
			
			this.mqttClient.setTimeToWait(this.mqttActionTimeout);	
			this.mqttClient.setCallback(this.mqttCallback);

			MqttConnectOptions options;
			try {
				options = getConnectOptions();
			} catch (Exception e) {
				e.printStackTrace();
				throw new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
			}

			Log.v(logTag, "Connecting to MQTT broker: "+this.mqttBrokerUri);
			this.mqttClient.connect(options);

			mqttBrokerConnectionLatency = System.currentTimeMillis() - mqttBrokerConnectionLastAttemptedAt;
			Log.v(logTag, "Connected to MQTT broker: "+this.mqttBrokerUri);

			mqttBrokerSubscriptionLatency = 0;
			for (String subscribeTopic : this.mqttTopics_Subscribe) {
				this.mqttClient.subscribe(subscribeTopic);
				Log.v(logTag, "Subscribed to MQTT topic: "+subscribeTopic);
			}
			mqttBrokerSubscriptionLatency = System.currentTimeMillis() - (mqttBrokerConnectionLastAttemptedAt + mqttBrokerConnectionLatency);
				
		}
				
		return allowBasedOnDeviceConnectivity && this.mqttClient.isConnected();
	}

	public boolean isConnected() {
		return ((this.mqttClient != null) && this.mqttClient.isConnected());
	}
	
	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}
	
	private void closeConnection() {
		try {
			this.mqttClient.disconnect();
			Log.v(logTag, "MQTT client disconnected");	
		} catch (MqttException e) {
			RfcxLog.logExc(logTag, e);
		}
	}
	
	private MqttMessage buildMessage(byte[] messageByteArray) {
		MqttMessage mqttMessage = new MqttMessage(messageByteArray);
		mqttMessage.setQos(mqttQos);
		mqttMessage.setRetained(false);
		return mqttMessage;
	}
	
	@Override
	public void messageArrived(String messageTopic, MqttMessage mqttMessage) throws Exception {
		Log.i(this.logTag, "Message Arrived: "+new String(mqttMessage.getPayload()));
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
		long checkInDuration = System.currentTimeMillis() - this.msgSendStart;
		Log.i(this.logTag, "Delivery Complete: "+DateTimeUtils.milliSecondDurationAsReadableString(checkInDuration, true));
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		Log.e(this.logTag, "Connection Lost");
		RfcxLog.logThrowable(logTag, cause);
	}

	private SSLSocketFactory getSSLSocketFactory(InputStream keyStore, String passphrase) throws MqttSecurityException {
		try {
			KeyStore ts = KeyStore.getInstance("BKS");
			ts.load(keyStore, passphrase.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
			tmf.init(ts);
			TrustManager[] tm = tmf.getTrustManagers();
			SSLContext ctx = SSLContext.getInstance("TLSv1");
			ctx.init(null, tm, null);
			return ctx.getSocketFactory();
		} catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
			throw new MqttSecurityException(e);
		}
	}
}
