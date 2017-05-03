package com.nivida.smartnode;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.adapter.CustomAdapter;
import com.nivida.smartnode.adapter.SlaveGridAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.AddMasterService;
import com.nivida.smartnode.services.ReceiveService;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.util.Strings;
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

public class AddSlaveActivity extends AppCompatActivity {


    GridView slaveList;

    SlaveGridAdapter slaveGridAdapter;
    private AppPreference preference;
    List<Bean_SlaveGroup> slaveGroupList=new ArrayList<>();

    LinearLayout layout_addedslave,layout_no_addedslave;

    DatabaseHandler databaseHandler;

    Toolbar toolbar;
    TextView txt_smartnode,txt_1,txt_2,txt_slave;
    Button btn_addslave;
    ImageView img_add,img_home;
    Typeface tf;

    String m_name="";

    //Define Context menu String items here
    String[] contextMenuItems={"Rename Device","Remove Device"};
    String slave_hex_id_for_context="";

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue=false;
    NetworkUtility netcheck;

    ArrayAdapter<String> spn_adp;
    ArrayList<String> master_names=new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_slave);
        tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        preference=new AppPreference(getApplicationContext());
        netcheck=new NetworkUtility(getApplicationContext());
        databaseHandler=new DatabaseHandler(getApplicationContext());

        if(!NetworkUtility.isOnline(getApplicationContext())){
            sendUDPCommandADD();
            startAddDeviceUDPService();
        }
        else {
            sendUDPCommandADD();
            startAddDeviceUDPService();
            //startAddDeviceService();
        }


        clientId=MqttClient.generateClientId();
        spn_adp=new ArrayAdapter<String>(getApplicationContext(),R.layout.custom_spinner_size,master_names);

        Intent i=getIntent();
        m_name=i.getStringExtra("m_name");
        //Toast.makeText(AddSlaveActivity.this, ""+m_name, Toast.LENGTH_SHORT).show();

        slaveGroupList=databaseHandler.getAllSlaveGroupData(preference.getMasterIDForDevice());
        layout_addedslave=(LinearLayout) findViewById(R.id.layout_addedslave);
        layout_no_addedslave=(LinearLayout) findViewById(R.id.layout_no_addedslave);
        slaveGridAdapter=new SlaveGridAdapter(this,slaveGroupList);


        checkIfSlaveAvailable();
        fetchid();
        txt_smartnode.setTypeface(tf);
        txt_1.setTypeface(tf);
        txt_2.setTypeface(tf);
        btn_addslave.setTypeface(tf);

        receiveMessage();

    }

    private void sendUDPCommandADD() {
        new SendUDP(AppConstant.CMD_ADD_DEVICE).execute();
    }

    private void startAddDeviceUDPService() {
        /*if(!NetworkUtility.isOnline(getApplicationContext())){*/
            final Intent intent=new Intent(this, ReceiveService.class);
            startService(intent);
        /*}*/
    }

    private void receiveMessage() {
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle=intent.getExtras();
                ArrayList<String> device_id=new ArrayList<>();
                ArrayList<String> device_name=new ArrayList<>();
                if(bundle!=null){

                    subscribedMessage=bundle.getString(AddDeviceService.MESSAGETOSEND);

                    if(subscribedMessage==null || subscribedMessage.equals("")){
                        //Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                        device_name.add("No Device Found");
                    }
                    else {
                        Log.e("JSOn fr dev ", subscribedMessage);
                        try{
                            JSONObject jsonDevice=new JSONObject(subscribedMessage);
                            String cmd=jsonDevice.getString("cmd");
                            if(cmd.equals("LOS")){
                                device_id.clear();
                                device_name.clear();
                                JSONArray slaveArray=jsonDevice.getJSONArray("sl");
                                Log.e("JArray sl ",slaveArray.toString());
                                for (int i=0;i<slaveArray.length();i++){
                                    device_id.add(slaveArray.getString(i));
                                    device_name.add(slaveArray.get(i).toString());
                                    Log.e(" Slave ID :",device_name.get(i));
                                }
                            }

                        } catch (JSONException e) {
                            Log.e("JSON Message : ",e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    master_names.clear();
                        for (int i=0;i<device_id.size();i++){
                            master_names.add(device_name.get(i));
                        }


                    spn_adp.notifyDataSetChanged();
                    //Toast.makeText(AddMasterActivity.this, subscribedMessage, Toast.LENGTH_SHORT).show();
                }
            }
        };
    }

    private void startAddDeviceService() {
        if(!serviceIsRunning()){
            final Intent intent=new Intent(this, AddDeviceService.class);
            startService(intent);
        }
    }

    private boolean serviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAddDeviceService();
        registerReceiver(receiver,new IntentFilter(AddDeviceService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        final Intent intent=new Intent(this, AddDeviceService.class);
        stopService(intent);
        unregisterReceiver(receiver);
    }

    private void checkIfSlaveAvailable() {
        if(databaseHandler.getSlaveDataCounts()>1){
            layout_no_addedslave.setVisibility(View.GONE);
            layout_addedslave.setVisibility(View.VISIBLE);
        }
        else {
            layout_no_addedslave.setVisibility(View.VISIBLE);
            layout_addedslave.setVisibility(View.GONE);
        }
    }

    private void fetchid() {

        txt_smartnode=(TextView)findViewById(R.id.txt_smartnode);
        txt_smartnode.setText(preference.getMasterNameForDevice());
        txt_1=(TextView)findViewById(R.id.txt_1);
        txt_2=(TextView)findViewById(R.id.txt_2);
        btn_addslave=(Button)findViewById(R.id.btn_addslave);
        img_add=(ImageView)findViewById(R.id.img_add);
        img_home=(ImageView)findViewById(R.id.img_home);

        img_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        img_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchDeviceFromServer();
            }
        });

        btn_addslave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchDeviceFromServer();
            }
        });

        slaveList=(GridView) findViewById(R.id.slavelist);
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        slaveList.setAdapter(slaveGridAdapter);

        slaveList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                //int slave_id=databaseHandler.getSlaveIdAtCurrentPosition(position);
                //String slave_hex_id=databaseHandler.getSlaveHexIdAtCurrentPosition(position);

                int slave_id=slaveGridAdapter.getSlave_id(position);
                String slave_hex_id=slaveGridAdapter.getSlaveHexID(position);

                Log.e("slave_hex_id",slave_hex_id+"");

                if(slave_id==200){
                    fetchDeviceFromServer();
                }
                else {
                        Intent i=new Intent(getApplicationContext(),AddSwitchActivity.class);
                        i.putExtra("slave_id",slave_id);
                        i.putExtra("slave_hex_id",slave_hex_id);
                        startActivity(i);
                        finish();
                }
            }
        });

        slaveList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int slave_id=databaseHandler.getSlaveIdAtCurrentPosition(position);
                final String slave_hex_id=databaseHandler.getSlaveHexIdAtCurrentPosition(position);
                final String slave_name=databaseHandler.getDeviceName(slave_id);

                if(slave_id==200){
                    return false;
                }
                else {
                    PopupMenu popupMenu=new PopupMenu(AddSlaveActivity.this,view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_edit,popupMenu.getMenu());
                    popupMenu.setGravity(Gravity.CENTER);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            switch (item.getItemId()){
                                case R.id.rename:
                                    showRenameDeviceDialog(slave_id,slave_hex_id,slave_name);
                                    break;
                                case R.id.remove:
                                    showRemoveDeviceDialog(slave_id,slave_hex_id);
                                    break;
                            }

                            return true;
                        }
                    });

                    popupMenu.show();
                    return true;
                }
            }
        });
    }

    private void showRemoveDeviceDialog(final int slave_id, final String slave_hex_id) {
        AlertDialog.Builder confirmDelete=new AlertDialog.Builder(AddSlaveActivity.this);
        confirmDelete.setTitle("Confirm to Remove");
        confirmDelete.setMessage("Are you sure to remove this device ?");
        confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                databaseHandler.deleteSlaveDevice(slave_id,slave_hex_id);
                databaseHandler.deleteAllSwitchesFromSlave(slave_hex_id);
                Toast.makeText(AddSlaveActivity.this, "Device removed successfully", Toast.LENGTH_SHORT).show();
                slaveGridAdapter.notifyDataSetChanged();

                if(databaseHandler.getSlaveDataCounts()<2){
                    layout_no_addedslave.setVisibility(View.VISIBLE);
                    layout_addedslave.setVisibility(View.GONE);
                }
                dialog.dismiss();
            }
        });
        confirmDelete.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog=confirmDelete.create();
        dialog.show();
    }

    private void showRenameDeviceDialog(final int slave_id, final String slave_hex_id, String slave_name) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_renameslave, null);
        dialogBuilder.setView(dialogView);

        final EditText edt_device_name=(EditText) dialogView.findViewById(R.id.edt_device_name);
        edt_device_name.setText(slave_name);

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
                        if(edt_device_name.getText().toString().trim().equalsIgnoreCase("")){
                            Toast.makeText(AddSlaveActivity.this, "Please enter device name", Toast.LENGTH_SHORT).show();
                        }
                        else if(databaseHandler.isSameSlaveName(edt_device_name.getText().toString().trim())){
                            Toast.makeText(AddSlaveActivity.this, "Device name already exists\nplease enter different Device name", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Bean_SlaveGroup beanSlaveGroup=new Bean_SlaveGroup();
                            beanSlaveGroup.setId(slave_id);
                            beanSlaveGroup.setHex_id(slave_hex_id);
                            beanSlaveGroup.setName(edt_device_name.getText().toString().trim());

                            databaseHandler.renameSlave(beanSlaveGroup);

                            slaveGridAdapter.notifyDataSetChanged();
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    private void fetchDeviceFromServer() {
        if(netcheck.isOnline()){
            try{
                mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientId,new MemoryPersistence());
                MqttConnectOptions connectOptions=new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);
                MqttMessage mqttMessage=new MqttMessage(AppConstant.CMD_LIST_OF_SLAVES.getBytes());
                mqttMessage.setRetained(true);
                mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                mqttClient.disconnect();

            } catch (MqttException e) {
                Log.e("Exception : ",e.getMessage());
                e.printStackTrace();
            }
            showAddNewSlaveDialog();
        }
        else {
            Toast.makeText(getApplicationContext(), "No internet connection found,\nplease check your connection first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddNewSlaveDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_addnewslave, null);
        dialogBuilder.setView(dialogView);

        final Spinner spn_device=(Spinner) dialogView.findViewById(R.id.spn_device);
        spn_device.setAdapter(spn_adp);

        dialogBuilder.setTitle("Add New Device");
        dialogBuilder.setPositiveButton("Add Device", null);
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
                        if(spn_device.getCount()==0){
                            Toast.makeText(AddSlaveActivity.this, "No device found on Server.", Toast.LENGTH_SHORT).show();
                        }
                        else if(spn_device.getSelectedItem().toString().trim().equals("")){
                            Toast.makeText(AddSlaveActivity.this, "Please select Device name", Toast.LENGTH_SHORT).show();
                        }
                        else if(databaseHandler.isSameSlaveId(spn_device.getSelectedItem().toString().trim())){
                            Toast.makeText(getApplicationContext(), "Device that you selected is already added\nplease select different Device", Toast.LENGTH_SHORT).show();
                        }
                        else if(databaseHandler.isSameSlaveName(spn_device.getSelectedItem().toString().trim())){
                            Toast.makeText(getApplicationContext(), "Device name you selected is already exist\nplease select different Device", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Bean_SlaveGroup beanSlaveGroup=new Bean_SlaveGroup();
                            if(databaseHandler.getSlaveDataCounts()==1){
                                beanSlaveGroup.setId(1);
                            }
                            else {
                                beanSlaveGroup.setId(databaseHandler.getSlaveLastId()+1);
                            }
                            beanSlaveGroup.setName(spn_device.getSelectedItem().toString());
                            beanSlaveGroup.setHasSwitches("0");
                            beanSlaveGroup.setHasDimmers("0");
                            beanSlaveGroup.setHex_id(spn_device.getSelectedItem().toString());
                            beanSlaveGroup.setMaster_id(preference.getMasterIDForDevice());

                            databaseHandler.addSlaveItem(beanSlaveGroup);

                            slaveGridAdapter.notifyDataSetChanged();

                            if(databaseHandler.getSlaveDataCounts()>0){
                                layout_no_addedslave.setVisibility(View.GONE);
                                layout_addedslave.setVisibility(View.VISIBLE);
                            }
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    @Override
    public void onBackPressed() {
            Intent intent=new Intent(getApplicationContext(),AddMasterActivity.class);
            startActivity(intent);
            finish();
    }

    public class SendUDP extends AsyncTask {
        String message;

        public SendUDP(String message){
            this.message=message;
        }

        @Override
        protected Object doInBackground(Object[] params) {


                try {
                    DatagramSocket socket = new DatagramSocket(13001);
                    byte[] senddata = new byte[message.length()];
                    senddata = message.getBytes();
                    InetSocketAddress server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    DatagramPacket packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("PAcket", "sent on " + C.getBroadcastAddress(getApplicationContext()).getHostAddress());

                    DatagramSocket client_socket = new DatagramSocket(13000);
                    Log.e("client_socket", client_socket.toString());
                    byte[] recieve_data = new byte[2048];
                    Log.e("client_socket", "");


                    DatagramPacket recvpacket = new DatagramPacket(recieve_data, recieve_data.length);
                    Log.e("client_socket", recvpacket.toString());
                    client_socket.setSoTimeout(60000);
                    Log.e("Timeout", "60000 set");
                    client_socket.receive(recvpacket);
                    Log.e("Packet :", "Recieved");

                    Log.e("Recived IP :", recvpacket.getAddress().toString());

                    String text = new String(recieve_data, 0, recvpacket.getLength());
                    Log.e("Received Data :", text);

                    int port = recvpacket.getPort();

                    Log.e("Received port :", String.valueOf(port));
                    Log.e("Received Pac Data", recvpacket.getData().toString());


                } catch (SocketException e) {
                    e.printStackTrace();
                    Log.e("Exception", "-->" + e.getLocalizedMessage());
                } catch (IOException e) {
                    Log.e("Exception", "->" + e.getLocalizedMessage());
                }
            return null;
        }
    }

}
