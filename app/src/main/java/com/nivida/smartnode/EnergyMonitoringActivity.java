package com.nivida.smartnode;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.adapter.EnergySlaveListAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_EnergySlave;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.GroupSwitchService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.NetworkUtility;
import com.nivida.smartnode.views.RoundView;

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
import java.util.ArrayList;
import java.util.List;

import static com.nivida.smartnode.GroupSwitchOnOffActivity.UDPSERVICE_CLASSNAME;

public class EnergyMonitoringActivity extends AppCompatActivity implements EnergySlaveListAdapter.OnViewSelection {

    Toolbar toolbar;
    TextView txt_title;

    GridView slaveList;
    LinearLayout layout_noDevice;

    DatabaseHandler db;
    AppPreference preference;
    List<Bean_EnergySlave> energySlaveList = new ArrayList<>();
    EnergySlaveListAdapter energySlaveListAdapter;

    MqttClient mqttClient;
    String clientId = "";
    BroadcastReceiver receiver;
    String subscribedMessage = "";

    List<Bean_SlaveGroup> slaveGroups = new ArrayList<>();

    DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_monitoring);

        clientId = MqttClient.generateClientId();
        db = new DatabaseHandler(getApplicationContext());
        preference = new AppPreference(getApplicationContext());
        databaseHandler=new DatabaseHandler(getApplicationContext());

        setUpToolbar();
        startServices();
        startReceiver();
        fetchIDs();
        setItems();
    }

    private void startServices() {
        if(!UDPServiceIsRunning()){
            Intent intent = new Intent(this, UDPService.class);
            startService(intent);
        }
    }

    private boolean UDPServiceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (UDPSERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void fetchIDs() {
        slaveList = (GridView) findViewById(R.id.slavelist);
        layout_noDevice = (LinearLayout) findViewById(R.id.layout_noDevice);
        energySlaveListAdapter = new EnergySlaveListAdapter(getApplicationContext(), energySlaveList, this);
        slaveList.setAdapter(energySlaveListAdapter);
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
            if (cmd.equals(Cmd.ENR)) {
                String slaveID = object.getString("slave");

                if (!energySlaveListAdapter.hasAdded(slaveID)) {

                    JSONArray wattsArray = object.getJSONArray("daily");
                    float[] usedWatts = new float[wattsArray.length()];

                    for (int i = 0; i < usedWatts.length; i++) {
                        usedWatts[i] = (float) wattsArray.getInt(i);
                    }

                    String watt = object.getString("watt");

                    Bean_EnergySlave energySlave = new Bean_EnergySlave();
                    energySlave.setDay(Bean_EnergySlave.TODAY);
                    energySlave.setSlaveID(slaveID);
                    energySlave.setSlaveName(getSlaveName(slaveID));
                    energySlave.setUsedWatts(usedWatts);
                    energySlave.setCurrentWatt(watt);
                    energySlave.setPricePerRate(preference.getPricePerUnit());
                    energySlave.setMasterName(databaseHandler.getMasterNameBySlaveHexID(slaveID));

                    energySlaveList.add(energySlave);
                }
            }
        } catch (JSONException j) {
            Log.e("Exception", j.getMessage());
        }
        energySlaveListAdapter.notifyDataSetChanged();
    }

    private String getSlaveName(String slaveID) {
        String slaveName = "";
        for (int i = 0; i < slaveGroups.size(); i++) {
            if (slaveGroups.get(i).getHex_id().equalsIgnoreCase(slaveID)) {
                slaveName = slaveGroups.get(i).getName();
                break;
            }
        }

        return slaveName;
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(AddDeviceService.NOTIFICATION));
        Log.e("Reciever :", "Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.e("Reciever :", "UnRegistered");
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txt_title = (TextView) findViewById(R.id.txt_title);

        setSupportActionBar(toolbar);
        txt_title.setText("ENERGY MONITORING");

        txt_title.setTypeface(C.raleway(getApplicationContext()));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void setItems() {
        slaveGroups.clear();
        slaveGroups = db.getAllSlaveData();

        for(int i=0; i<slaveGroups.size(); i++){
            if(slaveGroups.get(i).getId()==200){
                slaveGroups.remove(i);
            }
        }

        if (slaveGroups.size() > 0) {
            if (NetworkUtility.isOnline(getApplicationContext())) {
                for (int i = 0; i < slaveGroups.size(); i++) {
                    String command = getUDPCommand(slaveGroups, i);
                    Log.e("command", command);
                    if (preference.isOnline() || (!preference.isOnline() && !preference.getCurrentIPAddr().equalsIgnoreCase(databaseHandler.getMasterIPBySlaveID(slaveGroups.get(i).getHex_id())))) {
                        new SendMQTTCommand(command,slaveGroups.get(i).getHex_id()).execute();
                    } else {
                        new SendUDP(command).execute();
                    }
                }
            } else {
                C.Toast(getApplicationContext(), "No Network Connection Found\nPlease Connect to Network First");
            }
        } else {
            slaveList.setVisibility(View.GONE);
            layout_noDevice.setVisibility(View.VISIBLE);
        }
    }

    private String getUDPCommand(List<Bean_SlaveGroup> slaveGroups, int position) {
        JSONObject object = new JSONObject();
        try {
            object.put("cmd", Cmd.ENR);

            for (int i = 0; i < slaveGroups.size(); i++) {
                if (i == position) {
                    object.put("slave", slaveGroups.get(i).getHex_id());
                    Log.e("slave Hex","->"+slaveGroups.get(i).getHex_id() + "--"+ slaveGroups.get(i).getId());
                    object.put("token", databaseHandler.getSlaveToken(slaveGroups.get(i).getHex_id()));
                    break;
                }
            }

            return object.toString();

        } catch (JSONException j) {
            Log.e("Exception", j.getMessage());
        }

        return null;
    }

    @Override
    public void onOptionSelected(int position, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_edit_energyswitch, popupMenu.getMenu());

        popupMenu.setGravity(Gravity.CENTER);

        final String slaveID = energySlaveListAdapter.getSlaveID(position);
        final String slaveName = energySlaveListAdapter.getSlaveName(position);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.changeRate:
                        changePriceAsPerUser();
                        break;
                    case R.id.rename:
                        renameSlaveName(slaveID, slaveName);
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    private void changePriceAsPerUser() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_renameslave, null);
        dialogBuilder.setView(dialogView);

        final TextView txt_switch = (TextView) dialogView.findViewById(R.id.txt_slave_name);
        final EditText edt_device_name = (EditText) dialogView.findViewById(R.id.edt_device_name);
        edt_device_name.setText(String.valueOf(preference.getPricePerUnit()));
        edt_device_name.setInputType(InputType.TYPE_CLASS_NUMBER);
        edt_device_name.setKeyListener(DigitsKeyListener.getInstance("0123456789."));

        edt_device_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int length=edt_device_name.getText().toString().length();
                String price=edt_device_name.getText().toString();
                if(!price.isEmpty()){
                    if(price.charAt(price.length()-1)=='.'){
                        int dotCount=0;

                        for(int i=0; i<price.length(); i++){
                            if(price.charAt(i)=='.')
                                dotCount++;
                        }

                        if(dotCount>1){
                            dotCount=0;
                            String clearString="";

                            for(int i=0; i<price.length(); i++){
                                if(price.charAt(i)=='.'){
                                    dotCount++;
                                    if(!(dotCount>1)){
                                        clearString+=String.valueOf(price.charAt(i));
                                    }
                                    else
                                        break;
                                }
                                else {
                                    clearString+=String.valueOf(price.charAt(i));
                                }
                            }

                            edt_device_name.setText(clearString);
                            edt_device_name.setSelection(clearString.length());
                        }
                    }
                    else if(length>5){
                        String firstFiveChars=price.substring(0,5);
                        edt_device_name.setText(firstFiveChars);
                        edt_device_name.setSelection(5);
                    }
                }
                else if(length>5){
                    String firstFiveChars=price.substring(0,5);
                    edt_device_name.setText(firstFiveChars);
                    edt_device_name.setSelection(5);
                }
            }
        });
        txt_switch.setText("Price Per Unit");

        dialogBuilder.setTitle("Change Price");
        dialogBuilder.setPositiveButton("Save", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();

        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (edt_device_name.getText().toString().trim().isEmpty()) {
                            C.Toast(getApplicationContext(), "Please Enter Price");
                        } else {
                            preference.setPricePerUnit(Float.parseFloat(edt_device_name.getText().toString().trim()));
                            C.Toast(getApplicationContext(), "Price Saved Successfully");
                            energySlaveListAdapter.notifyDataSetChanged();
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    private void renameSlaveName(final String slaveID, String slaveName) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_renameslave, null);
        dialogBuilder.setView(dialogView);

        final TextView txt_switch = (TextView) dialogView.findViewById(R.id.txt_slave_name);
        final EditText edt_device_name = (EditText) dialogView.findViewById(R.id.edt_device_name);
        edt_device_name.setText(slaveName);
        txt_switch.setText("Device name");

        dialogBuilder.setTitle("Rename Device");
        dialogBuilder.setPositiveButton("Ok", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();

        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (edt_device_name.getText().toString().trim().equalsIgnoreCase("")) {
                            C.Toast(getApplicationContext(), "Please Enter Device Name");
                        } else if (db.isSameSlaveName(edt_device_name.getText().toString().trim())) {
                            C.Toast(getApplicationContext(), "Device Name Already Exists\nPlease Enter Different Device Name");
                        } else {
                            db.renameSlave(slaveID, edt_device_name.getText().toString());
                            C.Toast(getApplicationContext(), "Device Renamed Successfully");

                            energySlaveListAdapter.notifyDataSetChanged();
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    public class SendMQTTCommand extends AsyncTask<Void, Void, String> {

        String command = "";
        String slaveID="";

        public SendMQTTCommand(String command,String slaveID) {
            this.command = command;
            this.slaveID=slaveID;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {
            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);
                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(1);
                mqttMessage.setRetained(true);
                mqttClient.publish(databaseHandler.getSlaveTopic(slaveID)+AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                Log.e("Mqtt Message", "Published for STS 1st");
                mqttClient.disconnect();

            } catch (MqttException e) {
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

                Log.e("IP Address Saved", "->" + preference.getIpaddress());

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
                    Log.e("Packet", "Sent");
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
        }
    }
}
