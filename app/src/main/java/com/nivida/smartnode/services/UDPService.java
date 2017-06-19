package com.nivida.smartnode.services;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class UDPService extends IntentService {

    public static final String NOTIFICATION = "com.nivida.smartnode";
    public static final String MESSAGEJSON = "jsondata";
    public static final String DEVICEIP = "deviceip";
    int count = 1;
    AppPreference preference;
    MulticastSocket client_socket;
    DatabaseHandler db;
    IPDb ipDb;

    ArrayList<Integer> recivedSerials = new ArrayList<>();

    HashMap<String, ArrayList<Integer>> serialIDs = new HashMap<>();

    int serial = 0;
    String currentIP = "";
    DatagramChannel channel;

    public UDPService() {
        super("UDPService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        preference = new AppPreference(getApplicationContext());
        db = new DatabaseHandler(getApplicationContext());
        ipDb = new IPDb(getApplicationContext());
        recivedSerials.clear();

        while (true) {

            //Log.e("Count","->"+count);

            try {
                channel = DatagramChannel.open();
                if (client_socket != null && client_socket.isConnected()) {
                    client_socket.disconnect();
                    client_socket.close();
                }

                if (client_socket == null) {
                    //client_socket=channel.socket();
                    client_socket = new MulticastSocket(13000);
                    //client_socket.bind(new InetSocketAddress(13000));
                    client_socket.setSoTimeout(60000);
                    client_socket.setReuseAddress(true);
                    client_socket.setBroadcast(true);
                }

                byte[] recieve_data = new byte[16384];


                DatagramPacket recvpacket = new DatagramPacket(recieve_data, recieve_data.length);
                client_socket.receive(recvpacket);


                String text = new String(recieve_data, 0, recvpacket.getLength());
                //preference.setOnline(false);
                String receivedIP = recvpacket.getAddress().getHostAddress();
                //preference.setCurrentIPAddr(receivedIP);

                if (!text.isEmpty() && !text.equalsIgnoreCase("OK")) {
                    try {
                        JSONObject object = new JSONObject(text);
                        int serialnum = object.getInt("serial");

                        //currentIP=recvpacket.getAddress().getHostAddress();

                        boolean isFirstCmd = false;
                        boolean hasSlaveIn = false;
                        boolean hasDeviceIDIn = false;
                        ArrayList<Integer> serials = new ArrayList<>();
                        if (object.has("device_id")) {
                            hasDeviceIDIn = true;
                            String deviceID = object.get("device_id").toString();
                            if (!SmartNode.deviceIDs.contains(deviceID))
                                SmartNode.deviceIDs.add(deviceID);

                            checkForDeviceRename(object);
                        } else if (object.has("slave")) {
                            hasSlaveIn = true;
                            serials = serialIDs.get(object.getString("slave"));
                            if (serials == null) serials = new ArrayList<>();
                        } else {
                            serials = recivedSerials;
                        }

                        if (hasDeviceIDIn) {
                            count++;
                            isFirstCmd = true;
                            serials.add(serialnum);
                            /*if (count < 10 || count % 25 == 0) {
                                db.setMasterSlaveIPByDeviceID(object.get("device_id").toString(), recvpacket.getAddress().getHostAddress());
                            }*/
                        } else if (!serials.contains(serialnum)) {
                            count++;
                            isFirstCmd = true;
                            if (hasSlaveIn) {
                                serials.add(serialnum);
                                serialIDs.remove(object.getString("slave"));
                                serialIDs.put(object.getString("slave"), serials);
                                String slaveID = object.getString("slave");

                                /*if (count < 10 || count % 25 == 0) {
                                    db.setMasterSlaveIP(slaveID, recvpacket.getAddress().getHostAddress());
                                }*/
                            } else {
                                recivedSerials.add(serialnum);
                            }
                        }

                        //Log.e("Serial in",serialnum+" -- "+serials.size()+" -- "+serials.contains(serialnum));

                        if (isFirstCmd) {
                            setMessageToActivity(text, recvpacket.getAddress().getHostAddress());
                        }

                        addCommandToCache(object);
                    } catch (Exception e) {
                        //Log.e("Exception",e.getMessage());
                    }
                } else {
                    count = 1;
                }
            } catch (SocketException s) {


                //Log.e("Exception UDP",s.getMessage());
                if (client_socket != null) {
                    client_socket.disconnect();
                    client_socket.close();
                }

                client_socket = null;
                preference.setOnline(true);
            } catch (UnknownHostException e) {
                //Log.e("No Such Host",e.getMessage());
                if (client_socket != null) {
                    client_socket.disconnect();
                    client_socket.close();
                }
                client_socket = null;
                preference.setOnline(true);
            } catch (IOException i) {
                //Log.e("IO Exception","->"+i.getMessage());
                if (client_socket != null) {
                    //client_socket.disconnect();
                    //client_socket.close();
                    client_socket = null;
                }
                client_socket = null;
                preference.setOnline(true);
            }

            count++;
        }
    }

    private void checkForDeviceRename(JSONObject object) {
        try {
            String deviceID = object.get("device_id").toString();
            String masterName = object.getString("m_name");

            SmartNode.databaseHandler.renameMaster(deviceID, masterName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        DatabaseHandler db = SmartNode.databaseHandler;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void addCommandToCache(JSONObject object) {
        //Log.e("Cmd Rcvd",object.toString());
        try {
            if (object.getString("cmd").equals(Cmd.STS)) {
                String slaveID = object.getString("slave");
                if (!SmartNode.slavesInLocal.contains(slaveID)) {
                    SmartNode.slavesInLocal.add(slaveID);
                }
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
                //Log.e("All Commands",SmartNode.slaveCommands.get(slaveID));
            } else if (object.getString("cmd").equals(Cmd.SET)) {
                String slaveID = object.getString("slave");
                String button = object.getString("button");
                String commandCache = SmartNode.slaveCommands.get(slaveID);
                if (!SmartNode.slavesInLocal.contains(slaveID)) {
                    SmartNode.slavesInLocal.add(slaveID);
                }
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
                //Log.e("All Commands",SmartNode.slaveCommands.get(slaveID));
            } else if (object.getString("cmd").equals(Cmd.UL1)) {
                String slaveID = object.getString("slave");
                if (!SmartNode.slavesInLocal.contains(slaveID)) {
                    SmartNode.slavesInLocal.add(slaveID);
                }
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
                if (!SmartNode.slavesInLocal.contains(slaveID)) {
                    SmartNode.slavesInLocal.add(slaveID);
                }
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
                if (!SmartNode.slavesInLocal.contains(slaveID)) {
                    SmartNode.slavesInLocal.add(slaveID);
                }
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
                                            currentValueInt = currentValueInt - 1;
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

                                    Log.e("New Schedule Data", schedule.toString());

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
            //Log.e("Exception J",e.getMessage());
        }

    }

    private void setMessageToActivity(String message) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(MESSAGEJSON, message);
        sendBroadcast(intent);
    }

    private void setMessageToActivity(String message, String ipAddress) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(MESSAGEJSON, message);
        intent.putExtra(DEVICEIP, ipAddress);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if (client_socket != null) {
            client_socket = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        if (client_socket != null) {
            client_socket.disconnect();
            client_socket.close();
        }
        return super.stopService(name);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (client_socket != null) {
            client_socket.disconnect();
            client_socket.close();
            client_socket = null;
        }
        //Log.e("UDPService","Stopped with force");
        stopSelf();
    }

    private class UpdateMasterIP extends AsyncTask<Void, Void, Void> {

        boolean bySlave = true;
        String slaveID = "";
        String ipAddress = "";

        public UpdateMasterIP(boolean bySlave, String slaveID, String ipAddress) {
            this.bySlave = bySlave;
            this.slaveID = slaveID;
            this.ipAddress = ipAddress;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            if (bySlave) {
                db.setMasterSlaveIP(slaveID, ipAddress);
            } else {
                db.setMasterSlaveIPByDeviceID(slaveID, ipAddress);
            }

            return null;
        }
    }
}
