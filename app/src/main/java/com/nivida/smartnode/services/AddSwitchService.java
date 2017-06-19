package com.nivida.smartnode.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.nivida.smartnode.app.AppConstant;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class AddSwitchService extends IntentService implements PushCallBack.MessageCallback {

    public static final String NOTIFICATION ="com.nivida.smartnode.AddSwitchService" ;
    public static final String MESSAGETOSEND = "message";

    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";

    public AddSwitchService() {
        super("AddSwitchService");


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        clientId=MqttClient.generateClientId();
        try{
            mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientId,new MemoryPersistence());
            mqttClient.setCallback(new PushCallBack(this,this));
            MqttConnectOptions connectOptions=new MqttConnectOptions();
            connectOptions.setUserName(AppConstant.MQTT_USERNAME);
            connectOptions.setPassword(AppConstant.getPassword());
            mqttClient.connect(connectOptions);

            mqttClient.subscribe(AppConstant.MQTT_SUBSCRIBE_TOPIC);

        } catch (MqttException e) {
            e.printStackTrace();
        }

    }

    private void setMessageToActivity(String message) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(MESSAGETOSEND, message);
        sendBroadcast(intent);
    }

    @Override
    public void sendMessage(String message) {
        subscribedMessage=message;
        Log.e("MQTT Sub msg fr swt :",subscribedMessage);
        Log.e("MQTT from swt srvc :", message);
        setMessageToActivity(message);
    }

    @Override
    public void reSubscribe() {

    }

    @Override
    public void setConnectionLost() {

    }
}
