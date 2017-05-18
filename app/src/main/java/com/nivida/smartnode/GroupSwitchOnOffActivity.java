package com.nivida.smartnode;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.a.Status;
import com.nivida.smartnode.adapter.CustomSwitchIconAdapter;
import com.nivida.smartnode.adapter.DimmerOnOffAdapter;
import com.nivida.smartnode.adapter.SwitchDimmerOnOffAdapter;
import com.nivida.smartnode.adapter.SwitchOnOffAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.GroupSwitchService;
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

public class GroupSwitchOnOffActivity extends AppCompatActivity implements SwitchDimmerOnOffAdapter.DimmerChangeCallBack, SwitchDimmerOnOffAdapter.OnSwitchSelection {

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    public static final String UDPSERVICE_CLASSNAME = "com.nivida.smartnode.services.UDPService";
    public static final int BUFFER_EXECUTION_TIME = 1000;
    RecyclerView switchView;
    SwitchOnOffAdapter switchOnOffAdapter;
    DimmerOnOffAdapter dimmerOnOffAdapter;
    TextView txt_smartnode;
    DatabaseHandler databaseHandler;
    Typeface typeface_raleway;
    LinearLayout switch_view, dimmer_view;
    GridView added_switchlist, added_dimmerlist;
    LinearLayout select_scene;
    SwitchDimmerOnOffAdapter switchDimmerOnOffAdapter;
    Toolbar toolbar;
    int groupid = 0;
    ProgressDialog loadingView;
    ArrayList<String> button_list = new ArrayList<>();
    ArrayList<String> buttonType = new ArrayList<>();
    ArrayList<String> buttonStatus = new ArrayList<>();
    ArrayList<String> buttonUserLock = new ArrayList<>();
    ArrayList<String> buttonTouchLock = new ArrayList<>();
    List<Bean_Switch> switchListToAdd = new ArrayList<>();
    ArrayList<String> scheduleInfo = new ArrayList<>();
    List<String> slaveIds=new ArrayList<>();
    MqttClient mqttClient;
    String clientId = "";
    String subscribedMessage = "";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue = false;
    NetworkUtility netcheck;
    AppPreference preference;
    boolean isFirstTimeEntered = true;
    List<String> commandBuffer = new ArrayList<>();
    Handler mHandler;
    Runnable mRunnable;
    ProgressDialog dialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_switch_on_off);
        databaseHandler = new DatabaseHandler(getApplicationContext());
        preference = new AppPreference(getApplicationContext());
        netcheck = new NetworkUtility(getApplicationContext());
        clientId = MqttClient.generateClientId();
        Intent intent = getIntent();
        groupid = intent.getIntExtra("group_id", 0);
        slaveIds = databaseHandler.getSlaveHexIdsForGroup(groupid);

        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Please Wait...");

        startGroupService();
        startReceiver();
        //getLiveSwitchStatus();

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                if (commandBuffer.size() > 0) {
                    handleCommands(commandBuffer.get(0));
                    commandBuffer.remove(0);
                }
                mHandler.postDelayed(this, BUFFER_EXECUTION_TIME);
            }
        };
        mHandler.postDelayed(mRunnable, BUFFER_EXECUTION_TIME);

        try {
            switchDimmerOnOffAdapter = new SwitchDimmerOnOffAdapter(getApplicationContext(), databaseHandler.getAllSwitchesByGroupId(groupid),
                    Globals.GROUP, this);
            switchDimmerOnOffAdapter.setCallBack(this);
        } catch (Exception e) {
            //C.connectionError(getApplicationContext());
        }


        typeface_raleway = Typeface.createFromAsset(getAssets(), "fonts/raleway.ttf");
        txt_smartnode = (TextView) findViewById(R.id.txt_smartnode);
        txt_smartnode.setTypeface(typeface_raleway);
        try {
            txt_smartnode.setText(databaseHandler.getGroupnameById(groupid));
        } catch (Exception e) {
            //C.connectionError(getApplicationContext());
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        fetchID();
    }

    private void getLiveSwitchStatus() {
        if (netcheck.isOnline()) {

            try{
                dialog.show();
            }
            catch (Exception e){
                Log.e("Window","Leaked");
            }

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        C.Toast(getApplicationContext(), "It might Device not Responding or Your Connection is Poor.\nPlease Try Again Later!");
                    }
                }
            }, 12000);

            try {
                for (int i = 0; i < slaveIds.size(); i++) {
                    isFirstTimeEntered = true;
                    /*if (preference.isOnline()) {
                        new GetLiveStatus(slaveIds.get(i)).execute();
                        Log.e("Call from", "MQTT");
                    } else {*/

                    String slaveIPAddress=databaseHandler.getMasterIPBySlaveID(slaveIds.get(i));

                    //Log.e("IP Addr",preference.getCurrentIPAddr()+" --> "+slaveIPAddress);

                    List<String> ipList=new IPDb(this).ipList();

                        if (ipList.contains(slaveIPAddress)) {
                            String mqttCommand = AppConstant.START_CMD_STATUS_OF_SLAVE + slaveIds.get(i) + AppConstant.CMD_KEY_TOKEN + databaseHandler.getSlaveToken(slaveIds.get(i)) + AppConstant.END_CMD_STATUS_OF_SLAVE;
                            new SendUDP(mqttCommand).execute();
                            Log.e("Call from", "UDP" + "\n" + mqttCommand);
                        } else {
                            new GetLiveStatus(slaveIds.get(i)).execute();
                            Log.e("Call from", "MQTT");
                        }


                    //}
                }
            } catch (Exception e) {
                //C.connectionError(getApplicationContext());
            }


        } else {
            Toast.makeText(getApplicationContext(), "No internet connection found\nplease try again later", Toast.LENGTH_SHORT).show();
        }

    }

    private void startGroupService() {
        if (!UDPServiceIsRunning()) {
            Intent intent = new Intent(this, UDPService.class);
            startService(intent);
        }

        if (!serviceIsRunning()) {
            Intent intent = new Intent(this, AddDeviceService.class);
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

    private boolean UDPServiceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (UDPSERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();

                if (bundle != null) {

                    subscribedMessage = bundle.getString(AddDeviceService.MESSAGETOSEND);
                    String UDPMessage = bundle.getString(UDPService.MESSAGEJSON);

                    //Log.e("JSOn fr group ", "" + subscribedMessage);
                    if (UDPMessage != null) {
                        commandBuffer.add(UDPMessage);
                        //handleCommands(UDPMessage);
                    } else if (subscribedMessage == null) {
                        //Log.e("JSON Message", "Null");
                    } else if (subscribedMessage.equals("")) {
                        Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                    } /*else if (subscribedMessage.contains("master")) {
                        //Log.e("From Grp actvty ", "Device Started...");
                    } */ else {
                        commandBuffer.add(subscribedMessage);
                        //handleCommands(subscribedMessage);
                    }
                }
            }
        };
    }

    private void handleCommands(String json) {
        String slave_hex_id = "";
        String button = "";
        String val = "";
        String dval = "";
        try {
            JSONObject jsonDevice = new JSONObject(json);
            String cmd = jsonDevice.getString("cmd");
            if(jsonDevice.has("status") && jsonDevice.getString("status").equalsIgnoreCase(Cmd.INVALID_TOKEN)){

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                if (jsonDevice.has("slave")) {
                    String slaveID = jsonDevice.getString("slave");
                    if (!slaveIds.contains(slaveID)) return;
                    C.Toast(this, "Token is Invalid\nPlease Login again to refresh token!");
                }
                return;
            }

            if (jsonDevice.has("slave")) {
                slave_hex_id = jsonDevice.getString("slave");
                if (!slaveIds.contains(slave_hex_id)) {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    return;
                }
            } else {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                return;
            }

            if (cmd.equals("SET")) {

                if (dialog.isShowing()) {
                    dialog.dismiss();
                }

                Log.e("Command :", "SET Found");

                button = jsonDevice.getString("button");
                val = jsonDevice.getString("val");
                dval = jsonDevice.getString("dval");
                String msg = switchDimmerOnOffAdapter.setSwitchItem(slave_hex_id, button, val, dval);

                if (msg.equalsIgnoreCase("success")) {
                    switchDimmerOnOffAdapter.notifyDataSetChanged();
                }
            } else if (cmd.equals("STS")) {
                if (isFirstTimeEntered) {
                    isFirstTimeEntered = false;
                    if (dialog != null && dialog.isShowing()) {
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dialog.dismiss();
                            }
                        },2000);
                    }
                }
                //Log.e("Command :", "STS Found");
                updateSwitchesAsLive(json);
            } else if (cmd.equalsIgnoreCase(Cmd.TL1)) {
                Log.e("Command :", "TL1 Found");
                updateSwitchForLocks(json, true);
            } else if (cmd.equals(Cmd.UL1)) {
                updateSwitchForLocks(json, false);
            } else if (cmd.equals(Cmd.DM1)) {
                updateSwitchTypes(json);
            } else if (cmd.equals(Cmd.UUP) && jsonDevice.has("status") && jsonDevice.getString("status").equalsIgnoreCase(Status.LOCKED)) {
                //C.Toast(getApplicationContext(),"Switch you want to access is locked by Admin.");
            }

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

        } catch (JSONException e) {
            Log.e("JSON Message : ", e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateSwitchTypes(String json) {
        try {
            JSONObject object = new JSONObject(json);

            String status = object.getString("status");
            String slaveID = object.getString("slave");
            String data = object.getString("data");

            if (status.equals(Status.SUCCESS)) {
                String switchNum = String.valueOf(data.charAt(0)) + String.valueOf(data.charAt(1));
                String sts = String.valueOf(data.charAt(2));

                String type = sts.equalsIgnoreCase("Y") ? "d" : "s";

                try {
                    databaseHandler.updateSwitchTypes(slaveID, switchNum, type);
                } catch (Exception e) {
                    //C.connectionError(getApplicationContext());
                }


            } else {
                C.Toast(getApplicationContext(), getString(R.string.error));
            }
        } catch (Exception e) {
            //Log.e("Exception", e.getMessage());
        }
        switchDimmerOnOffAdapter.notifyIconChanged();
    }

    private void updateSwitchForLocks(String json, boolean isTouchLock) {
        try {
            JSONObject object = new JSONObject(json);
            if (object.has("status") && object.getString("status").equals(Status.SUCCESS)) {
                String data = object.getString("data");
                String slaveID = object.getString("slave");
                Bean_Switch switcheItem = new Bean_Switch();
                switcheItem.setSwitchInSlave(slaveID);

                String switchButtonNum = String.valueOf(data.charAt(0)) + String.valueOf(data.charAt(1));
                String sts = String.valueOf(data.charAt(2));

                switcheItem.setSwitch_btn_num(switchButtonNum);

                try {
                    if (isTouchLock) {
                        switcheItem.setTouchLock(sts);
                        databaseHandler.updateSwitchLocks(switcheItem, true);
                    } else {
                        switcheItem.setUserLock(sts);
                        databaseHandler.updateSwitchLocks(switcheItem, false);
                    }
                } catch (Exception e) {
                    //C.connectionError(getApplicationContext());
                }


                switchDimmerOnOffAdapter.notifyIconChanged();
            } else {
                C.Toast(getApplicationContext(), "Failed to Perform Lock Operation!");
            }
        } catch (JSONException e) {
            //Log.e("Exception", e.getMessage());
        }
    }

    private void updateSwitchesAsLive(String subscribedMessage) {
        String slave_id = "";

        try {
            JSONObject jsonSwitches = new JSONObject(subscribedMessage);
            slave_id = jsonSwitches.getString("slave");

            try {
                //List<Bean_Switch> switchesInDB = databaseHandler.getAllSwitches(groupid, slave_id);

                button_list.clear();
                buttonType.clear();
                buttonStatus.clear();
                buttonUserLock.clear();
                buttonTouchLock.clear();
                scheduleInfo.clear();

                String userLock = jsonSwitches.getString("user_locked");
                String touchLock = jsonSwitches.getString("touch_lock");
                String buttons = jsonSwitches.getString("button");
                String schedules = jsonSwitches.getString("schedule_info");
                int j = 0;
                for (int i = 0; i < buttons.length(); i += 2) {
                    button_list.add(String.valueOf(buttons.charAt(i)) + String.valueOf(buttons.charAt(i + 1)));
                    ////Log.e("Button Added :", button_list.get(j));
                    //j++;
                }

                String dimmerValue = jsonSwitches.getString("dval");

                for (int i = 0; i < dimmerValue.length(); i++) {
                    buttonType.add(String.valueOf(dimmerValue.charAt(i)));
                    ////Log.e("Dimmer Value :", buttonType.get(i));
                }

                String buttonStatusValues = jsonSwitches.getString("val");

                for (int i = 0; i < buttonStatusValues.length(); i++) {
                    buttonStatus.add(String.valueOf(buttonStatusValues.charAt(i)));
                    ////Log.e("Switch status :", buttonStatus.get(i));
                }

                for (int i = 0; i < userLock.length(); i++) {
                    buttonUserLock.add(String.valueOf(userLock.charAt(i)));
                }

                for (int i = 0; i < touchLock.length(); i++) {
                    buttonTouchLock.add(String.valueOf(touchLock.charAt(i)));
                }

                for (int i = 0; i < schedules.length(); i += 2) {
                    scheduleInfo.add(String.valueOf(schedules.charAt(i)) + String.valueOf(schedules.charAt(i + 1)));
                }

                switchListToAdd.clear();

                for (int i = 0; i < button_list.size(); i++) {

                    int onOff = 0;
                    int dval = 0;
                    String type = "";
                    if (buttonStatus.get(i).equalsIgnoreCase("A")) onOff = 1;
                    else onOff = 0;

                    if (buttonType.get(i).equalsIgnoreCase("X")) {
                        dval = 0;
                        type = "s";
                    } else {
                        dval = Integer.parseInt(buttonType.get(i));
                        type = "d";
                    }

                    databaseHandler.updateSwitch(groupid, slave_id, button_list.get(i), onOff, dval, type, buttonUserLock.get(i), buttonTouchLock.get(i));

                    int scheduleCount = Integer.parseInt(scheduleInfo.get(i));

                    if (scheduleCount > 0) {
                        databaseHandler.setSwitchHasSchedule(slave_id, button_list.get(i), true);
                    } else {
                        databaseHandler.setSwitchHasSchedule(slave_id, button_list.get(i), false);
                    }
                }
            } catch (Exception e) {
                //C.connectionError(getApplicationContext());
            }
            switchDimmerOnOffAdapter.notifyIconChanged();

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(GroupSwitchService.NOTIFICATION));
        if (UDPServiceIsRunning()) {
            stopService(new Intent(getApplicationContext(), UDPService.class));
            startService(new Intent(getApplicationContext(), UDPService.class));
        }

        if (mHandler != null) {
            mHandler.postDelayed(mRunnable, BUFFER_EXECUTION_TIME);
        }

        getLiveSwitchStatus();
        //Log.e("Reciever :", "Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        //Log.e("Reciever :", "UnRegistered");
    }

    private void fetchID() {
        added_switchlist = (GridView) findViewById(R.id.added_switchlist);
        added_switchlist.setAdapter(switchDimmerOnOffAdapter);

        switch_view = (LinearLayout) findViewById(R.id.switch_view);
        select_scene = (LinearLayout) findViewById(R.id.btn_select_scene);

        select_scene.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), SceneActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("group_id", groupid);
                startActivity(intent);
                finish();
            }
        });


        try {
            if (databaseHandler.getSwitchesInGroup(groupid) == 0) {
                switch_view.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            //C.connectionError(getApplicationContext());
        }


    }

    private void showChangeSwitchIconDialog(final int groupid, final int switchID, String switch_name) {

        final int[] switch_icon = {1};
        final GridView iconsView = new GridView(this);
        final CustomSwitchIconAdapter iconAdapter = new CustomSwitchIconAdapter(getApplicationContext(), databaseHandler.getAllSwitchIconData());
        iconsView.setAdapter(iconAdapter);
        iconsView.setNumColumns(2);
        iconsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                iconAdapter.setIconChecked(position);
                iconsView.setItemChecked(position, true);
                switch_icon[0] = iconAdapter.getSwitchIconId(position);
            }
        });

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(iconsView);

        dialogBuilder.setTitle("Change Switch Icon");
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

                        try {
                            databaseHandler.changeSwitchIcon(groupid, switchID, switch_icon[0]);
                        } catch (Exception e) {
                            //C.connectionError(getApplicationContext());
                        }


                        switchDimmerOnOffAdapter.notifyIconChanged();
                        b.dismiss();
                    }
                });
            }
        });
        b.show();


    }

    private void showRenameDeviceDialog(final int group_id, final int switch_id, String switch_name, final int position) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_renameslave, null);
        dialogBuilder.setView(dialogView);

        final TextView txt_switch = (TextView) dialogView.findViewById(R.id.txt_slave_name);
        final EditText edt_device_name = (EditText) dialogView.findViewById(R.id.edt_device_name);
        edt_device_name.setText(switch_name);
        txt_switch.setText("Switch name");

        dialogBuilder.setTitle("Rename Switch");
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
                            Toast.makeText(GroupSwitchOnOffActivity.this, "Please enter switch name", Toast.LENGTH_SHORT).show();
                        } else {

                            try {
                                databaseHandler.renameSwitch(group_id, switch_id, edt_device_name.getText().toString().trim());
                                databaseHandler.renameSwitchInScenes((Bean_Switch) switchDimmerOnOffAdapter.getItem(position), edt_device_name.getText().toString());
                                Toast.makeText(GroupSwitchOnOffActivity.this, "Switch renamed successfully", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                //C.connectionError(getApplicationContext());
                            }
                            switchDimmerOnOffAdapter.notifyDataSetChanged();
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    private void showRemoveItemFromGroupDialog(final int groupID, final int switchID, final int position) {

        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(this);
        confirmDelete.setTitle("Confirm to remove");
        confirmDelete.setMessage("Are you sure to remove ?");
        confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    databaseHandler.removeSwitchFromGroup(switchID);
                    Toast.makeText(getApplicationContext(), "Switch removed successfully", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    //C.connectionError(getApplicationContext());
                }


                switchDimmerOnOffAdapter.notifyDataSetChanged();
                dialog.dismiss();

                try {
                    int totalSwitchNow = databaseHandler.removeSwitchFromScenes((Bean_Switch) switchDimmerOnOffAdapter.getItem(position));
                    if (totalSwitchNow < 1) {
                        onBackPressed();
                    }
                } catch (Exception e) {
                    //C.connectionError(getApplicationContext());
                }


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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void stopScrolling() {
        added_switchlist.setVerticalScrollBarEnabled(false);
    }

    @Override
    public void startScrolling() {
        added_switchlist.setVerticalScrollBarEnabled(true);
    }

    @Override
    public void showOptionMenu(final int position, View view) {
        final int groupID = switchDimmerOnOffAdapter.getGroupIdAtPosition(position);
        final int switchID = switchDimmerOnOffAdapter.getSwitchIdAtPosition(position);
        final String switch_name = switchDimmerOnOffAdapter.getSwitchName(position);
        final Boolean isSwitch = switchDimmerOnOffAdapter.isSwitch(position);
        final String switch_num = switchDimmerOnOffAdapter.getSwitchNumber(position);
        final String slaveID = switchDimmerOnOffAdapter.getSlaveIDForSwitch(position);
        //Log.e("isSwitch or :", "" + isSwitch);

        PopupMenu popupMenu = new PopupMenu(GroupSwitchOnOffActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.menu_edit_switch, popupMenu.getMenu());
        //Log.e("isSwitch :", "" + isSwitch);

        popupMenu.setGravity(Gravity.CENTER);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.rename:
                        showRenameDeviceDialog(groupID, switchID, switch_name, position);
                        break;
                    case R.id.change_icon:
                        showChangeSwitchIconDialog(groupID, switchID, switch_name);
                        break;
                    case R.id.remove:
                        showRemoveItemFromGroupDialog(groupID, switchID, position);
                        break;
                    case R.id.change_type:
                        changeTypefrom(groupID, switchID, position, isSwitch, switch_num, slaveID);
                        break;
                }

                return true;
            }
        });

        popupMenu.show();
    }

    private void changeTypefrom(final int groupID, final int switchID, int position, final Boolean isSwitch, String switch_num, String slaveID) {
        JSONObject object = new JSONObject();
        try {
            object.put("cmd", Cmd.DM1);
            object.put("token", databaseHandler.getSlaveToken(slaveID));
            String dmData = switch_num;
            if (isSwitch) {
                dmData += "Y";
            } else {
                dmData += "N";
            }

            object.put("slave", slaveID);
            object.put("data", dmData);

            String command = object.toString();

            //Log.e("command", command);

            if (NetworkUtility.isOnline(getApplicationContext())) {

                List<String> ipList=new IPDb(this).ipList();

                if(ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveID))){
                    new SendUDP(command).execute();
                }
                else {
                  new SendMQTT(databaseHandler.getSlaveTopic(slaveID) + AppConstant.MQTT_PUBLISH_TOPIC,command).execute();
                }
            } else {
                C.Toast(getApplicationContext(), getString(R.string.nointernet));
            }
        } catch (Exception e) {
            ////Log.e("Exception", e.getMessage());
        }
    }

    @Override
    public void sendUDPCommand(String command) {
        new SendUDP(command).execute();
    }

    public class GetLiveStatus extends AsyncTask<Void, Void, String> {

        String slave_hex_id = "";

        public GetLiveStatus(String slave_hex_id) {
            loadingView = new ProgressDialog(GroupSwitchOnOffActivity.this);
            loadingView.setMessage("Please wait...");
            loadingView.setIndeterminate(false);
            loadingView.setCancelable(true);
            //loadingView.show();
            this.slave_hex_id = slave_hex_id;
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
                String mqttCommand = AppConstant.START_CMD_STATUS_OF_SLAVE + slave_hex_id + AppConstant.CMD_KEY_TOKEN + databaseHandler.getSlaveToken(slave_hex_id) + AppConstant.END_CMD_STATUS_OF_SLAVE;
                MqttMessage mqttMessage = new MqttMessage(mqttCommand.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(databaseHandler.getSlaveTopic(slave_hex_id) + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                //Log.e("Mqtt Message", "Published for STS 1st\n" + mqttCommand);
                mqttClient.disconnect();

            } catch (MqttException e) {
                //Log.e("MQTT Exception",e.getMessage());
            } catch (Exception e) {
                //Log.e("Exception",e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //loadingView.dismiss();
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
                server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
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
                //Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                //Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
            //progressbar.setVisibility(View.GONE);

        }
    }

    public class SendMQTT extends AsyncTask<Void, Void, Void>{

        String topic="";
        String command="";

        public SendMQTT(String topic, String command) {
            this.topic = topic;
            this.command = command;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, C.MQTT_ClientID, new MemoryPersistence());
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
}
