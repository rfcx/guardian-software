package rfcx.utility.mqtt;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import android.util.Log;
import rfcx.utility.datetime.DateTimeUtils;
import rfcx.utility.rfcx.RfcxLog;

public class MqttUtils implements MqttCallback {

	public MqttUtils(String appRole, String guardianGuid) {
		this.logTag = RfcxLog.generateLogTag(appRole, MqttUtils.class);
		this.mqttClientId = (new StringBuilder()).append("rfcx-guardian-").append(guardianGuid.toLowerCase(Locale.US)).append("-").append(appRole.toLowerCase(Locale.US)).toString();
	}
	
	private String logTag = RfcxLog.generateLogTag("Utils", MqttUtils.class);
	
	private String filePath_authCertificate = null;
	private String filePath_authPrivateKey = null;
	
	private int mqttQos = 2; // QoS - Send message exactly once

	private String mqttClientId = null;
	private int mqttBrokerPort = 1883;
	private String mqttBrokerProtocol = "tcp";
	private String mqttBrokerAddress = null;
	private String mqttBrokerUri = null;
	private MqttClient mqttClient = null;
	private List<String> mqttTopics_Subscribe = new ArrayList<String>();;
	private MqttCallback mqttCallback = this;
	
	private long mqttActionTimeout = 0;
	
	private long msgSendStart = System.currentTimeMillis();
	
	private static MqttConnectOptions getConnectOptions() {
		
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		
		// More info on options here:
		// https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html
		
		mqttConnectOptions.setCleanSession(true); // If false, both the client and server will maintain state across restarts of the client, the server and the connection.
		mqttConnectOptions.setConnectionTimeout(20); // in seconds
		mqttConnectOptions.setKeepAliveInterval(40); // in seconds
		mqttConnectOptions.setAutomaticReconnect(false); // automatically attempt to reconnect to the server if the connection is lost
		mqttConnectOptions.setMaxInflight(1); // limits how many messages can be sent without receiving acknowledgments
//		mqttConnectOptions.setSSLProperties(props);
//		mqttConnectOptions.setSSLHostnameVerifier(hostnameVerifier);
//		mqttConnectOptions.setUserName(userName);
//		mqttConnectOptions.setPassword(password);
//		mqttConnectOptions.setWill(topic, payload, qos, retained);
		
		return mqttConnectOptions;
	}
	
	public void setActionTimeout(long timeToWaitInMillis) {
		// PLEASE NOTE:
		// In the event of a timeout the action carries on running in the background until it completes. 
		// The timeout is used on methods that block while the action is in progress.
		// https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#setTimeToWait-long-
		this.mqttActionTimeout = timeToWaitInMillis;
		Log.v(logTag, "MQTT client action timeout set to: "+Math.round(this.mqttActionTimeout/1000)+" seconds");
	}
	
	public void addSubscribeTopic(String subscribeTopic) {
		this.mqttTopics_Subscribe.add(subscribeTopic);
	}
	
	public long mqttBrokerConnectionLastAttemptedAt = System.currentTimeMillis();
	
	public long publishMessage(String publishTopic, byte[] messageByteArray) throws MqttPersistenceException, MqttException {
		if (confirmOrCreateConnectionToBroker(true)) {
			Log.i(logTag, (new StringBuilder()).append("Message (").append(messageByteArray.length).append(" bytes) published to '").append(publishTopic).append("' at ").append(DateTimeUtils.getDateTime(new Date())).toString());
			this.msgSendStart = System.currentTimeMillis();
			this.mqttClient.publish(publishTopic, buildMessage(messageByteArray));
		} else {
			Log.e(logTag, "Message could not be sent because connection could not be created...");
		}
		return this.msgSendStart;
	}
	
	public void setOrResetBroker(String mqttBrokerProtocol, int mqttBrokerPort, String mqttBrokerAddress) {
		this.mqttBrokerProtocol = mqttBrokerProtocol;
		this.mqttBrokerPort = mqttBrokerPort; 
		this.mqttBrokerAddress = mqttBrokerAddress;
		
		String _mqttBrokerUri = ((new StringBuilder()).append(this.mqttBrokerProtocol).append("://").append(this.mqttBrokerAddress).append(":").append(this.mqttBrokerPort)).toString();
		if (!_mqttBrokerUri.equals(this.mqttBrokerUri) && (this.mqttClient != null) && this.mqttClient.isConnected()) { closeConnection(); }
		this.mqttBrokerUri = _mqttBrokerUri;
	}
	
	public boolean confirmOrCreateConnectionToBroker(boolean allowBasedOnDeviceConnectivity) throws MqttException {
		
		mqttBrokerConnectionLastAttemptedAt = System.currentTimeMillis();
		
		if (allowBasedOnDeviceConnectivity && ((this.mqttClient == null) || !this.mqttClient.isConnected())) {
				
			this.mqttClient = new MqttClient(this.mqttBrokerUri, this.mqttClientId, new MemoryPersistence());
			
			this.mqttClient.setTimeToWait(this.mqttActionTimeout);	
			this.mqttClient.setCallback(this.mqttCallback);

			Log.v(logTag, "Connecting to MQTT broker: "+this.mqttBrokerUri);
			this.mqttClient.connect(getConnectOptions());
			Log.v(logTag, "Connected to MQTT broker: "+this.mqttBrokerUri);
			
			for (String subscribeTopic : this.mqttTopics_Subscribe) {
				this.mqttClient.subscribe(subscribeTopic);
				Log.v(logTag, "Subscribed to MQTT topic: "+subscribeTopic);
			}
				
		}
		return allowBasedOnDeviceConnectivity && this.mqttClient.isConnected();
	}
	
	public void setCallback(MqttCallback mqttCallback) {
		this.mqttCallback = mqttCallback;
	}
	
	public void closeConnection() {
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
		Log.i(this.logTag, "Delivery Complete: "+Math.round(checkInDuration/1000)+"s");
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		Log.e(this.logTag, "Connection Lost");
		RfcxLog.logThrowable(logTag, cause);
	}
		
}
