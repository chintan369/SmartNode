package com.nivida.smartnode.services;

import android.content.ContextWrapper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Nivida new on 01-Aug-16.
 */
public class PushCallBack implements MqttCallback {

    public MessageCallback messageCallback;
    String message = "";
    private ContextWrapper context;

    public PushCallBack(ContextWrapper context, MessageCallback callback) {
        this.context = context;
        this.messageCallback = callback;
    }

    @Override
    public void connectionLost(Throwable cause) {
        Log.e("MQTT", "Connection Lost");
        messageCallback.setConnectionLost();
        messageCallback.reSubscribe();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        this.message = new String(message.getPayload());
        //Log.e("MQTT message ",new String(message.getPayload()));
        if (messageCallback != null) {
            messageCallback.sendMessage(new String(message.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            Log.e("MQTT", "Message Delivery Complete : " + token.getMessage().getPayload().toString());
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e("MQTT", "Message Delivery Complete : ");
        }
    }

    public void setMessageCallback(MessageCallback messageCallback) {
        this.messageCallback = messageCallback;
    }

    public interface MessageCallback {
        public void sendMessage(String message);

        public void reSubscribe();

        void setConnectionLost();
    }
}
