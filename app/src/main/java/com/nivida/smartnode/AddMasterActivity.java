package com.nivida.smartnode;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.a.Status;
import com.nivida.smartnode.adapter.CustomAdapter;
import com.nivida.smartnode.adapter.MasterDeviceAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Master;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
import com.nivida.smartnode.services.AddMasterService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class AddMasterActivity extends AppCompatActivity {

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddMasterService";
    DrawerLayout drawerLayout;
    GridView masterDeviceList, drawerlist;
    ActionBarDrawerToggle drawerToggle;
    CustomAdapter customadapter;
    MasterDeviceAdapter masterDeviceAdapter;
    List<Bean_Master> masterList = new ArrayList<>();
    LinearLayout layout_addedslave, layout_no_addedslave;
    DatabaseHandler databaseHandler;
    Toolbar toolbar;
    TextView txt_smartnode, txt_1, txt_2, txt_slave;
    Button btn_addmaster;
    ImageView img_add, img_home;
    Typeface tf;
    int masteridForRename = 0;
    String masterNameForRename = "";
    ProgressBar progressbar;
    ProgressDialog dialog;
    boolean isDialogShowing=false;
    DatagramSocket client_socket;
    AlertDialog.Builder dialogBuilder;
    AlertDialog b;
    MqttClient mqttClient;
    String clientId = "";
    String subscribedMessage = "";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue = false;
    String selectedMasterName="";
    String masterType="";
    ArrayAdapter<String> spn_adp;
    ArrayList<String> master_names = new ArrayList<>();
    ArrayList<String> master_ips=new ArrayList<>();
    ArrayList<String> master_deviceIDs=new ArrayList<>();
    String ipAddress="";
    ConnectivityManager manager;
    boolean isCalledToShowMST=false;
    String[] perms = { Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.WRITE_SETTINGS,Manifest.permission.WRITE_SECURE_SETTINGS,Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_NETWORK_STATE};
    private AppPreference preference;
    private boolean isDrawerOpen = false;
    private NetworkUtility netcheck;
    private int code=1;

    public static boolean isMarshmallowPlusDevice() {

        return Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isPermissionRequestRequired(Activity activity, @NonNull String[] permissions, int requestCode) {
        if (isMarshmallowPlusDevice() && permissions.length > 0) {
            List<String> newPermissionList = new ArrayList<>();
            for (String permission : permissions) {
                if (PERMISSION_GRANTED != activity.checkSelfPermission(permission)) {
                    newPermissionList.add(permission);

                }
            }
            if (newPermissionList.size() > 0) {
                activity.requestPermissions(newPermissionList.toArray(new String[newPermissionList.size()]), requestCode);
                return true;
            }


        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_master);
        tf = Typeface.createFromAsset(getAssets(), "fonts/raleway.ttf");
        dialog=new ProgressDialog(this);
        dialog.setTitle("Please Wait");
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);


        if(isMarshmallowPlusDevice())
            isPermissionRequestRequired(this, perms, code);

        manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        //startAddMasterService();
        Log.e("AppConst ", AppConstant.CMD_GET_MASTER_TOKEN);


        clientId = MqttClient.generateClientId();
        spn_adp = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_spinner_size, master_names);

        preference = new AppPreference(getApplicationContext());
        databaseHandler = new DatabaseHandler(getApplicationContext());
        netcheck = new NetworkUtility(getApplicationContext());
        customadapter = new CustomAdapter(this);
        masterList = databaseHandler.getAllMasterDeviceData();
        layout_addedslave = (LinearLayout) findViewById(R.id.layout_addedslave);
        layout_no_addedslave = (LinearLayout) findViewById(R.id.layout_no_addedslave);

        masterDeviceAdapter = new MasterDeviceAdapter(this, masterList);

        checkIfSlaveAvailable();
        fetchid();
        txt_smartnode.setTypeface(tf);
        txt_1.setTypeface(tf);
        txt_2.setTypeface(tf);
        btn_addmaster.setTypeface(tf);

        //new ReceiveUDP().execute();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                String master_name = "";
                if(dialog.isShowing())
                    dialog.dismiss();
                if (bundle != null) {

                    subscribedMessage = bundle.getString(AddMasterService.MESSAGETOSEND);


                    String UDPMessage=bundle.getString(UDPService.MESSAGEJSON);
                    ipAddress=bundle.getString(UDPService.DEVICEIP);

                    if(UDPMessage!=null && UDPMessage.equalsIgnoreCase(Status.ERROR)){
                        C.Toast(getApplicationContext(),Status.ERRORMSG);
                    }
                    else if(UDPMessage!=null){
                        try {
                            JSONObject object=new JSONObject(UDPMessage);

                            String command=object.getString("cmd");
                            if(command.equalsIgnoreCase(Cmd.MST)){
                                Log.e("json",object.getString("m_name"));

                                String deviceID=String.valueOf(object.getInt("device_id"));

                                if(!master_deviceIDs.contains(deviceID)){

                                    Log.e("Master Device ID","New One");

                                    master_names.add(object.getString("m_name"));
                                    master_ips.add(ipAddress);
                                    master_deviceIDs.add(deviceID);
                                    spn_adp.notifyDataSetChanged();
                                }
                                if(b==null || !b.isShowing()){
                                    if(isCalledToShowMST)
                                    {
                                        showDialog(object.getString("type"));
                                    }
                                }

                                //master_names.clear();

                            }
                            else if(command.equalsIgnoreCase(Cmd.LIN) && !b.isShowing()){
                                String status=object.getString("status");
                                Log.e("Status",status+"->");
                                if(status.equalsIgnoreCase(Status.SUCCESS) && !selectedMasterName.isEmpty()){
                                    Bean_Master master=new Bean_Master();
                                    master.setId(databaseHandler.getLastIDForMaster()+1);
                                    master.setName(selectedMasterName);
                                    master.setType(object.getString("type"));
                                    master.setTopic(object.getString("topic"));
                                    master.setEnckey(object.getString("encryption_key"));
                                    master.setMasterID(object.getString("slave"));
                                    master.setIpAddress(ipAddress);
                                    master.setUserType(Cmd.LIN);
                                    preference.setToken(object.getString("token"));
                                    preference.setTopic(object.getString("topic"));
                                    preference.setMasterUser(true);

                                    if(!databaseHandler.isMasterAdded(master.getMasterID())){
                                        databaseHandler.addMasterDeviceItem(master);

                                        if(object.getString("type").equalsIgnoreCase(Status.STANDALONE)){
                                            Bean_SlaveGroup beanSlaveGroup=new Bean_SlaveGroup();
                                            if(databaseHandler.getSlaveDataCounts()==1){
                                                beanSlaveGroup.setId(1);
                                            }
                                            else {
                                                beanSlaveGroup.setId(databaseHandler.getSlaveLastId()+1);
                                            }
                                            beanSlaveGroup.setName(selectedMasterName);
                                            beanSlaveGroup.setHasSwitches("0");
                                            beanSlaveGroup.setHasDimmers("0");
                                            beanSlaveGroup.setHex_id(object.getString("slave"));
                                            beanSlaveGroup.setMaster_id(databaseHandler.getLastIDForMaster());
                                            beanSlaveGroup.setSlaveToken(object.getString("token"));
                                            beanSlaveGroup.setSlaveTopic(object.getString("topic"));
                                            beanSlaveGroup.setSlaveUserType(Cmd.LIN);

                                            Log.e("master ID",databaseHandler.getLastIDForMaster()+"->");
                                            databaseHandler.addSlaveItem(beanSlaveGroup);
                                        }
                                        C.Toast(getApplicationContext(),"Device Added Successfully");
                                        selectedMasterName="";
                                    }
                                    masterDeviceAdapter.notifyDataSetChanged();
                                    if(databaseHandler.getMastersCounts()>1){
                                        layout_no_addedslave.setVisibility(View.GONE);
                                        layout_addedslave.setVisibility(View.VISIBLE);
                                    }
                                }
                                else if(status.equalsIgnoreCase(Status.ERROR)){
                                    C.Toast(getApplicationContext(),"Invalid Username or PIN you entered!");
                                }
                            }
                            else if(command.equalsIgnoreCase(Cmd.ULN) && !b.isShowing()){
                                String status=object.getString("status");
                                Log.e("Status",status+"->");
                                if(status.equalsIgnoreCase(Status.SUCCESS) && !selectedMasterName.isEmpty()){
                                    Bean_Master master=new Bean_Master();
                                    master.setId(databaseHandler.getLastIDForMaster()+1);
                                    master.setName(selectedMasterName);
                                    master.setType(object.getString("type"));
                                    master.setTopic(object.getString("topic"));
                                    master.setEnckey(object.getString("encryption_key"));
                                    master.setMasterID(object.getString("slave"));
                                    master.setIpAddress(ipAddress);
                                    master.setUserType(Cmd.ULN);
                                    preference.setToken(object.getString("token"));

                                    Log.e("Topic","->"+object.getString("topic"));
                                    preference.setTopic(object.getString("topic"));
                                    preference.setMasterUser(false);

                                    if(!databaseHandler.isMasterAdded(master.getMasterID())){
                                        databaseHandler.addMasterDeviceItem(master);

                                        if(object.getString("type").equalsIgnoreCase(Status.STANDALONE)){
                                            Bean_SlaveGroup beanSlaveGroup=new Bean_SlaveGroup();
                                            if(databaseHandler.getSlaveDataCounts()==1){
                                                beanSlaveGroup.setId(1);
                                            }
                                            else {
                                                beanSlaveGroup.setId(databaseHandler.getSlaveLastId()+1);
                                            }
                                            beanSlaveGroup.setName(selectedMasterName);
                                            beanSlaveGroup.setHasSwitches("0");
                                            beanSlaveGroup.setHasDimmers("0");
                                            beanSlaveGroup.setHex_id(object.getString("slave"));
                                            beanSlaveGroup.setMaster_id(databaseHandler.getLastIDForMaster());
                                            beanSlaveGroup.setSlaveToken(object.getString("token"));
                                            beanSlaveGroup.setSlaveTopic(object.getString("topic"));
                                            beanSlaveGroup.setSlaveUserType(Cmd.ULN);

                                            Log.e("master ID",databaseHandler.getLastIDForMaster()+"->");
                                            databaseHandler.addSlaveItem(beanSlaveGroup);
                                        }
                                        C.Toast(getApplicationContext(),"Device Added Successfully");
                                        selectedMasterName="";
                                    }
                                    masterDeviceAdapter.notifyDataSetChanged();
                                    if(databaseHandler.getMastersCounts()>1){
                                        layout_no_addedslave.setVisibility(View.GONE);
                                        layout_addedslave.setVisibility(View.VISIBLE);
                                    }
                                }
                                else if(status.equalsIgnoreCase(Status.ERROR)){
                                    C.Toast(getApplicationContext(),"Invalid Username or PIN you entered!");
                                }
                            }
                            else if(command.equalsIgnoreCase(Cmd.MRN)){
                                String status=object.getString("status");

                                if(status.equalsIgnoreCase(Status.SUCCESS)){
                                    if (!masterNameForRename.isEmpty()) {
                                        databaseHandler.renameMaster(masteridForRename, masterNameForRename);
                                        masterDeviceAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                            else{
                                //C.Toast(getApplicationContext(),UDPMessage);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("Exception",e.getMessage());
                        }
                    }
                    else if (subscribedMessage == null || subscribedMessage.equals("")) {
                        //Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                        master_name = "No Master Found";
                    } else {
                        try {
                            JSONObject jsonMaster = new JSONObject(subscribedMessage);
                            String cmd = jsonMaster.getString("cmd");
                            if (cmd.equalsIgnoreCase("MST")) {
                                master_name = jsonMaster.getString("m_name");
                            } else if (cmd.equalsIgnoreCase("MRN")) {
                                String status = jsonMaster.getString("status");
                                if (status.equalsIgnoreCase("success")) {
                                    if (masteridForRename != 0 && !masterNameForRename.equals("")) {
                                        databaseHandler.renameMaster(masteridForRename, masterNameForRename);
                                        masterDeviceAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        } catch (JSONException e) {
                            Log.e("JSON Message : ", e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        };


    }

    private void startAddMasterService() {
        if (!serviceIsRunning()) {
            final Intent intent = new Intent(this, AddMasterService.class);
            //startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAddMasterService();
        registerReceiver(receiver, new IntentFilter(AddMasterService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        final Intent intent = new Intent(this, AddMasterService.class);
        stopService(intent);
        unregisterReceiver(receiver);
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

    private void checkIfSlaveAvailable() {
        if (databaseHandler.getMastersCounts() > 1) {
            layout_no_addedslave.setVisibility(View.GONE);
            layout_addedslave.setVisibility(View.VISIBLE);
        } else {
            layout_no_addedslave.setVisibility(View.VISIBLE);
            layout_addedslave.setVisibility(View.GONE);
        }
    }

    private void fetchid() {
        progressbar=(ProgressBar) findViewById(R.id.progressbar);

        txt_smartnode = (TextView) findViewById(R.id.txt_smartnode);
        txt_1 = (TextView) findViewById(R.id.txt_1);
        txt_2 = (TextView) findViewById(R.id.txt_2);
        btn_addmaster = (Button) findViewById(R.id.btn_addmaster);
        img_add = (ImageView) findViewById(R.id.img_add);
        img_home = (ImageView) findViewById(R.id.img_home);

        img_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
                startActivity(intent);
                finish();
            }
        });

        img_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchMasterDeviceFromServer();
                //showAddNewSlaveDialog();
            }
        });

        btn_addmaster.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchMasterDeviceFromServer();
                //showAddNewSlaveDialog();
            }
        });

        drawerlist = (GridView) findViewById(R.id.drawerlist);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerlayout);
        drawerlist.setAdapter(customadapter);
        masterDeviceList = (GridView) findViewById(R.id.masterdevicelist);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        masterDeviceList.setAdapter(masterDeviceAdapter);

        masterDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                int masterid = masterDeviceAdapter.getMasterId(position);
                if (masterid == 200) {
                    fetchMasterDeviceFromServer();
                    //showAddNewSlaveDialog();
                } else {
                    Log.d("M_id", String.valueOf(masterid));
                    boolean isMaster=masterDeviceAdapter.isMasterType(position);

                    if(isMaster){
                        Intent i = new Intent(getApplicationContext(), AddSlaveActivity.class);
                        preference.setMasterNameForDevice(databaseHandler.getMasterNameById(masterid));
                        preference.setMasterIDForDevice(masterid);
                        startActivity(i);
                        preference.setSlaveActivityFromMaster(true);
                        preference.setFromDirectMaster(false);
                        finish();
                    }
                    else {
                        String slave_hexID=databaseHandler.getSlaveHexIdForMaster(masterid);
                        Log.e("Slave HEX ID",slave_hexID);
                        Intent i = new Intent(getApplicationContext(), AddSwitchActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        i.putExtra("slave_hex_id",slave_hexID);
                        startActivity(i);
                        preference.setFromDirectMaster(true);
                        finish();
                    }
                }
            }
        });

        masterDeviceList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final int master_id = databaseHandler.getMasterDeviceIdAtCurrentPosition(position);
                final String master_name = databaseHandler.getMasterNameById(master_id);
                final String masterLocalIP=databaseHandler.getMasterIPById(master_id);

                if (master_id == 200) {
                    return false;
                } else {
                    PopupMenu popupMenu = new PopupMenu(AddMasterActivity.this, view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_edit, popupMenu.getMenu());
                    popupMenu.setGravity(Gravity.CENTER);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            switch (item.getItemId()) {
                                case R.id.rename:
                                    showRenameMasterDialog(master_id, master_name,masterLocalIP);
                                    break;
                                case R.id.remove:
                                    showRemoveMasterDialog(master_id);
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

        drawerlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position) {
                    case 0:
                        intent = new Intent(getApplicationContext(), ContactUsActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        shareApp();
                        break;

                }
            }
        });

        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close) {

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                toolbar.setNavigationIcon(R.drawable.arrow_back);
                isDrawerOpen = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                toolbar.setNavigationIcon(R.drawable.drawer_icon);
                isDrawerOpen = false;
            }


        };
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(R.drawable.drawer_icon);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDrawerOpen) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    toolbar.setNavigationIcon(R.drawable.drawer_icon);
                    isDrawerOpen = false;
                } else {
                    drawerLayout.openDrawer(GravityCompat.START);
                    toolbar.setNavigationIcon(R.drawable.arrow_back);
                    isDrawerOpen = true;
                }

            }
        });
    }

    private void showRemoveMasterDialog(final int master_id) {
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(AddMasterActivity.this);
        confirmDelete.setTitle("Confirm to Remove");
        confirmDelete.setMessage("Are you sure to remove this device ?");
        confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                databaseHandler.deleteMasterDevice(master_id);
                databaseHandler.deleteAllSwitchesFromMaster(master_id);
                databaseHandler.deleteAllSlaveFromMaster(master_id);
                C.Toast(AddMasterActivity.this, "Master removed successfully");
                masterDeviceAdapter.notifyDataSetChanged();

                if (databaseHandler.getMastersCounts() < 2) {
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
        AlertDialog dialog = confirmDelete.create();
        dialog.show();
    }

    private void showRenameMasterDialog(final int master_id, final String master_name, final String masterLocalIP) {

        if (netcheck.isOnline()) {
            final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.custom_dialog_renameslave, null);
            dialogBuilder.setView(dialogView);

            final TextView txt_master = (TextView) dialogView.findViewById(R.id.txt_slave_name);
            final EditText edt_device_name = (EditText) dialogView.findViewById(R.id.edt_device_name);
            edt_device_name.setText(master_name);

            txt_master.setText("Master name");
            dialogBuilder.setTitle("Rename Master");
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
                                C.Toast(AddMasterActivity.this, "Please enter master name");
                            } else if (databaseHandler.isSameMasterName(edt_device_name.getText().toString().trim())) {
                                C.Toast(AddMasterActivity.this, "Device name already exists\nplease enter different Device name");
                            } else {

                                String renameMaster=edt_device_name.getText().toString().trim();

                                String command="";

                                String slaveID=databaseHandler.getSlaveHexIdForMaster(master_id);

                                JSONObject object=new JSONObject();
                                try {
                                    object.put("cmd",Cmd.MRN);
                                    object.put("data",renameMaster);
                                    object.put("slave", slaveID);
                                    object.put("token",databaseHandler.getSlaveToken(slaveID));
                                    command=object.toString();
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                List<String> ipList=new IPDb(getApplicationContext()).ipList();

                                Log.e("Command",command);

                                if(ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveID))){
                                    new SendUDP(command).execute();
                                }
                                else {
                                    new SendRenameForMaster(command).execute();
                                }

                                //Log.e("MRN cmd",sts);

                                masteridForRename = master_id;
                                masterNameForRename = renameMaster;

                                b.dismiss();
                            }
                        }
                    });
                }
            });
            b.show();
        } else {
            Toast.makeText(getApplicationContext(), "No internet connection found,\nplease check your connection first",
                    Toast.LENGTH_SHORT).show();
        }

    }

    private void fetchMasterDeviceFromServer() {
        if (netcheck.isOnline()) {
            if(manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()){
                if(!dialog.isShowing())
                    dialog.show();
                isCalledToShowMST=true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(dialog.isShowing()){
                            stopService(new Intent(getApplicationContext(), UDPService.class));
                            startService(new Intent(getApplicationContext(), UDPService.class));
                            Log.e("Started","--");
                            new SendUDP(AppConstant.CMD_GET_MASTER_TOKEN,true).execute();
                        }
                    }
                },10000);
                new SendUDP(AppConstant.CMD_GET_MASTER_TOKEN,true).execute();
            }
            else {
                C.Toast(getApplicationContext(),"Please Connect to Wi-Fi First to Add Device");
            }
        } else {
            Toast.makeText(getApplicationContext(), "No connection found,\nplease check your connection first",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void showDialog(String type) {
        showAddNewSlaveDialog(type);
    }

    private void showAddNewSlaveDialog(final String type) {
        isCalledToShowMST=false;
        isDialogShowing=true;
        dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setCancelable(false);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_addnewmaster, null);
        dialogBuilder.setView(dialogView);

        final LinearLayout add_master = (LinearLayout) dialogView.findViewById(R.id.add_master);
        final LinearLayout user_login = (LinearLayout) dialogView.findViewById(R.id.user_login);

        final TextView txt_name = (TextView) dialogView.findViewById(R.id.txt_slave_name);
        final Spinner spn_master = (Spinner) dialogView.findViewById(R.id.spn_master);
        final RadioGroup rdo_userType=(RadioGroup) dialogView.findViewById(R.id.radiogroup_type);
        final EditText edt_username = (EditText) dialogView.findViewById(R.id.edt_username);
        final EditText edt_pin = (EditText) dialogView.findViewById(R.id.edt_pin);

        spn_master.setAdapter(spn_adp);

        dialogBuilder.setTitle("Add New Master");
        dialogBuilder.setPositiveButton("Add Master", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        b = dialogBuilder.create();
        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        isDialogShowing=false;
                        master_names.clear();
                        master_ips.clear();
                        master_deviceIDs.clear();
                        b.dismiss();
                    }
                });
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (spn_master.getCount() == 0) {
                            Toast.makeText(getApplicationContext(), "No master found on server\nplease check connection first", Toast.LENGTH_SHORT).show();
                        } else if (spn_master.getSelectedItem().toString().contains("No Master")) {
                            Toast.makeText(getApplicationContext(), "Please select Master", Toast.LENGTH_SHORT).show();
                        } else if (databaseHandler.isSameMasterName(spn_master.getSelectedItem().toString())) {
                            Toast.makeText(getApplicationContext(), "Master you selected is already added\nplease select another Master", Toast.LENGTH_SHORT).show();
                        } else {

                            user_login.setVisibility(View.VISIBLE);
                            add_master.setVisibility(View.GONE);

                            btn_postive.setText("Add");

                            btn_postive.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String username = edt_username.getText().toString().trim();
                                    String pin = edt_pin.getText().toString().trim();
                                    Log.e("uid n pwd", username + " " + pin);

                                    if (username.isEmpty()) {
                                        C.Toast(getApplicationContext(), "Please enter User Name");
                                    } else if (pin.isEmpty()) {
                                        C.Toast(getApplicationContext(), "Please enter User PIN");
                                    } else if (pin.length()<4){
                                        C.Toast(getApplicationContext(),"Please enter 4 Digit PIN");
                                    }else {

                                        String loginCommand="";

                                        JSONObject loginObj=new JSONObject();

                                        try {
                                            if(rdo_userType.getCheckedRadioButtonId()==R.id.radio_admin){
                                                //loginCommand=AppConstant.CMD_ADMIN_LOGIN_1+username
                                                        //+AppConstant.CMD_ADMIN_LOGIN_2+pin+AppConstant.CMD_ADMIN_LOGIN_3;
                                                loginObj.put("cmd",Cmd.LIN);
                                            }
                                            else if(rdo_userType.getCheckedRadioButtonId()==R.id.radio_guest){
                                                //loginCommand=AppConstant.CMD_GUEST_LOGIN_1+username
                                                        //+AppConstant.CMD_GUEST_LOGIN_2+pin+AppConstant.CMD_GUEST_LOGIN_3;
                                                loginObj.put("cmd",Cmd.ULN);
                                            }

                                            loginObj.put("user",username);
                                            loginObj.put("pin",pin);
                                            loginObj.put("device_id",master_deviceIDs.get(spn_master.getSelectedItemPosition()));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        loginCommand=loginObj.toString();
                                        Log.e("loginCmd",loginCommand);

                                        int positionSelectedMaster=spn_master.getSelectedItemPosition();

                                        new SendUDP(loginCommand,spn_master.getSelectedItem().toString(),type,master_ips.get(positionSelectedMaster)).execute();
                                        b.dismiss();
                                        master_names.clear();
                                        master_ips.clear();
                                        master_deviceIDs.clear();
                                        isDialogShowing=false;

                                        /*Bean_Master bean_master = new Bean_Master();
                                        if (databaseHandler.getMastersCounts() == 1) {
                                            bean_master.setId(1);
                                        } else {
                                            bean_master.setId(databaseHandler.getMasterDeviceLastId() + 1);
                                        }
                                        bean_master.setName(spn_master.getSelectedItem().toString());

                                        databaseHandler.addMasterDeviceItem(bean_master);

                                        masterDeviceAdapter.notifyDataSetChanged();

                                        if (databaseHandler.getMastersCounts() > 1) {
                                            layout_no_addedslave.setVisibility(View.GONE);
                                            layout_addedslave.setVisibility(View.VISIBLE);
                                        }
                                        Toast.makeText(getApplicationContext(), "Master Added Successfully", Toast.LENGTH_SHORT).show();
                                        b.dismiss();
                                    } else {
                                        Toast.makeText(getApplicationContext(), "Please enter correct username and password", Toast.LENGTH_SHORT).show();*/
                                    }
                                }
                            });
                        }
                    }
                });
            }
        });
        b.show();
    }

    private boolean checkUserCredentials() {
        isUserCredentialTrue = false;

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_checkuserinfo, null);
        dialogBuilder.setView(dialogView);

        final EditText edt_username = (EditText) dialogView.findViewById(R.id.edt_username);
        final EditText edt_password = (EditText) dialogView.findViewById(R.id.edt_password);

        dialogBuilder.setTitle("Confirm Authentication");
        dialogBuilder.setPositiveButton("Add Master", null);
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
                        String username = edt_username.getText().toString().trim();
                        String password = edt_password.getText().toString().trim();

                        Log.e("uid n pwd", username + " " + password);

                        if (username.equals("")) {
                            Toast.makeText(getApplicationContext(), "Please enter User Name", Toast.LENGTH_SHORT).show();
                        } else if (password.equals("")) {
                            Toast.makeText(getApplicationContext(), "Please enter Password", Toast.LENGTH_SHORT).show();
                        } else if (username.equals("nimesh") && password.equals("nopassword")) {
                            isUserCredentialTrue = true;
                            b.dismiss();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please enter correct username and password", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
        b.show();

        Log.e("User details ", "" + isUserCredentialTrue);
        return isUserCredentialTrue;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (isDrawerOpen) {
            drawerLayout.closeDrawer(GravityCompat.START);
            toolbar.setNavigationIcon(R.drawable.drawer_icon);
            isDrawerOpen = false;
        } else {
            //stopService(new Intent(getApplicationContext(),UDPService.class));
            Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
            startActivity(intent);
            finish();
        }
    }

    public void logout() {
        drawerLayout.closeDrawer(GravityCompat.START);
        isDrawerOpen = false;
        drawerToggle.setDrawerIndicatorEnabled(false);
        //toolbar.setNavigationIcon(R.drawable.drawer_icon);
        AlertDialog.Builder logoutDialog = new AlertDialog.Builder(this);
        logoutDialog.setMessage("Are you sure to logout ?");
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                preference.setLoggedIn(false);
                preference.setMaster(false);
                Toast.makeText(getApplicationContext(), "Successfully logged out", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
        logoutDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        logoutDialog.show();
    }

    public void shareApp() {
        try {
            String shareBody = Globals.share;
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            //sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,""+s+"(Open it in Google Play Store to Download the Application)");

            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (Exception e) {

        }
    }

    private void publishCommandForMaster() {
        try {
            mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setUserName(AppConstant.MQTT_USERNAME);
            connectOptions.setPassword(AppConstant.getPassword());
            mqttClient.connect(connectOptions);
            MqttMessage mqttMessage = new MqttMessage(AppConstant.CMD_GET_MASTER.getBytes());
            mqttMessage.setRetained(true);
            mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
            mqttClient.disconnect();

        } catch (MqttException e) {
            Log.e("Exception : ", e.getMessage());
            Toast.makeText(getApplicationContext(), "Unable to make server connection", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class SendUDP extends AsyncTask<Void, Void, String> {
        String message;
        String masterName;
        String type;
        boolean showMaster = true;
        String ipAddress = "";

        public SendUDP(String message) {
            this.message = message;
            showMaster = true;
            //progressbar.setVisibility(View.VISIBLE);
        }

        public SendUDP(String message, boolean isAddMaster) {
            this.message = message;
            showMaster = isAddMaster;
            //progressbar.setVisibility(View.VISIBLE);
        }

        public SendUDP(String message, String masterName, String type) {
            this.message = message;
            this.masterName = masterName;
            selectedMasterName = masterName;
            masterType = type;
            this.type = type;
            showMaster = false;
            //progressbar.setVisibility(View.VISIBLE);
        }

        public SendUDP(String message, String masterName, String type, String ipAddress) {
            this.message = message;
            this.masterName = masterName;
            selectedMasterName = masterName;
            masterType = type;
            this.type = type;
            showMaster = false;
            this.ipAddress = ipAddress;
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

                //Log.e("IP Address Saved","->"+preference.getIpaddress());

                /*if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {*/

                /*if (showMaster) {*/
                    server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("Packet", "Sent");
                /*} else {
                    server_addr = new InetSocketAddress(this.ipAddress, 13001);
                    Log.e("IP Address", this.ipAddress);
                    preference.setIpaddress(this.ipAddress);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("Packet", "Sent");
                }*/
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

        private void checkLogin(String text) {

        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
            //progressbar.setVisibility(View.GONE);

            /*Log.e("text", "<->" + text);

            if (text != null) {
                try {
                    master_names.clear();
                    JSONObject jsonMaster = new JSONObject(text);
                    String cmd = jsonMaster.getString("cmd");
                    if (cmd.equalsIgnoreCase("MST")) {
                        master_names.add(jsonMaster.getString("m_name"));
                        spn_adp.notifyDataSetChanged();

                        if (master_names.size() > 0) {
                            showDialog(jsonMaster.getString("type"));
                        } else {
                            C.Toast(getApplicationContext(), "No Device Found");
                        }
                    } else if (cmd.equalsIgnoreCase("MRN")) {
                        String status = jsonMaster.getString("status");
                        if (status.equalsIgnoreCase("success")) {
                            if (masteridForRename != 0 && !masterNameForRename.equals("")) {
                                databaseHandler.renameMaster(masteridForRename, masterNameForRename);
                                masterDeviceAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                    else if(cmd.equalsIgnoreCase("LIN")){
                      String status=jsonMaster.getString("status");
                        if(status.equalsIgnoreCase("success")){

                        }
                    }

                } catch (Exception j) {
                    C.Toast(getApplicationContext(), j.getLocalizedMessage());
                }
            }*/


        }
    }

    public class SendCommandForMaster extends AsyncTask {

        public ProgressDialog statusDialog;


        @Override
        protected void onPreExecute() {
            statusDialog = new ProgressDialog(AddMasterActivity.this);
            statusDialog.setMessage("Please wait...");
            statusDialog.setIndeterminate(false);
            statusDialog.setCancelable(false);
            statusDialog.show();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            publishCommandForMaster();
            return null;
        }

        @Override
        protected void onProgressUpdate(Object[] values) {
            statusDialog.setMessage(values[0].toString());
        }

        @Override
        protected void onPostExecute(Object o) {
            statusDialog.dismiss();
        }
    }

    public class SendRenameForMaster extends AsyncTask<Void, Void, String> {

        public ProgressDialog statusDialog;
        String masterName;

        public SendRenameForMaster(String masterName) {
            this.masterName = masterName;
        }

        @Override
        protected void onPreExecute() {
            statusDialog = new ProgressDialog(AddMasterActivity.this);
            statusDialog.setMessage("Please wait...");
            statusDialog.setIndeterminate(false);
            statusDialog.setCancelable(false);
            statusDialog.show();
        }

        @Override
        protected String doInBackground(Void[] params) {
            String isDone = "0";

            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, preference.getMqttClientID(), new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);
                //String renameCommand = AppConstant.START_CMD_RENAME_MASTER + masterName+AppConstant.CMD_KEY_TOKEN+databaseHandler.getSlaveToken(databaseHandler.getSlaveHexIdForMaster(masteridForRename)) + AppConstant.END_CMD_RENAME_MASTER;
                MqttMessage mqttMessage = new MqttMessage(masterName.getBytes());
                mqttMessage.setRetained(true);
                mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                isDone = "1";
                mqttClient.disconnect();

            } catch (MqttException e) {
                Log.e("Exception : ", e.getMessage());
                Toast.makeText(getApplicationContext(), "Unable to make server connection", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

            return isDone;
        }

        @Override
        protected void onPostExecute(String s) {
            statusDialog.dismiss();
        }
    }
}
