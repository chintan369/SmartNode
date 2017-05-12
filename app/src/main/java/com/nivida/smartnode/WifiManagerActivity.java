package com.nivida.smartnode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WifiManagerActivity extends AppCompatActivity {

    WifiManager wifi;
    ListView list_wifi;
    TextView txt_status,txt_scan;
    int size = 0;
    List<ScanResult> results=new ArrayList<>();

    String ITEM_KEY = "key";
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    ArrayList<String> bssidList=new ArrayList<>();
    SimpleAdapter adapter;

    Toolbar toolbar;
    TextView txt_title;

    boolean WIFI = false;
    boolean MOBILE = false;
    ConnectivityManager CM;
    NetworkInfo[] networkInfo;
    String IPaddress="";

    String broadCastAddress="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_manager);

        CM= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkInfo= CM.getAllNetworkInfo();
        
        getCurrentIPAddress();

        fetchIDs();
        setUpToolbar();
    }

    private void getCurrentIPAddress() {
        for (NetworkInfo netInfo : networkInfo) {

            if (netInfo.getTypeName().equalsIgnoreCase("WIFI"))
                if (netInfo.isConnected())
                    WIFI = true;

            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE"))
                if (netInfo.isConnected())
                    MOBILE = true;
        }

        if(WIFI)
        {
            IPaddress = C.GetDeviceipWiFiData(getApplicationContext());
        }

        if(MOBILE)
        {
            IPaddress = C.GetDeviceipMobileData();
        }

        Log.e("Current IP", "-->" + IPaddress);

        if(!WIFI){
            C.Toast(getApplicationContext(),"Please Connect in Same Network as Your Master Device");
        }
        else {
            try {
                broadCastAddress=C.getBroadcastAddress(getApplicationContext()).getHostAddress();
                sendUDPCommandADD();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }

    private void setUpToolbar() {
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        txt_title=(TextView) findViewById(R.id.txt_title);
        txt_title.setText("Select Wi-Fi");
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void fetchIDs() {
        /*sendUDPCommandADD();
        final Intent intent=new Intent(this, ReceiveService.class);
        startService(intent);*/
        txt_status = (TextView) findViewById(R.id.txt_status);
        txt_scan = (TextView) findViewById(R.id.txt_scan);
        list_wifi = (ListView)findViewById(R.id.list_wifi);

        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled())
        {
            Toast.makeText(getApplicationContext(), "Wifi is disabled...making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }

        this.adapter = new SimpleAdapter(getApplicationContext(), arraylist, R.layout.wifi_listrow, new String[] { ITEM_KEY }, new int[] { R.id.list_item });
        list_wifi.setAdapter(this.adapter);

        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {
                results = wifi.getScanResults();
                size = results.size();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        doInback();

        list_wifi.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try{
                    if(wifi.isWifiEnabled()) {
                        WifiConfiguration configuration = new WifiConfiguration();
                        configuration.SSID = "\"" + arraylist.get(position).get(ITEM_KEY) + "\"";
                        wifi.disconnect();
                        wifi.enableNetwork(configuration.networkId, true);
                        wifi.reconnect();
                    }
                }catch (Exception e){
                    Log.e("Exception",e.getMessage());
                }

            }
        });

        txt_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doInback();
            }
        });
    }

    private void sendUDPCommandADD() {
        //new SendUDP(AppConstant.CMD_GET_MASTER).execute();
    }

    public void doInback() {
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub

                arraylist.clear();
                bssidList.clear();
                wifi.startScan();
                //Toast.makeText(getApplicationContext(), "Scanning...." + size, Toast.LENGTH_SHORT).show();
                try {
                    size = size - 1;
                    while (size >= 0) {
                        HashMap<String, String> item = new HashMap<String, String>();
                        item.put(ITEM_KEY, results.get(size).SSID);
                        //Log.e("BSSID",results.get(size).SSID+" -- "+results.get(size).BSSID);
                        arraylist.add(item);
                        bssidList.add(results.get(size).BSSID);
                        size--;
                        adapter.notifyDataSetChanged();
                    }
                } catch (Exception e) {
                    Log.e("Exception", e.getMessage());
                }
                doInback();
            }
        }, 3500);

    }

    public class SendUDP extends AsyncTask {
        String message;

        public SendUDP(String message){
            this.message=message;
        }

        @Override
        protected Object doInBackground(Object[] params) {

            try {
                DatagramSocket socket=new DatagramSocket(13001);
                byte[] senddata=new byte[message.length()];
                senddata=message.getBytes();
                InetSocketAddress server_addr=new InetSocketAddress(broadCastAddress,13001);
                Log.e("server_add",server_addr.toString());
                DatagramPacket packet=new DatagramPacket(senddata,senddata.length,server_addr);
                socket.setBroadcast(true);
                socket.send(packet);
                Log.e("packet","sent");

                DatagramSocket client_socket=new DatagramSocket(13000);
                byte[] recieve_data=new byte[2048];

                DatagramPacket recvpacket=new DatagramPacket(recieve_data,recieve_data.length);
                client_socket.setSoTimeout(60000);
                client_socket.receive(recvpacket);
                Log.e("Packet :","Recieved");

                Log.e("Recived IP :",recvpacket.getAddress().toString());

                String text=new String(recieve_data,0,recvpacket.getLength());
                Log.e("Received Data :",text);

                int port=recvpacket.getPort();

                Log.e("Received port :",String.valueOf(port));
                Log.e("Received Pac Data",recvpacket.getData().toString());
            }
            catch (SocketException s){
                Log.e("Exception",s.getMessage());
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.e("Exception",e.getMessage());
            }
            return null;
        }
    }
}
