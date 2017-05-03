package com.nivida.smartnode.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

public class ReceiveService extends IntentService {

    DatagramSocket socket;
    int server_get_port=13000;
    byte[] server_msg=new byte[1500];

    public ReceiveService() {
        super("ReceiveService");

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String broadCastIP=intent.getStringExtra("broadCastIP");
        while (true){
            try{

                Log.e("Service","started");
                DatagramSocket client_socket=new DatagramSocket(13000);
                byte[] recieve_data=new byte[2048];

                DatagramPacket recvpacket=new DatagramPacket(recieve_data,recieve_data.length);
                client_socket.setSoTimeout(60000);
                client_socket.receive(recvpacket);
                Log.e("Packet :","Recieved");

                Log.e("Recived Data :",recvpacket.getAddress().toString());
                Log.e("Received Data :",new String(recieve_data));

                String text=new String(recieve_data,0,recvpacket.getLength());
                Log.e("Received data 2 :",text);

                int port=recvpacket.getPort();

                Log.e("Received port :",String.valueOf(port));
                Log.e("Received Pac Data",recvpacket.getData().toString());
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
