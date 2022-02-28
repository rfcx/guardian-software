package org.rfcx.guardian.utility.network;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.rfcx.guardian.utility.misc.DateTimeUtils;
import org.rfcx.guardian.utility.misc.FileUtils;
import org.rfcx.guardian.utility.rfcx.RfcxLog;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class MqttUtils implements MqttCallback {

    private static final int mqttConnectionTimeoutInSeconds_min = 45;
    private static final int minMqttKeepAliveIntervalInSeconds = 30;
    private final Context context;
    private final String logTag;
    private final List<String> mqttTopics_Subscribe = new ArrayList<String>();
    public long mqttBrokerConnectionLastAttemptedAt = System.currentTimeMillis();
    public long mqttBrokerConnectionLatency = 0;
    public long mqttBrokerSubscriptionLatency = 0;
    private String mqttClientId = null;
    private int mqttBrokerPort = 1883;
    private String mqttBrokerProtocol = "tcp";
    private String mqttBrokerAddress = null;
    private String mqttBrokerUri = null;
    ;
    private String mqttBrokerKeystorePassphrase = null;
    private String mqttBrokerAuthUserName = null;
    private String mqttBrokerAuthPassword = null;
    private MqttClient mqttClient = null;
    private int mqttSubscriptionQoS = 1;
    private MqttCallback mqttCallback = this;
    private long mqttActionTimeout = 0;
    private int mqttConnectionTimeoutInSeconds = mqttConnectionTimeoutInSeconds_min;
    private int mqttKeepAliveIntervalInSeconds = minMqttKeepAliveIntervalInSeconds;
    private long msgSendStart = System.currentTimeMillis();

    public MqttUtils(Context context, String appRole, String guardianGuid) {
        this.context = context;
        this.logTag = RfcxLog.generateLogTag(appRole, "MqttUtils");
        this.mqttClientId = "rfcx-guardian-" + guardianGuid.toLowerCase(Locale.US) + "-" + appRole.toLowerCase(Locale.US);
    }

    private MqttConnectOptions getConnectOptions() throws MqttSecurityException {

        MqttConnectOptions connectOptions = new MqttConnectOptions();

        // More info on options here:
        // https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html

        connectOptions.setCleanSession(true); // If false, both the client and server will maintain state across restarts of the client, the server and the connection.
        connectOptions.setConnectionTimeout(this.mqttConnectionTimeoutInSeconds); // in seconds
        connectOptions.setKeepAliveInterval(this.mqttKeepAliveIntervalInSeconds); // in seconds
        connectOptions.setAutomaticReconnect(false); // automatically attempt to reconnect to the server if the connection is lost
        connectOptions.setMaxInflight(1); // limits how many messages can be sent without receiving acknowledgments

        if (this.mqttBrokerProtocol.equalsIgnoreCase("ssl")) {
            try {
                InputStream brokerKeystore = context.getAssets().open(this.mqttBrokerAddress.replaceAll("\\.", "_") + ".bks");
                connectOptions.setSocketFactory(getSSLSocketFactory(brokerKeystore, mqttBrokerKeystorePassphrase));
            } catch (IOException e) {
                RfcxLog.logExc(logTag, e);
            }
        }

        if ((this.mqttBrokerAuthUserName != null) && (this.mqttBrokerAuthPassword != null)) {
            connectOptions.setUserName(this.mqttBrokerAuthUserName);
            connectOptions.setPassword(this.mqttBrokerAuthPassword.toCharArray());
        }

        return connectOptions;
    }

    public void setActionTimeout(long timeToWaitInMillis) {
        // PLEASE NOTE:
        // In the event of a timeout the action carries on running in the background until it completes.
        // The timeout is used on methods that block while the action is in progress.
        // https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#setTimeToWait-long-
        this.mqttActionTimeout = timeToWaitInMillis;
        Log.v(logTag, "MQTT client action timeout set to: " + DateTimeUtils.milliSecondDurationAsReadableString(this.mqttActionTimeout, true));
    }

    public void setConnectionTimeouts(int connectionTimeoutInSeconds, int keepAliveIntervalInSeconds) {

        boolean reportUpdatedValuesToLog = (this.mqttConnectionTimeoutInSeconds != Math.max(connectionTimeoutInSeconds, mqttConnectionTimeoutInSeconds_min))
                || (this.mqttKeepAliveIntervalInSeconds != Math.min(keepAliveIntervalInSeconds, minMqttKeepAliveIntervalInSeconds));

        this.mqttConnectionTimeoutInSeconds = Math.max(connectionTimeoutInSeconds, mqttConnectionTimeoutInSeconds_min);
        this.mqttKeepAliveIntervalInSeconds = Math.min(keepAliveIntervalInSeconds, minMqttKeepAliveIntervalInSeconds);

        if (reportUpdatedValuesToLog) {
            Log.v(logTag, "MQTT timeouts set for Connection (" + DateTimeUtils.milliSecondDurationAsReadableString(this.mqttConnectionTimeoutInSeconds * 1000) + ") and Keep-Alive Interval (" + DateTimeUtils.milliSecondDurationAsReadableString(this.mqttKeepAliveIntervalInSeconds * 1000) + ")");
        }
    }

    public void addSubscribeTopic(String subscribeTopic) {
        this.mqttTopics_Subscribe.add(subscribeTopic);
    }

    public long publishMessage(String publishTopic, int publishQoS, boolean trackDeliveryDuration, byte[] messageByteArray) throws MqttException {

        if (confirmOrCreateConnectionToBroker(true)) {
            Log.i(logTag, "Publishing " + FileUtils.bytesAsReadableString(messageByteArray.length) + " to '" + publishTopic + "' (QoS: " + publishQoS + ") at " + DateTimeUtils.getDateTime());
            if (trackDeliveryDuration) {
                this.msgSendStart = System.currentTimeMillis();
            }
            this.mqttClient.publish(publishTopic, buildMessage(publishQoS, messageByteArray));
        } else {
            Log.e(logTag, "Message could not be sent because connection could not be created...");
        }
        return trackDeliveryDuration ? this.msgSendStart : System.currentTimeMillis();
    }

    public void setOrResetBroker(String protocol, int port, String address, String keystorePassphrase, String authUserName, String authPassword) {
        this.mqttBrokerProtocol = protocol;
        this.mqttBrokerPort = port;
        this.mqttBrokerAddress = address;
        this.mqttBrokerKeystorePassphrase = keystorePassphrase;
        this.mqttBrokerAuthUserName = authUserName;
        this.mqttBrokerAuthPassword = authPassword;

        String newUri = this.mqttBrokerProtocol + "://" + this.mqttBrokerAddress + ":" + this.mqttBrokerPort;
        if (!newUri.equals(this.mqttBrokerUri) && (this.mqttClient != null) && this.mqttClient.isConnected()) {
            closeConnection();
        }
        this.mqttBrokerUri = newUri;
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
                RfcxLog.logExc(logTag, e, "confirmOrCreateConnectionToBroker");
                throw new MqttException(MqttException.REASON_CODE_UNEXPECTED_ERROR);
            }

            Log.v(logTag, "Connecting to MQTT broker: " + this.mqttBrokerUri);
            this.mqttClient.connect(options);

            mqttBrokerConnectionLatency = System.currentTimeMillis() - mqttBrokerConnectionLastAttemptedAt;
            Log.v(logTag, "Connected to MQTT broker: " + this.mqttBrokerUri
                    + ((this.mqttBrokerAuthUserName != null) ? " (Auth User: '" + this.mqttBrokerAuthUserName + "')" : "")
            );

            mqttBrokerSubscriptionLatency = 0;
            for (String subscribeTopic : this.mqttTopics_Subscribe) {
                this.mqttClient.subscribe(subscribeTopic, this.mqttSubscriptionQoS);
                Log.v(logTag, "Subscribed to MQTT topic: " + subscribeTopic + " (QoS: " + this.mqttSubscriptionQoS + ")");
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

    public void closeConnection() {
        try {
            this.mqttClient.disconnect();
            Log.v(logTag, "MQTT client disconnected");
        } catch (MqttException e) {
            RfcxLog.logExc(logTag, e);
        }
    }

    private MqttMessage buildMessage(int publishQoS, byte[] messageByteArray) {
        MqttMessage mqttMessage = new MqttMessage(messageByteArray);
        mqttMessage.setQos(publishQoS);
        mqttMessage.setRetained(false);
        return mqttMessage;
    }

    @Override
    public void messageArrived(String messageTopic, MqttMessage mqttMessage) {
        Log.i(this.logTag, "Message Arrived: " + new String(mqttMessage.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken deliveryToken) {
        long checkInDuration = System.currentTimeMillis() - this.msgSendStart;
        Log.i(this.logTag, "Delivery Complete: " + DateTimeUtils.milliSecondDurationAsReadableString(checkInDuration, true));
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.e(this.logTag, "Connection Lost");
        RfcxLog.logThrowable(logTag, cause);
    }

    private SSLSocketFactory getSSLSocketFactory(InputStream keyStoreInputStream, String passphrase) throws MqttSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(keyStoreInputStream, passphrase.toCharArray());
            TrustManagerFactory tmFactory = TrustManagerFactory.getInstance("X509");
            tmFactory.init(keyStore);
            TrustManager[] trustManager = tmFactory.getTrustManagers();
            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustManager, null);
            return new TLSSocketFactory(sslContext.getSocketFactory(),
                    new String[]{
                            //	"TLSv1",
                            //	"TLSv1.1",//,  // v1.1 and v1.2 require enabling at the Android OS level
                            "TLSv1.2"
                    });
        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new MqttSecurityException(e);
        }
    }
}
