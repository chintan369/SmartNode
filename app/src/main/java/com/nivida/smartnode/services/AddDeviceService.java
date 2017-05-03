package com.nivida.smartnode.services;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class AddDeviceService extends IntentService implements PushCallBack.MessageCallback {

    public static final String NOTIFICATION ="com.nivida.smartnode" ;
    public static final String MESSAGETOSEND = "message";

    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    DatabaseHandler db;

    AppPreference preference;

    int serial=0;

    public AddDeviceService() {
        super("AddDeviceService");


    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        preference=new AppPreference(getApplicationContext());
        db=new DatabaseHandler(getApplicationContext());
        clientId=MqttClient.generateClientId();
        List<String> slaveIDs=db.getAllSlaveIDs();

        try{
            mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientId,new MemoryPersistence());
            mqttClient.setCallback(new PushCallBack(this,this));
            MqttConnectOptions connectOptions=new MqttConnectOptions();
            connectOptions.setUserName(AppConstant.MQTT_USERNAME);
            connectOptions.setPassword(AppConstant.getPassword());
            mqttClient.connect(connectOptions);
            for(int i=0; i<slaveIDs.size(); i++){
                mqttClient.subscribe(db.getSlaveTopic(slaveIDs.get(i))+AppConstant.MQTT_SUBSCRIBE_TOPIC);
                //Log.e("Subscribe MQTT",db.getSlaveTopic(slaveIDs.get(i)));
            }

            Log.e("topic",preference.getTopic()+AppConstant.MQTT_SUBSCRIBE_TOPIC);

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
        //Log.e("MQTT Sub msg :",subscribedMessage);
        Log.e("MQTT :", message);
        try{
            JSONObject object=new JSONObject(message);
            if(object.has("serial")){
                int serailID=object.getInt("serial");
                if(serial!=serailID){
                    setMessageToActivity(message);
                    serial=serailID;
                }
            }
        }catch (JSONException e){
            Log.e("Exception",e.getMessage());
        }
    }
}
