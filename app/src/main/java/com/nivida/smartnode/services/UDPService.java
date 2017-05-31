package com.nivida.smartnode.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;

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

    public static final String NOTIFICATION ="com.nivida.smartnode" ;
    public static final String MESSAGEJSON = "jsondata";
    public static final String DEVICEIP = "deviceip";
    int count = 1;
    AppPreference preference;
    MulticastSocket client_socket;
    DatabaseHandler db;
    IPDb ipDb;

    ArrayList<Integer> recivedSerials=new ArrayList<>();

    HashMap<String,ArrayList<Integer>> serialIDs=new HashMap<>();

    int serial=0;
    String currentIP="";
    DatagramChannel channel;

    public UDPService() {
        super("UDPService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        preference=new AppPreference(getApplicationContext());
        db=new DatabaseHandler(getApplicationContext());
        ipDb=new IPDb(getApplicationContext());
        recivedSerials.clear();

        while (true){

            //Log.e("Count","->"+count);

            try{
                channel = DatagramChannel.open();
                if(client_socket!=null && client_socket.isConnected()){
                    client_socket.disconnect();
                    client_socket.close();
                }

                if(client_socket==null){
                    //client_socket=channel.socket();
                    client_socket=new MulticastSocket(13000);
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
                String receivedIP=recvpacket.getAddress().getHostAddress();
                //preference.setCurrentIPAddr(receivedIP);

                if(!text.isEmpty() && !text.equalsIgnoreCase("OK")){
                    try{
                        JSONObject object=new JSONObject(text);
                        int serialnum=object.getInt("serial");

                        //currentIP=recvpacket.getAddress().getHostAddress();

                        boolean isFirstCmd=false;
                        boolean hasSlaveIn=false;
                        ArrayList<Integer> serials=new ArrayList<>();
                        if(object.has("slave")){
                            hasSlaveIn=true;
                            serials = serialIDs.get(object.getString("slave"));
                            if(serials==null) serials=new ArrayList<>();
                        }
                        else {
                            serials=recivedSerials;
                        }

                        if(!serials.contains(serialnum)){
                            count++;
                            isFirstCmd=true;
                            if(hasSlaveIn){
                                serials.add(serialnum);
                                serialIDs.remove(object.getString("slave"));
                                serialIDs.put(object.getString("slave"), serials);
                                String slaveID = object.getString("slave");

                                if (count == 1 || count % 25 == 0) {
                                    db.setMasterSlaveIP(slaveID, recvpacket.getAddress().getHostAddress());
                                }

                            }
                            else {
                                recivedSerials.add(serialnum);
                            }
                        }

                        //Log.e("Serial in",serialnum+" -- "+serials.size()+" -- "+serials.contains(serialnum));

                        if(isFirstCmd){
                            setMessageToActivity(text,recvpacket.getAddress().getHostAddress());

                            //Log.e("Packet :", "Received In Service");
                            //Log.e("Received IP :", recvpacket.getAddress().getHostAddress());
                            if (count == 1 || count % 25 == 0) {
                                ipDb.addIP(recvpacket.getAddress().getHostAddress());
                            }
                            //Log.e("UDP Data :", text);
                        }
                    }catch (Exception e){
                        Log.e("Exception",e.getMessage());
                    }
                }
                else {
                    count=1;
                }
            }catch (SocketException s){


                //Log.e("Exception UDP",s.getMessage());
                if(client_socket!=null){
                    client_socket.disconnect();
                    client_socket.close();
                }

                client_socket=null;
                preference.setOnline(true);
            }catch (UnknownHostException e){
                //Log.e("No Such Host",e.getMessage());
                if(client_socket!=null){
                    client_socket.disconnect();
                    client_socket.close();
                }
                client_socket=null;
                preference.setOnline(true);
            }catch (IOException i){
                //Log.e("IO Exception","->"+i.getMessage());
                if(client_socket!=null){
                    //client_socket.disconnect();
                    //client_socket.close();
                    client_socket=null;
                }
                client_socket=null;
                preference.setOnline(true);
            }

            count++;
        }
    }

    private void setMessageToActivity(String message) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(MESSAGEJSON, message);
        sendBroadcast(intent);
    }

    private void setMessageToActivity(String message,String ipAddress) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(MESSAGEJSON, message);
        intent.putExtra(DEVICEIP,ipAddress);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        if(client_socket!=null){
            client_socket=null;
        }
        super.onDestroy();
    }

    @Override
    public boolean stopService(Intent name) {
        if(client_socket!=null){
            client_socket.disconnect();
            client_socket.close();
        }
        return super.stopService(name);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if(client_socket!=null){
            client_socket.disconnect();
            client_socket.close();
            client_socket=null;
        }
        Log.e("UDPService","Stopped with force");
        stopSelf();
    }
}
