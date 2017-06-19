package com.nivida.smartnode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.a.Status;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.GroupSwitchService;
import com.nivida.smartnode.services.UDPService;
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

import static android.view.View.GONE;

public class MyAccountActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView txt_title;
    Button btn_changeUsername, btn_changePIN;

    LinearLayout layout_chnageUsername, layout_chnageUserPIN;
    Button btn_saveUserPIN, btn_saveUsername;
    EditText edt_username, edt_userPIN, edt_olduserPIN, edt_newuserPIN;

    AppPreference preference;

    String masterID = "";
    String userType = "";

    MqttClient mqttClient;
    String clientId = "";
    String subscribedMessage = "";
    BroadcastReceiver receiver;

    DatabaseHandler db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_account);

        clientId = MqttClient.generateClientId();

        Intent intent = getIntent();
        masterID = intent.getStringExtra("masterID");
        userType = intent.getStringExtra("userType");

        db=new DatabaseHandler(this);
        preference = new AppPreference(getApplicationContext());

        setUpToolbar();
        startReceiver();
        fetchIDs();


    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txt_title = (TextView) findViewById(R.id.txt_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        txt_title.setText("My Account");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void fetchIDs() {
        btn_changeUsername = (Button) findViewById(R.id.btn_changeUsername);
        btn_changePIN = (Button) findViewById(R.id.btn_changePIN);
        btn_saveUserPIN = (Button) findViewById(R.id.btn_saveUserPIN);
        btn_saveUsername = (Button) findViewById(R.id.btn_saveUsername);

        edt_username = (EditText) findViewById(R.id.edt_username);
        edt_userPIN = (EditText) findViewById(R.id.edt_userPIN);
        edt_olduserPIN = (EditText) findViewById(R.id.edt_olduserPIN);
        edt_newuserPIN = (EditText) findViewById(R.id.edt_newuserPIN);

        layout_chnageUsername = (LinearLayout) findViewById(R.id.layout_chnageUsername);
        layout_chnageUserPIN = (LinearLayout) findViewById(R.id.layout_chnageUserPIN);

        btn_changeUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                C.Toast(getApplicationContext(), "This Feature will Available Soon");

                /*if(layout_chnageUsername.getVisibility()==View.VISIBLE){
                    layout_chnageUsername.setVisibility(GONE);
                    btn_changePIN.setVisibility(View.VISIBLE);
                }
                else {
                    layout_chnageUsername.setVisibility(View.VISIBLE);
                    btn_changePIN.setVisibility(GONE);
                }*/
            }
        });

        btn_changePIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layout_chnageUserPIN.getVisibility() == View.VISIBLE) {
                    layout_chnageUserPIN.setVisibility(GONE);
                    btn_changeUsername.setVisibility(View.VISIBLE);
                } else {
                    layout_chnageUserPIN.setVisibility(View.VISIBLE);
                    btn_changeUsername.setVisibility(GONE);
                }
            }
        });

        btn_saveUsername.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = edt_username.getText().toString().trim();
                String userPIN = edt_userPIN.getText().toString().trim();

                if (username.isEmpty()) {
                    C.Toast(getApplicationContext(), "Please Enter Username");
                } else if (userPIN.isEmpty() || userPIN.length() < 4) {
                    C.Toast(getApplicationContext(), "Please Enter your 4 Digit PIN");
                } else {
                    JSONObject object = new JSONObject();

                    try {
                        if (userType.equalsIgnoreCase(Cmd.LIN)) {
                            object.put("cmd", Cmd.USR);
                        } else {
                            object.put("cmd", Cmd.UNM);
                        }

                        object.put("slave", masterID);
                        object.put("rename_user", username);
                        object.put("pin", userPIN);
                        object.put("token", db.getSlaveToken(masterID));
                    } catch (Exception e) {
                        Log.e("Exception", e.getMessage());
                    }
                }
            }
        });

        btn_saveUserPIN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldUserPIN = edt_olduserPIN.getText().toString().trim();
                String newUserPIN = edt_newuserPIN.getText().toString().trim();

                if (oldUserPIN.isEmpty() || oldUserPIN.length() < 4) {
                    C.Toast(getApplicationContext(), "Please Enter 4 Digit Old PIN ");
                } else if (newUserPIN.isEmpty() || newUserPIN.length() < 4) {
                    C.Toast(getApplicationContext(), "Please Enter 4 Digit New PIN");
                } else {
                    JSONObject object = new JSONObject();

                    try {
                        if (userType.equalsIgnoreCase(Cmd.LIN))
                            object.put("cmd", Cmd.PIN);
                        else
                            object.put("cmd", Cmd.UPI);

                        object.put("slave", masterID);
                        object.put("old", oldUserPIN);
                        object.put("new", newUserPIN);
                        object.put("token", db.getSlaveToken(masterID));

                        sendChangePINCommand(object.toString());

                    } catch (Exception e) {
                        Log.e("Exception", e.getMessage());
                    }
                }
            }
        });
    }

    private void startReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();

                if (bundle != null) {

                    subscribedMessage = bundle.getString(AddDeviceService.MESSAGETOSEND);
                    String UDPMessage = bundle.getString(UDPService.MESSAGEJSON);

                    Log.e("JSOn fr group ", "" + subscribedMessage);
                    if (UDPMessage != null) {
                        handleCommands(UDPMessage);
                    } else if (subscribedMessage == null) {
                        Log.e("JSON Message", "Null");
                    } else if (subscribedMessage.equals("")) {
                        Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                    } else if (subscribedMessage.contains("master")) {
                        Log.e("From Grp actvty ", "Device Started...");
                    } else {
                        handleCommands(subscribedMessage);
                    }
                }
            }
        };
    }

    private void handleCommands(String message) {
        try {
            JSONObject object = new JSONObject(message);
            String cmd = object.getString("cmd");

            if (cmd.equals(Cmd.INTERNET)) {
                C.Toast(getApplicationContext(), object.getString("message"));
                return;
            }

            if (cmd.equalsIgnoreCase(Cmd.PIN)) {
                if (object.has("status") && object.getString("status").equalsIgnoreCase(Status.SUCCESS)) {
                    C.Toast(getApplicationContext(), "Your PIN Changed Successfully.");
                } else {
                    C.Toast(getApplicationContext(), "Sorry, Some Error Occured\nPlease Try Again Later!");
                }
            } else if (cmd.equalsIgnoreCase(Cmd.UPI)) {
                if (object.has("status") && object.getString("status").equalsIgnoreCase(Status.SUCCESS)) {
                    C.Toast(getApplicationContext(), "Your PIN Changed Successfully.");
                } else {
                    C.Toast(getApplicationContext(), "Either you enetred incorrect Old PIN or might be device is offline.\nPlease Try Again Later!");
                }
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void sendChangePINCommand(String command) {
        if (NetworkUtility.isOnline(getApplicationContext())) {

            DatabaseHandler db = new DatabaseHandler(this);
            edt_newuserPIN.setText("");
            edt_olduserPIN.setText("");
            if (SmartNode.slavesInLocal.contains(masterID)) {
                new SendUDP(command).execute();
            } else {
                new SendMQTT(db.getSlaveTopic(masterID) + AppConstant.MQTT_PUBLISH_TOPIC, command).execute();
            }

            C.Toast(getApplicationContext(), "Please Wait while Updating your PIN...");
        } else {
            Toast.makeText(getApplicationContext(), "No internet connection found,\nplease check your connection first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(GroupSwitchService.NOTIFICATION));
        Log.e("Reciever :", "Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.e("Reciever :", "UnRegistered");
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), SelectDeviceForChangeAccountActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private class SendMQTT extends AsyncTask<Void, Void, Void> {

        String topic = "";
        String command = "";

        public SendMQTT(String topic, String command) {
            this.topic = topic;
            this.command = command;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);

                Log.e("Command Fired UPD :", command);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setRetained(true);
                mqttClient.publish(topic, mqttMessage);
                //Log.e("topic msg",preference.getTopic()+AppConstant.MQTT_PUBLISH_TOPIC+" "+mqttMessage);


            } catch (MqttException e) {
                Log.e("Exception : ", e.getMessage());
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

        public SendUDP(String message) {
            this.message = message;
            showMaster = true;
        }

        @Override
        protected String doInBackground(Void[] params) {

            try {
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                /*if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {*/
                server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);
                Log.e("Packet", "Sent");
                /*} else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("Packet","Sent");
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
    }
}
