package com.nivida.smartnode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

import static com.nivida.smartnode.utils.NetworkUtility.TYPE_MOBILE;
import static com.nivida.smartnode.utils.NetworkUtility.TYPE_NOT_CONNECTED;
import static com.nivida.smartnode.utils.NetworkUtility.TYPE_WIFI;

public class NetworkChangeReceiver extends BroadcastReceiver {

    AppPreference preference;

    public NetworkChangeReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        preference = new AppPreference(context);
        int conn = NetworkUtility.getConnectivityStatus(context);

        if (conn == TYPE_WIFI) {
            SmartNode.slavesInLocal.clear();
            SmartNode.slavesWorking.clear();
            sendMessageAfterTime(context, false);
            //new ReceiveUDP(context).execute();
        } else if (conn == TYPE_MOBILE) {
            SmartNode.slavesInLocal.clear();
            SmartNode.slavesWorking.clear();
            sendMessageAfterTime(context, true);
        } else if (conn == TYPE_NOT_CONNECTED) {
            SmartNode.slavesInLocal.clear();
            SmartNode.slavesWorking.clear();
        }

        //C.Toast(context,NetworkUtility.getConnectivityStatusString(context));
    }

    private void sendMessageAfterTime(final Context context, boolean mobileData) {
        if (mobileData) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(context);
                        }
                    }, 1000);
                }
            }, 3000);
        } else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(context);
                        }
                    }, 1000);
                }
            }, 3000);
        }

    }

    private void sendMessage(Context context) {
        List<Bean_SlaveGroup> slaveGroupList = new DatabaseHandler(context).getAllSlaveHex();

        for (int i = 0; i < slaveGroupList.size(); i++) {
            JSONObject object = new JSONObject();
            try {
                object.put("cmd", Cmd.STS);
                object.put("slave", slaveGroupList.get(i).getHex_id());
                object.put("token", slaveGroupList.get(i).getSlaveToken());
            } catch (Exception e) {

            }
            new SendUDP(object.toString(), context).execute();
            new SendMQTT(slaveGroupList.get(i).getSlaveTopic() + AppConstant.MQTT_PUBLISH_TOPIC, object.toString()).execute();
        }
    }

    private class ReceiveUDP extends AsyncTask<Void, Void, String> {

        Context context;

        public ReceiveUDP(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            String message = AppConstant.CMD_GET_MASTER_TOKEN;
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                server_addr = new InetSocketAddress(C.getBroadcastAddress(context).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);

                DatagramSocket client_socket;
                client_socket = new DatagramSocket(13000);
                client_socket.setSoTimeout(2500);
                client_socket.setReuseAddress(true);

                String text = "";
                byte[] recieve_data = new byte[2048];
                DatagramPacket recvpacket = new DatagramPacket(recieve_data, recieve_data.length);
                Log.e("Packet", "Object created");
                //client_socket.setSoTimeout(60000);
                client_socket.receive(recvpacket);
                Log.e("Packet :", "Recieved");

                Log.e("Recived IP :", recvpacket.getAddress().getHostAddress());

                text = new String(recieve_data, 0, recvpacket.getLength());
                Log.e("Received Data :", text);

                socket.disconnect();
                socket.close();
                client_socket.disconnect();
                client_socket.close();
                return text;
                /*int port = recvpacket.getPort();

                Log.e("Received port :", String.valueOf(port));
                Log.e("Received Pac Data", recvpacket.getData().toString());*/
            } catch (IOException e) {
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
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
                MqttClient mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, MqttClient.generateClientId(), new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);

                ////Log.e("Command Fired :", command);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(topic, mqttMessage);
                ////Log.e("topic msg", preference.getTopic() + AppConstant.MQTT_PUBLISH_TOPIC + " " + mqttMessage);
                //mqttClient.disconnect();

            } catch (MqttException e) {
                //Log.e("Exception : ", e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    private class SendUDP extends AsyncTask<Void, Void, String> {
        String message;
        String masterName;
        String type;
        boolean showMaster = true;
        Context context;

        public SendUDP(String message, Context context) {
            this.message = message;
            showMaster = true;
            this.context = context;
            //progressbar.setVisibility(View.VISIBLE);
        }

        public SendUDP(String message, String masterName, String type) {
            this.message = message;
            this.masterName = masterName;
            this.type = type;
            showMaster = false;
            //progressbar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            try {
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                //Log.e("IP Address Saved", "->" + preference.getIpaddress());

               /* if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {*/
                server_addr = new InetSocketAddress(C.getBroadcastAddress(context).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                //socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);
                //Log.e("Packet", "Sent");
                /*} else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                    //Log.e("Packet", "Sent");
                }*/

                socket.disconnect();
                socket.close();
            } catch (SocketException s) {
                Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
            //progressbar.setVisibility(View.GONE);

        }
    }
}
