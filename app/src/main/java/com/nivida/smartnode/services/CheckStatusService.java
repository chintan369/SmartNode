package com.nivida.smartnode.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.channels.DatagramChannel;

public class CheckStatusService extends IntentService {

    int count=1;
    AppPreference preference;

    DatagramSocket client_socket;

    int serial=0;
    DatagramChannel channel;

    public static final int DELAY_TIME=5000; //5 Seconds

    public CheckStatusService() {
        super("CheckStatusService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        preference=new AppPreference(getApplicationContext());
        Log.e("Check Service","Started...");
        startChecking();
    }

    private void startChecking() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                new ReceiveUDP().execute();
                startChecking();
            }
        },DELAY_TIME);
    }

    private class ReceiveUDP extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            String message = AppConstant.CMD_GET_MASTER_TOKEN;
            try {
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                Log.e("IP Address Saved", "->" + preference.getIpaddress());

                if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {
                    server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                } else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                }

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
                preference.setIpaddress(recvpacket.getAddress().getHostAddress());

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
            } catch (SocketException s) {
                preference.setOnline(true);
                //C.Toast(getApplicationContext(), s.getLocalizedMessage());
                Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                preference.setOnline(true);
                //C.Toast(getApplicationContext(), e.getLocalizedMessage());
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s == null || s.isEmpty()) {
                preference.setOnline(true);
                //C.Toast(getApplicationContext(), "You are Connected with Internet");
            } else {
                preference.setOnline(false);
                //C.Toast(getApplicationContext(), "You are Connected in LAN");
            }
        }
    }

    @Override
    public void onDestroy() {
        if(client_socket!=null){
            client_socket.disconnect();
            client_socket.close();
            client_socket=null;
        }
        super.onDestroy();
    }
}
