package com.nivida.smartnode.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AddDeviceService extends IntentService implements PushCallBack.MessageCallback {

    public static final String NOTIFICATION ="com.nivida.smartnode" ;
    public static final String MESSAGETOSEND = "message";

    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    DatabaseHandler db;

    AppPreference preference;

    int serial=0;

    Timer timer;

    String TAG="";

    PushCallBack pushCallBack;

    boolean notConnected = true;

    public AddDeviceService() {
        super("AddDeviceService");
        TAG="AddDeviceService";

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

        connectToServer(true);

        startCacheTimer();

    }

    private void connectToServer(boolean isFirstTime) {
        clientId = MqttClient.generateClientId();
        List<Bean_SlaveGroup> slaveIDs = db.getAllSlaveHex();
        pushCallBack = new PushCallBack(this, this);
        try{
            mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientId,new MemoryPersistence());
            mqttClient.setCallback(pushCallBack);
            MqttConnectOptions connectOptions=new MqttConnectOptions();
            connectOptions.setUserName(AppConstant.MQTT_USERNAME);
            connectOptions.setPassword(AppConstant.getPassword());
            connectOptions.setKeepAliveInterval(10);
            mqttClient.connect(connectOptions);
            for(int i=0; i<slaveIDs.size(); i++){
                //Log.e("Subscribbe for",slaveIDs.get(i).getSlaveTopic());
                mqttClient.subscribe(slaveIDs.get(i).getSlaveTopic() + AppConstant.MQTT_SUBSCRIBE_TOPIC);
            }
            notConnected = false;
            if (!isFirstTime) {
                JSONObject object = new JSONObject();
                object.put("cmd", Cmd.INTERNET);
                object.put("message", Cmd.REINTERNET);
                object.put("type", Cmd.CONNECTED);

                setMessageToActivity(object.toString());
                sendSTSMessageAgain();
            }
            SmartNode.isConnectedToInternet = true;
            ////Log.e("topic",preference.getTopic()+AppConstant.MQTT_SUBSCRIBE_TOPIC);

        } catch (MqttException e) {
            e.printStackTrace();
            //Log.e("MQTT Exc Service", e.getMessage());
            SmartNode.isConnectedToInternet = false;
            if (isFirstTime) {
                isFirstTime = false;
                connectToServer(false);
            }
        } catch (Exception e) {
            //Log.e("MQTT Exce",e.getMessage());
            SmartNode.isConnectedToInternet = false;
            if (isFirstTime) {
                isFirstTime = false;
                connectToServer(false);
            }
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
        notConnected = false;
        ////Log.e("MQTT Sub msg :",subscribedMessage);
        //Log.e(TAG, message);
        try{
            JSONObject object=new JSONObject(message);
            if(object.has("serial")){
                int serailID=object.getInt("serial");
                if(serial!=serailID){
                    setMessageToActivity(message);
                    serial=serailID;
                }
            }

            addCommandToCache(object);
        }catch (JSONException e){
            //Log.e("Exception",e.getMessage());
        }
    }

    @Override
    public void reSubscribe() {
        //Log.e("Resubscibe","Called");
        final Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if (!notConnected) {
                    timer.cancel();
                } else {
                    connectToServer(false);
                }
            }
        };
        timer.scheduleAtFixedRate(task, 2000, 2000);
    }

    private void sendSTSMessageAgain() {
        List<Bean_SlaveGroup> slaveIDs = db.getAllSlaveHex();
        for (int i = 0; i < slaveIDs.size(); i++) {
            if (!SmartNode.slavesWorking.contains(slaveIDs.get(i).getHex_id())) {
                JSONObject object = new JSONObject();
                try {
                    object.put("cmd", Cmd.STS);
                    object.put("slave", slaveIDs.get(i).getHex_id());
                    object.put("token", slaveIDs.get(i).getSlaveToken());

                    new SendMQTT(slaveIDs.get(i).getSlaveTopic() + AppConstant.MQTT_PUBLISH_TOPIC, object.toString()).execute();
                } catch (Exception e) {
                    //Log.e("Exception",e.getMessage());
                }
            }
        }
    }

    @Override
    public void setConnectionLost() {
        notConnected = true;
        try {
            SmartNode.isConnectedToInternet = false;
        } catch (Exception e) {
            //Log.e("Exception",e.getMessage());
        }

        try {
            JSONObject object = new JSONObject();
            object.put("cmd", Cmd.INTERNET);
            object.put("message", Cmd.NO_INTERNET);
            object.put("type", Cmd.NOT_CONNECTED);

            setMessageToActivity(object.toString());
        } catch (Exception e) {
            //Log.e("Exception C",e.getMessage());
        }

    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void addCommandToCache(JSONObject object) {
        try {
            if (object.getString("cmd").equals(Cmd.STS)) {
                String slaveID = object.getString("slave");
                if (!SmartNode.slavesWorking.contains(slaveID)) {
                    SmartNode.slavesWorking.add(slaveID);
                }
                String commandCache = SmartNode.slaveCommands.get(slaveID);
                if (commandCache != null && !commandCache.isEmpty()) {
                    JSONArray array = new JSONArray(SmartNode.slaveCommands.get(slaveID));
                    boolean isNotFoundAny = true;
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject currentObject = array.getJSONObject(i);
                        if (currentObject.getString("cmd").equals(Cmd.STS)) {
                            isNotFoundAny = false;
                            array.remove(i);
                            array.put(object);
                            break;
                        }
                    }

                    if (isNotFoundAny) {
                        array.put(object);
                    }
                    SmartNode.slaveCommands.remove(slaveID);
                    SmartNode.slaveCommands.put(slaveID, array.toString());
                } else {
                    JSONArray array = new JSONArray();
                    array.put(object);
                    SmartNode.slaveCommands.put(slaveID, array.toString());
                }
            } else if (object.getString("cmd").equals(Cmd.SET)) {
                String slaveID = object.getString("slave");
                String button = object.getString("button");
                String commandCache = SmartNode.slaveCommands.get(slaveID);

                if (!SmartNode.slavesWorking.contains(slaveID)) {
                    SmartNode.slavesWorking.add(slaveID);
                }

                if (commandCache != null && !commandCache.isEmpty()) {
                    JSONArray array = new JSONArray(SmartNode.slaveCommands.get(slaveID));
                    boolean isNotFoundAny = true;
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject currentObject = array.getJSONObject(i);
                        if (currentObject.getString("cmd").equals(Cmd.SET) && currentObject.getString("slave").equals(slaveID) && currentObject.getString("button").equals(button)) {
                            isNotFoundAny = false;
                            array.remove(i);
                            array.put(object);
                            break;
                        }
                    }
                    if (isNotFoundAny) {
                        array.put(object);
                    }
                    SmartNode.slaveCommands.remove(slaveID);
                    SmartNode.slaveCommands.put(slaveID, array.toString());
                } else {
                    JSONArray array = new JSONArray();
                    array.put(object);
                    SmartNode.slaveCommands.put(slaveID, array.toString());
                }
            } else if (object.getString("cmd").equals(Cmd.UL1)) {
                String slaveID = object.getString("slave");

                if (!SmartNode.slavesWorking.contains(slaveID)) {
                    SmartNode.slavesWorking.add(slaveID);
                }

                if (object.getString("status").equals("success")) {
                    String commandCache = SmartNode.slaveCommands.get(slaveID);
                    String data = object.getString("data");
                    String button = String.valueOf(data.charAt(0)) + String.valueOf(data.charAt(1));

                    if (commandCache != null && !commandCache.isEmpty()) {
                        JSONArray array = new JSONArray(SmartNode.slaveCommands.get(slaveID));
                        boolean isNotFoundAny = true;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject currentObject = array.getJSONObject(i);
                            if (currentObject.getString("cmd").equals(Cmd.UL1) && currentObject.getString("slave").equals(slaveID) && currentObject.getString("data").startsWith(button)) {
                                isNotFoundAny = false;
                                array.remove(i);
                                array.put(object);
                                break;
                            }
                        }
                        if (isNotFoundAny) {
                            array.put(object);
                        }
                        SmartNode.slaveCommands.remove(slaveID);
                        SmartNode.slaveCommands.put(slaveID, array.toString());
                    } else {
                        JSONArray array = new JSONArray();
                        array.put(object);
                        SmartNode.slaveCommands.put(slaveID, array.toString());
                    }
                }
                //Log.e("All Commands",SmartNode.slaveCommands.get(slaveID));
            } else if (object.getString("cmd").equals(Cmd.TL1)) {
                String slaveID = object.getString("slave");

                if (!SmartNode.slavesWorking.contains(slaveID)) {
                    SmartNode.slavesWorking.add(slaveID);
                }

                if (object.getString("status").equals("success")) {
                    String commandCache = SmartNode.slaveCommands.get(slaveID);
                    String data = object.getString("data");
                    String button = String.valueOf(data.charAt(0)) + String.valueOf(data.charAt(1));

                    if (commandCache != null && !commandCache.isEmpty()) {
                        JSONArray array = new JSONArray(SmartNode.slaveCommands.get(slaveID));
                        boolean isNotFoundAny = true;
                        for (int i = 0; i < array.length(); i++) {
                            JSONObject currentObject = array.getJSONObject(i);
                            if (currentObject.getString("cmd").equals(Cmd.TL1) && currentObject.getString("slave").equals(slaveID) && currentObject.getString("data").startsWith(button)) {
                                isNotFoundAny = false;
                                array.remove(i);
                                array.put(object);
                                break;
                            }
                        }
                        if (isNotFoundAny) {
                            array.put(object);
                        }
                        SmartNode.slaveCommands.remove(slaveID);
                        SmartNode.slaveCommands.put(slaveID, array.toString());
                    } else {
                        JSONArray array = new JSONArray();
                        array.put(object);
                        SmartNode.slaveCommands.put(slaveID, array.toString());
                    }
                }
                //Log.e("All Commands",SmartNode.slaveCommands.get(slaveID));
            } else if (object.getString("cmd").equals(Cmd.SCH) && !SmartNode.rSerials.contains(object.getInt("serial"))) {
                SmartNode.rSerials.add(object.getInt("serial"));
                String slaveID = object.getString("slave");
                if (!SmartNode.slavesWorking.contains(slaveID)) {
                    SmartNode.slavesWorking.add(slaveID);
                }

                String commandCache = SmartNode.slaveCommands.get(slaveID);
                String[] data = object.getString("data").split("-");

                if (data[0].equals("I")) {
                    if (commandCache != null && !commandCache.isEmpty()) {
                        boolean isSTSFound = false, isSCHFound = false;
                        String commandSTS = "", commandSCH = "";

                        JSONArray array = new JSONArray(commandCache);
                        for (int a = 0; a < array.length(); a++) {
                            JSONObject object1 = array.getJSONObject(a);
                            if (object1.getString("cmd").equals(Cmd.STS) && object1.getString("slave").equals(slaveID)) {
                                commandSTS = object1.toString();
                                isSTSFound = true;
                                continue;
                            }

                            if (object1.getString("cmd").equals(Cmd.SCH) && object1.getString("slave").equals(slaveID)) {
                                commandSCH = object1.toString();
                                isSCHFound = true;
                            }
                        }


                        if (isSTSFound && isSCHFound) {
                            JSONObject schedule = new JSONObject(commandSCH);
                            JSONObject statusObject = new JSONObject(commandSTS);

                            String scheduleInfoStatus = statusObject.getString("schedule_info");
                            String[] scheduleAllInfo = schedule.getString("data").split("-");
                            String slotAllocated = data[1];

                            if (data[2].equalsIgnoreCase("EMPTY")) {

                                int slotAllocatedInt = Integer.parseInt(slotAllocated);

                                if (scheduleAllInfo[0].equals("A")) {
                                    String availableSlots = scheduleAllInfo[1];
                                    String dataItem = "A-";

                                    String toDeleteNodeNumber = "";

                                    for (int i = 0; i < availableSlots.length(); i += 2) {
                                        String currentSwitchNum = String.valueOf(availableSlots.charAt(i)) + String.valueOf(availableSlots.charAt(i + 1));

                                        int currentSlot = (i / 2);

                                        if (slotAllocatedInt == currentSlot) {
                                            toDeleteNodeNumber = currentSwitchNum;
                                            dataItem += "00";

                                        } else {
                                            dataItem += currentSwitchNum;
                                        }
                                    }

                                    schedule.remove("data");
                                    schedule.put("data", dataItem);

                                    String newScheduleInfo = "";

                                    for (int i = 0; i < scheduleInfoStatus.length(); i += 2) {
                                        int currentNode = (i / 2) + 1;
                                        String nodeNum = "";

                                        String currentValue = String.valueOf(scheduleInfoStatus.charAt(i)) + String.valueOf(scheduleInfoStatus.charAt(i + 1));
                                        int currentValueInt = Integer.parseInt(currentValue);


                                        if (currentNode < 10) {
                                            nodeNum = "0" + currentNode;
                                        } else {
                                            nodeNum = String.valueOf(currentNode);
                                        }

                                        if (toDeleteNodeNumber.equals(nodeNum)) {
                                            if (currentValueInt > 0)
                                                currentValueInt--;
                                            String newValue = "";
                                            if (currentValueInt < 10) {
                                                newValue = "0" + currentValueInt;
                                            } else {
                                                newValue = String.valueOf(currentValueInt);
                                            }
                                            newScheduleInfo += newValue;
                                        } else {
                                            newScheduleInfo += currentValue;
                                        }
                                    }

                                    statusObject.remove("schedule_info");
                                    statusObject.put("schedule_info", newScheduleInfo);

                                    Log.e("Status Obj", statusObject.toString());

                                    commandCache = SmartNode.slaveCommands.get(slaveID);
                                    JSONArray array1 = new JSONArray(commandCache);
                                    for (int a = 0; a < array1.length(); a++) {
                                        JSONObject object1 = array1.getJSONObject(a);

                                        if (object1.getString("cmd").equals(Cmd.SCH) && object1.getString("slave").equals(slaveID)) {
                                            array1.remove(a);
                                            array1.put(schedule);
                                        }
                                    }

                                    for (int a = 0; a < array1.length(); a++) {
                                        JSONObject object1 = array1.getJSONObject(a);
                                        if (object1.getString("cmd").equals(Cmd.STS) && object1.getString("slave").equals(slaveID)) {
                                            array1.remove(a);
                                            array1.put(statusObject);
                                        }
                                    }

                                    Log.e("New Array", array1.toString());

                                    SmartNode.slaveCommands.remove(slaveID);
                                    SmartNode.slaveCommands.put(slaveID, array1.toString());

                                    Log.e("Command Cache", SmartNode.slaveCommands.get(slaveID));
                                }
                            } else {
                                String allocatedSwitch = data[5];
                                int slotAllocatedInt = Integer.parseInt(slotAllocated);

                                if (scheduleAllInfo[0].equals("A")) {
                                    String availableSlots = scheduleAllInfo[1];
                                    String dataItem = "A-";

                                    for (int i = 0; i < availableSlots.length(); i += 2) {
                                        String currentSwitchNum = String.valueOf(availableSlots.charAt(i)) + String.valueOf(availableSlots.charAt(i + 1));

                                        int currentSlot = (i / 2);

                                        if (slotAllocatedInt == currentSlot) {
                                            dataItem += allocatedSwitch;
                                        } else {
                                            dataItem += currentSwitchNum;
                                        }
                                    }

                                    schedule.remove("data");
                                    schedule.put("data", dataItem);

                                    String newScheduleInfo = "";

                                    for (int i = 0; i < scheduleInfoStatus.length(); i += 2) {
                                        int currentNode = (i / 2) + 1;
                                        String nodeNum = "";

                                        String currentValue = String.valueOf(scheduleInfoStatus.charAt(i)) + String.valueOf(scheduleInfoStatus.charAt(i + 1));
                                        int currentValueInt = Integer.parseInt(currentValue);


                                        if (currentNode < 10) {
                                            nodeNum = "0" + currentNode;
                                        } else {
                                            nodeNum = String.valueOf(currentNode);
                                        }

                                        if (allocatedSwitch.equals(nodeNum)) {
                                            currentValueInt++;
                                            String newValue = "";
                                            if (currentValueInt < 10) {
                                                newValue = "0" + currentValueInt;
                                            } else {
                                                newValue = String.valueOf(currentValueInt);
                                            }

                                            newScheduleInfo += newValue;
                                        } else {
                                            newScheduleInfo += currentValue;
                                        }
                                    }

                                    statusObject.remove("schedule_info");
                                    statusObject.put("schedule_info", newScheduleInfo);

                                    commandCache = SmartNode.slaveCommands.get(slaveID);
                                    JSONArray array1 = new JSONArray(commandCache);
                                    for (int a = 0; a < array1.length(); a++) {
                                        JSONObject object1 = array1.getJSONObject(a);

                                        if (object1.getString("cmd").equals(Cmd.SCH) && object1.getString("slave").equals(slaveID)) {
                                            array1.remove(a);
                                            array1.put(schedule);
                                        }
                                    }

                                    for (int a = 0; a < array1.length(); a++) {
                                        JSONObject object1 = array1.getJSONObject(a);
                                        if (object1.getString("cmd").equals(Cmd.STS) && object1.getString("slave").equals(slaveID)) {
                                            array1.remove(a);
                                            array1.put(statusObject);
                                        }
                                    }

                                    SmartNode.slaveCommands.remove(slaveID);
                                    SmartNode.slaveCommands.put(slaveID, array1.toString());

                                    Log.e("Command Cache", SmartNode.slaveCommands.get(slaveID));
                                }
                            }
                        }
                    }
                } else if (data[0].equals("A")) {
                    if (commandCache != null && !commandCache.isEmpty()) {
                        boolean isSCHFound = false;

                        JSONArray array = new JSONArray(commandCache);
                        for (int a = 0; a < array.length(); a++) {
                            JSONObject object1 = array.getJSONObject(a);

                            if (object1.getString("cmd").equals(Cmd.SCH) && object1.getString("slave").equals(slaveID)) {
                                array.remove(a);
                                array.put(object);
                                isSCHFound = true;
                                break;
                            }
                        }

                        if (!isSCHFound) {
                            array.put(object);
                        }

                        SmartNode.slaveCommands.remove(slaveID);
                        SmartNode.slaveCommands.put(slaveID, array.toString());

                    } else {
                        JSONArray array = new JSONArray();
                        array.put(object);
                        SmartNode.slaveCommands.put(slaveID, array.toString());
                    }
                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onDestroy() {
        if (mqttClient != null) {
            mqttClient = null;
        }
        stopCacheTimer();
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        if (mqttClient != null) {
            mqttClient = null;
        }
        stopCacheTimer();
        return super.stopService(name);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (mqttClient != null) {
            mqttClient = null;
        }
        stopCacheTimer();
        //Log.e("MQTTService","Stopped with force");
        stopSelf();
    }

    public void startCacheTimer() {

        final List<Bean_SlaveGroup> slaveIDs = db.getAllSlaveHex();

        timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                for (int i = 0; i < slaveIDs.size(); i++) {

                    String cache = SmartNode.slaveCommands.get(slaveIDs.get(i).getHex_id());

                    if (cache == null || cache.isEmpty()) {

                        JSONObject object = new JSONObject();
                        try {
                            object.put("cmd", Cmd.STS);
                            object.put("slave", slaveIDs.get(i).getHex_id());
                            object.put("token", slaveIDs.get(i).getSlaveToken());

                            if (SmartNode.slavesInLocal.contains(slaveIDs.get(i).getHex_id())) {
                                Log.e("Slave Execution", slaveIDs.get(i).getHex_id());
                                new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            } else {
                                new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                new SendMQTT(slaveIDs.get(i).getSlaveTopic(), object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        timer.scheduleAtFixedRate(task, 2000, 2000);
    }

    private void stopCacheTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    private class SendUDP extends AsyncTask<Void, Void, String> {
        String message;
        String masterName;
        String type;
        boolean showMaster = true;

        public SendUDP(String message) {
            this.message = message;
            showMaster = true;
            //progressbar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);

                socket.disconnect();
                socket.close();
            } catch (SocketException s) {
                Log.e("Exception", "-->" + s.getLocalizedMessage());
            } catch (IOException e) {
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
        }
    }

    public class SendMQTT extends AsyncTask<Void, Void, Void> {

        String topic = "";
        String command = "";

        public SendMQTT(String topic, String command) {
            this.topic = topic;
            this.command = command;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String clientID = MqttClient.generateClientId();
                MqttClient mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientID, new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(topic + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                mqttClient.disconnect();

            } catch (MqttException e) {
                Log.e("Exception M: ", e.getMessage());
                e.printStackTrace();
                SmartNode.isConnectedToInternet = false;
            } catch (Exception e) {
                Log.e("Exception : ", e.getMessage());
            }
            return null;
        }
    }
}
