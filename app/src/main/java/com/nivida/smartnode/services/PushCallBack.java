package com.nivida.smartnode.services;

import android.content.ContextWrapper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Nivida new on 01-Aug-16.
 */
public class PushCallBack implements MqttCallback {

    private ContextWrapper context;
    String message="";
    public MessageCallback messageCallback;

    public PushCallBack(ContextWrapper context, MessageCallback callback) {
        this.context = context;
        this.messageCallback=callback;
    }

    @Override
    public void connectionLost(Throwable cause) {

    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        this.message=new String(message.getPayload());
        //Log.e("MQTT message ",new String(message.getPayload()));
        if(messageCallback!=null){
            messageCallback.sendMessage(new String(message.getPayload()));
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    public void setMessageCallback(MessageCallback messageCallback){
        this.messageCallback=messageCallback;
    }

    public interface MessageCallback{
        public void sendMessage(String message);
    }
}
