package com.nivida.smartnode;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
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
import com.nivida.smartnode.adapter.SwitchDimmerRecyclerAdapter;
import com.nivida.smartnode.adapter.SwitchOnOffAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.NetworkUtility;

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

public class FavouriteActivity extends AppCompatActivity implements SwitchDimmerOnOffAdapter.DimmerChangeCallBack, SwitchDimmerOnOffAdapter.OnSwitchSelection {

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    private static final int[] CHECK_STS_AGAIN_TIME_INTERVEL = {5000};
    RecyclerView switchView;
    SwitchOnOffAdapter switchOnOffAdapter;
    DimmerOnOffAdapter dimmerOnOffAdapter;
    TextView txt_smartnode;
    DatabaseHandler databaseHandler;
    Typeface typeface_raleway;
    LinearLayout switch_view,dimmer_view,hasFavourites,favourite_not_found;
    GridView added_switchlist,added_dimmerlist;
    Toolbar toolbar;
    AppPreference preference;
    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue=false;
    NetworkUtility netcheck;

    ArrayList<String> button_list=new ArrayList<>();
    ArrayList<String> buttonType=new ArrayList<>();
    ArrayList<String> buttonStatus=new ArrayList<>();
    ArrayList<String> buttonUserLock = new ArrayList<>();
    ArrayList<String> buttonTouchLock = new ArrayList<>();
    List<Bean_Switch> switchListToAdd=new ArrayList<>();
    ArrayList<String> scheduleInfo = new ArrayList<>();

    List<String> slaveIds = new ArrayList<>();

    SwitchDimmerRecyclerAdapter recyclerAdapter;

    RecyclerView recyclerView;

    boolean isFirstTimeOpened = true;

    List<String> slaveSTSReceived = new ArrayList<>();
    Handler liveStatusHandler;
    Runnable liveStatusRunnable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite);
        databaseHandler=new DatabaseHandler(getApplicationContext());
        preference=new AppPreference(getApplicationContext());
        netcheck=new NetworkUtility(getApplicationContext());

        startGroupService();
        startReceiver();


        try{
            List<Bean_Switch> switchList = databaseHandler.getAllSwitchesInFavourite();


            recyclerAdapter = new SwitchDimmerRecyclerAdapter(switchList, this, Globals.FAVOURITE, this);
            recyclerAdapter.setCallBack(this);
            recyclerAdapter.startToCheckResendCommands();
        }catch (Exception e){
            //C.connectionError(getApplicationContext());
        }





        typeface_raleway= Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        txt_smartnode=(TextView) findViewById(R.id.txt_smartnode);
        txt_smartnode.setTypeface(typeface_raleway);
        toolbar=(Toolbar) findViewById(R.id.toolbar);
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

    private void startGroupService() {

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

    private boolean serviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startReceiver() {
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle=intent.getExtras();
                if(bundle!=null){

                    subscribedMessage=bundle.getString(AddDeviceService.MESSAGETOSEND);
                    String UDPMessage=bundle.getString(UDPService.MESSAGEJSON);

                    //Log.e("JSOn fr group ", "" + subscribedMessage + " " + UDPMessage);
                    if(UDPMessage!=null){
                        handleCommands(UDPMessage);
                    }
                    else if(subscribedMessage==null){
                        //Log.e("JSON Message", "Null");
                    }
                    else if(subscribedMessage.equals("")){
                        Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                    }
                    else if(subscribedMessage.contains("master")){
                        //Log.e("From Grp actvty ","Device Started...");
                    }
                    else {
                       handleCommands(subscribedMessage);
                    }
                }
            }
        };
    }

    private void handleCommands(String json) {
        String slave_hex_id="";
        String button="";
        String val="";
        String dval="";
        try{
            JSONObject jsonDevice=new JSONObject(json);
            String cmd=jsonDevice.getString("cmd");

            if (jsonDevice.has("status") && jsonDevice.getString("status").equalsIgnoreCase(Cmd.INVALID_TOKEN)) {

                if (jsonDevice.has("slave")) {
                    String slaveID = jsonDevice.getString("slave");
                    if (!slaveIds.contains(slaveID)) return;
                    C.Toast(this, "Token is Invalid\nPlease Login again to refresh token!");
                }
                return;


            }

            if (cmd.equals(Cmd.INTERNET)) {
                C.Toast(getApplicationContext(), jsonDevice.getString("message"));
                if (jsonDevice.getString("type").equals(Cmd.NOT_CONNECTED)) {
                    setAllDeviceOffline();
                }
                return;
            }

            if (jsonDevice.has("slave")) {
                slave_hex_id = jsonDevice.getString("slave");
                if (!slaveIds.contains(slave_hex_id)) return;
            } else {
                return;
            }

            if(cmd.equals("SET")){
                //Log.e("Command :","SET Found");
                button=jsonDevice.getString("button");
                val=jsonDevice.getString("val");
                dval=jsonDevice.getString("dval");
                String msg = recyclerAdapter.setSwitchItem(slave_hex_id, button, val, dval);

                /*if(msg.equalsIgnoreCase("success")){
                    switchDimmerOnOffAdapter.notifyDataSetChanged();
                }*/
            }
            else if(cmd.equals("STS")){
                ////Log.e("Command :", "STS Found");
                updateSwitchesAsLive(json);
            }
            else if(cmd.equalsIgnoreCase(Cmd.TL1)){
                //Log.e("Command :","TLK Found");
                updateSwitchForLocks(json,true);
            }
            else if(cmd.equals(Cmd.UL1)){
                updateSwitchForLocks(json,false);
            }
            else if(cmd.equals(Cmd.DM1)){
                updateSwitchTypes(json);
            }


        } catch (JSONException e) {
            //Log.e("JSON Message : ",e.getMessage());
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
                try{
                    databaseHandler.updateSwitchTypes(slaveID, switchNum, type);
                }catch (Exception e){
                    //C.connectionError(getApplicationContext());
                }

                recyclerAdapter.notifyIconChanged();
            } else {
                C.Toast(getApplicationContext(), getString(R.string.error));
            }
        } catch (Exception e) {
            //Log.e("Exception", e.getMessage());
        }
    }

    private void updateSwitchForLocks(String json, boolean isTouchLock) {

        try {
            JSONObject object = new JSONObject(json);
            if (object.has("status") && object.getString("status").equals(Status.SUCCESS)) {
                String data = object.getString("data");
                String slaveID = object.getString("slave");
                Bean_Switch switcheItem = new Bean_Switch();
                switcheItem.setSwitchInSlave(slaveID);

                String switchButtonNum=String.valueOf(data.charAt(0))+String.valueOf(data.charAt(1));
                String sts=String.valueOf(data.charAt(2));

                //switcheItem.setSwitch_btn_num(switchButtonNum);
                recyclerAdapter.setLockStatusChanged(slaveID, switchButtonNum, sts, isTouchLock);
            } else {
                C.Toast(getApplicationContext(), "Failed to Perform Lock Operation!");
            }
        } catch (JSONException e) {
            //Log.e("Exception", e.getMessage());
        }
    }

    private void getLiveSwitchStatus() {
        if(netcheck.isOnline()){

            for(int i=0; i<slaveIds.size() ; i++){

                JSONObject object=new JSONObject();
                try {
                    object.put("cmd",Cmd.STS);
                    object.put("slave",slaveIds.get(i));
                    object.put("token",databaseHandler.getSlaveToken(slaveIds.get(i)));

                    if (SmartNode.slavesInLocal.contains(slaveIds.get(i))) {
                        new SendUDP(object.toString()).execute();
                    }
                    else {
                        new GetLiveStatus(slaveIds.get(i)).execute();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No internet connection found\nplease try again later", Toast.LENGTH_SHORT).show();
        }

    }

    private void setAllDeviceOffline() {
        List<String> workingSlaves = new ArrayList<>();
        workingSlaves.addAll(SmartNode.slavesWorking);
        for (int i = 0; i < workingSlaves.size(); i++) {
            if (!SmartNode.slavesInLocal.contains(workingSlaves.get(i))) {
                SmartNode.slavesWorking.remove(workingSlaves.get(i));
                recyclerAdapter.setAllSwitchesOffline(workingSlaves.get(i));

            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void setCommandsFromCache(String commandInCache, String slaveID) {
        //Log.e("CommandInCache", commandInCache);
        try {
            JSONArray array = new JSONArray(commandInCache);

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                handleCommands(object.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendSTSCommand(boolean onLAN, String slaveID) {
        try {
            if (onLAN) {
                CHECK_STS_AGAIN_TIME_INTERVEL[0] = 2000;
                String mqttCommand = AppConstant.START_CMD_STATUS_OF_SLAVE + slaveID + AppConstant.CMD_KEY_TOKEN + databaseHandler.getSlaveToken(slaveID) + AppConstant.END_CMD_STATUS_OF_SLAVE;

                //Log.e("command", mqttCommand);
                new SendUDP(mqttCommand).execute();
            } else {
                CHECK_STS_AGAIN_TIME_INTERVEL[0] = 5000;
                new GetLiveStatus(slaveID).execute();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSwitchesAsLive(String subscribedMessage) {
        String slave_id="";

        try {
            JSONObject jsonSwitches=new JSONObject(subscribedMessage);
            slave_id=jsonSwitches.getString("slave");

            if (!slaveSTSReceived.contains(slave_id)) slaveSTSReceived.add(slave_id);

            button_list.clear();
            buttonType.clear();
            buttonStatus.clear();
            buttonUserLock.clear();
            buttonTouchLock.clear();
            scheduleInfo.clear();

            String userLock=jsonSwitches.getString("user_locked");
            String touchLock=jsonSwitches.getString("touch_lock");
            String buttons=jsonSwitches.getString("button");
            String schedules = jsonSwitches.getString("schedule_info");
            int j=0;
            for(int i=0; i<buttons.length() ; i+=2){
                button_list.add(String.valueOf(buttons.charAt(i))+String.valueOf(buttons.charAt(i+1)));
                //Log.e("Button Added :",button_list.get(j));
                j++;
            }

            String dimmerValue=jsonSwitches.getString("dval");

            for(int i=0;i<dimmerValue.length();i++){
                buttonType.add(String.valueOf(dimmerValue.charAt(i)));
                //Log.e("Dimmer Value :",buttonType.get(i));
            }

            String buttonStatusValues=jsonSwitches.getString("val");

            for(int i=0;i<buttonStatusValues.length();i++){
                buttonStatus.add(String.valueOf(buttonStatusValues.charAt(i)));
                //Log.e("Switch status :",buttonStatus.get(i));
            }

            for(int i=0; i<userLock.length(); i++){
                buttonUserLock.add(String.valueOf(userLock.charAt(i)));
            }

            for(int i=0; i<touchLock.length(); i++){
                buttonTouchLock.add(String.valueOf(touchLock.charAt(i)));
            }

            for (int i = 0; i < schedules.length(); i += 2) {
                scheduleInfo.add(String.valueOf(schedules.charAt(i)) + String.valueOf(schedules.charAt(i + 1)));
            }

            switchListToAdd.clear();

            for(int i=0;i<button_list.size();i++){

                int onOff=0;
                int dval=0;
                String type="";
                if(buttonStatus.get(i).equalsIgnoreCase("A"))  onOff=1;
                else onOff=0;

                if (buttonType.get(i).equalsIgnoreCase("X")) {dval = 0;
                    type="s";
                }
                else {dval = Integer.parseInt(buttonType.get(i));
                    type="d";
                }

                databaseHandler.updateSwitchInFavourite(slave_id,button_list.get(i),onOff,dval,type,buttonUserLock.get(i),buttonTouchLock.get(i));

                int scheduleCount = Integer.parseInt(scheduleInfo.get(i));

                if (scheduleCount > 0) {
                    databaseHandler.setSwitchHasSchedule(slave_id, button_list.get(i), true);
                } else {
                    databaseHandler.setSwitchHasSchedule(slave_id, button_list.get(i), false);
                }
            }
            recyclerAdapter.notifyIconChanged(slave_id);

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver,new IntentFilter(AddDeviceService.NOTIFICATION));
        //Log.e("Reciever :","Registered");
        if (!isFirstTimeOpened) {
            slaveSTSReceived.clear();
            new WatchForLiveStatus().execute();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        //Log.e("Reciever :","UnRegistered");
    }

    private void fetchID() {

        slaveIds = databaseHandler.getSlaveHexIdsForFavourite();

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(this, 2, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(recyclerAdapter);

        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        added_switchlist=(GridView) findViewById(R.id.added_switchlist);

        switch_view=(LinearLayout) findViewById(R.id.switch_view);
        hasFavourites=(LinearLayout) findViewById(R.id.hasFavoutites);
        favourite_not_found=(LinearLayout) findViewById(R.id.favourite_not_found);

        if(databaseHandler.getSwitchesInFavoutite()==0){
            switch_view.setVisibility(View.GONE);
            favourite_not_found.setVisibility(View.VISIBLE);
            hasFavourites.setVisibility(View.GONE);
        }
        else {
            favourite_not_found.setVisibility(View.GONE);
            hasFavourites.setVisibility(View.VISIBLE);
            isFirstTimeOpened = false;
            new WatchForLiveStatus().execute();
        }
    }

    @Override
    public void onBackPressed() {
        recyclerAdapter.stopToCheckResendCommand();
        Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void stopScrolling() {

    }

    @Override
    public void startScrolling() {

    }

    @Override
    public void showOptionMenu(final int position, View view) {

        final int groupID = recyclerAdapter.getGroupIdAtPosition(position);
        final int switchID = recyclerAdapter.getSwitchIdAtPosition(position);
        final String switch_name = recyclerAdapter.getSwitchName(position);
        final Boolean isSwitch = recyclerAdapter.isSwitch(position);
        final String switch_num = recyclerAdapter.getSwitchNumber(position);
        final String slaveID = recyclerAdapter.getSlaveIDForSwitch(position);
        //Log.e("isSwitch or :",""+isSwitch);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(switch_name);

        if (isSwitch) {
            final CharSequence[] items = {"Rename", "Change Icon", "Change to Switch/Dimmer", "Remove"};


            //builder.setCancelable(false);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    switch (item) {
                        case 0:
                            showRenameDeviceDialog(groupID, switchID, switch_name, position);
                            break;
                        case 1:
                            showChangeSwitchIconDialog(groupID, switchID, switch_name, slaveID);
                            break;
                        case 2:
                            changeTypefrom(groupID, switchID, position, isSwitch, switch_num, slaveID);
                            break;
                        case 3:
                            showRemoveItemFromGroupDialog(groupID, switchID, position);
                            break;
                    }
                }
            });
        } else {
            final CharSequence[] items = {"Rename", "Change to Switch/Dimmer", "Remove"};


            //builder.setCancelable(false);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    switch (item) {
                        case 0:
                            showRenameDeviceDialog(groupID, switchID, switch_name, position);
                            break;
                        case 1:
                            changeTypefrom(groupID, switchID, position, isSwitch, switch_num, slaveID);
                            break;
                        case 2:
                            showRemoveItemFromGroupDialog(groupID, switchID, position);
                            break;
                    }
                }
            });
        }
        builder.show();
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

                recyclerAdapter.setViewLoading(position, true, command);
                if (SmartNode.slavesInLocal.contains(slaveID)) {
                    new SendUDP(command).execute();
                }
                else {
                    new SendMQTT(databaseHandler.getSlaveTopic(slaveID) + AppConstant.MQTT_PUBLISH_TOPIC,command).execute();
                }
            } else {
                C.Toast(getApplicationContext(), getString(R.string.nointernet));
            }
        } catch (Exception e) {
            //Log.e("Exception", e.getMessage());
        }
    }

    private void showChangeSwitchIconDialog(final int groupid, final int switchID, String switch_name, final String slaveID) {

        final int[] switch_icon = {1};
        final GridView iconsView=new GridView(this);
        iconsView.setSelector(R.drawable.icon_selector_bg);
        final CustomSwitchIconAdapter iconAdapter=new CustomSwitchIconAdapter(getApplicationContext(),databaseHandler.getAllSwitchIconData());
        iconsView.setAdapter(iconAdapter);
        iconsView.setNumColumns(2);
        iconsView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                iconAdapter.setIconChecked(position);
                iconsView.setItemChecked(position,true);
                switch_icon[0] =iconAdapter.getSwitchIconId(position);
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

                        databaseHandler.changeSwitchIcon(groupid,switchID,switch_icon[0]);
                        recyclerAdapter.notifyIconChanged(slaveID);
                        String commandInCache = SmartNode.slaveCommands.get(slaveID);
                        if (commandInCache != null && !commandInCache.isEmpty()) {
                            try {
                                JSONArray array = new JSONArray(commandInCache);
                                for (int i = 0; i < array.length(); i++) {
                                    handleCommands(array.get(i).toString());
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
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

        final TextView txt_switch=(TextView) dialogView.findViewById(R.id.txt_slave_name);
        final EditText edt_device_name=(EditText) dialogView.findViewById(R.id.edt_device_name);
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
                        if(edt_device_name.getText().toString().trim().equalsIgnoreCase("")){
                            Toast.makeText(FavouriteActivity.this, "Please enter switch name", Toast.LENGTH_SHORT).show();
                        }
                        else if(databaseHandler.isSameSlaveName(edt_device_name.getText().toString().trim())){
                            Toast.makeText(FavouriteActivity.this, "switch name already exists\nplease enter different switch name", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            databaseHandler.renameSwitch(group_id,switch_id,edt_device_name.getText().toString().trim());
                            databaseHandler.renameSwitchInScenes((Bean_Switch) recyclerAdapter.getItem(position), edt_device_name.getText().toString());
                            Toast.makeText(FavouriteActivity.this,"Switch renamed successfully",Toast.LENGTH_SHORT).show();

                            recyclerAdapter.notifyIconChanged();
                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    private void showRemoveItemFromGroupDialog(int groupID, final int switchID, final int position) {

        AlertDialog.Builder confirmDelete=new AlertDialog.Builder(this);
        confirmDelete.setTitle("Confirm to remove");
        confirmDelete.setMessage("Are you sure to remove ?");
        confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                databaseHandler.removeSwitchFromGroup(switchID);
                databaseHandler.removeSwitchFromScenes((Bean_Switch) recyclerAdapter.getItem(position));
                Toast.makeText(getApplicationContext(), "Switch removed successfully", Toast.LENGTH_SHORT).show();
                recyclerAdapter.notifyIconChanged();
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

    @Override
    public void sendUDPCommand(String command) {
        new SendUDP(command).execute();
    }

    private class WatchForLiveStatus extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (netcheck.isOnline()) {
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (netcheck.isOnline()) {

                int commandNotInCache = 0;

                try {

                    for (int i = 0; i < slaveIds.size(); i++) {

                        //String slaveIPAddress = databaseHandler.getMasterIPBySlaveID(slaveIds.get(i));
                        if (SmartNode.slavesInLocal.contains(slaveIds.get(i))) {
                            CHECK_STS_AGAIN_TIME_INTERVEL[0] = 2000;

                            String commandInCache = SmartNode.slaveCommands.get(slaveIds.get(i));
                            //Log.e("send from","UDP");
                            if (commandInCache != null && !commandInCache.isEmpty()) {
                                setCommandsFromCache(commandInCache, slaveIds.get(i));
                            } else {
                                //dialog.show();
                                commandNotInCache++;
                                sendSTSCommand(true, slaveIds.get(i));
                            }
                        } else {
                            CHECK_STS_AGAIN_TIME_INTERVEL[0] = 5000;
                            String commandInCache = SmartNode.slaveCommands.get(slaveIds.get(i));
                            if (commandInCache != null && !commandInCache.isEmpty()) {
                                setCommandsFromCache(commandInCache, slaveIds.get(i));

                                if (!SmartNode.slavesWorking.contains(slaveIds.get(i))) {
                                    sendSTSCommand(false, slaveIds.get(i));
                                }

                            } else {
                                //dialog.show();

                                if (!SmartNode.slavesWorking.contains(slaveIds.get(i))) {
                                    recyclerAdapter.setAllSwitchesOffline(slaveIds.get(i));
                                }
                                commandNotInCache++;
                                sendSTSCommand(false, slaveIds.get(i));
                            }
                        }
                    }


                } catch (Exception e) {
                    //Log.e("Window", "Leaked");
                }

                liveStatusHandler = new Handler();
                liveStatusRunnable = new Runnable() {
                    @Override
                    public void run() {

                        int sendCommandAgain = 0;
                        if (slaveSTSReceived.size() == slaveIds.size()) {
                            liveStatusHandler.removeCallbacks(liveStatusRunnable);
                        } else {
                            for (int i = 0; i < slaveIds.size(); i++) {

                                if (!slaveSTSReceived.contains(slaveIds.get(i))) {
                                    if (SmartNode.slavesInLocal.contains(slaveIds.get(i))) {
                                        CHECK_STS_AGAIN_TIME_INTERVEL[0] = 2000;

                                        String commandInCache = SmartNode.slaveCommands.get(slaveIds.get(i));
                                        if (commandInCache != null && !commandInCache.isEmpty()) {
                                            setCommandsFromCache(commandInCache, slaveIds.get(i));
                                        } else {
                                            sendSTSCommand(true, slaveIds.get(i));
                                            sendCommandAgain++;
                                        }
                                    } else {
                                        //Log.e("send from","MQTT");

                                        CHECK_STS_AGAIN_TIME_INTERVEL[0] = 5000;
                                        String commandInCache = SmartNode.slaveCommands.get(slaveIds.get(i));
                                        if (commandInCache != null && !commandInCache.isEmpty()) {
                                            setCommandsFromCache(commandInCache, slaveIds.get(i));
                                        } else {
                                            recyclerAdapter.setAllSwitchesOffline(slaveIds.get(i));
                                            liveStatusHandler.removeCallbacks(liveStatusRunnable);
                                        }
                                    }
                                }

                            }
                            if (sendCommandAgain > 0) {
                                liveStatusHandler.postDelayed(this, CHECK_STS_AGAIN_TIME_INTERVEL[0]);
                            }
                        }
                    }
                };

                if (commandNotInCache > 0) {
                    liveStatusHandler.postDelayed(liveStatusRunnable, CHECK_STS_AGAIN_TIME_INTERVEL[0]);
                }
            } else {
                C.Toast(getApplicationContext(), "No internet connection found\nplease try again later");
            }
        }
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

                //Log.e("Command Fired :", command);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setRetained(false);
                mqttClient.publish(topic, mqttMessage);
                ////Log.e("topic msg", databaseHandler.getSlaveTopic(slaveID) + AppConstant.MQTT_PUBLISH_TOPIC + " " + mqttMessage);
                //mqttClient.disconnect();

            } catch (MqttException e) {
                //Log.e("Exception : ", e.getMessage());
                e.printStackTrace();
            }

            return null;
        }
    }

    public class GetLiveStatus extends AsyncTask<Void,Void,Void> {

        ProgressDialog loadingView;
        String slave_hex_id="";

        String topic="";
        String message="";

        public GetLiveStatus(String slave_hex_id){
            loadingView=new ProgressDialog(FavouriteActivity.this);
            loadingView.setMessage("Please wait...");
            loadingView.setIndeterminate(false);
            loadingView.setCancelable(false);
            //loadingView.show();
            this.slave_hex_id=slave_hex_id;
        }

        public GetLiveStatus(String topic, String message){
            this.topic=topic;
            this.message=message;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void[] params) {
            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, preference.getMqttClientID(), new MemoryPersistence());
                MqttConnectOptions connectOptions=new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);
                String mqttCommand=AppConstant.START_CMD_STATUS_OF_SLAVE+slave_hex_id+AppConstant.CMD_KEY_TOKEN+databaseHandler.getSlaveToken(slave_hex_id)+AppConstant.END_CMD_STATUS_OF_SLAVE;
                MqttMessage mqttMessage=new MqttMessage(mqttCommand.getBytes());
                mqttMessage.setQos(C.QoS);
                mqttMessage.setRetained(false);
                mqttClient.publish(databaseHandler.getSlaveTopic(slave_hex_id)+AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                //Log.e("Mqtt Message","Published for STS 1st");
                mqttClient.disconnect();

            } catch (MqttException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void o) {
            //loadingView.dismiss();
        }
    }

    private class SendUDP extends AsyncTask<Void, Void, String> {
        String message;
        String masterName;
        String type;
        boolean showMaster=true;

        public SendUDP(String message) {
            this.message = message;
            showMaster=true;
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
                    server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                //Log.e("Packet","Sent");
                /*} else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                    //Log.e("Packet","Sent");
                }*/

                socket.disconnect();
                socket.close();
            } catch (SocketException s) {
                //Log.e("Exception", "->" + s.getLocalizedMessage());
                preference.setOnline(true);
            } catch (IOException e) {
                //Log.e("Exception", "->" + e.getLocalizedMessage());
                preference.setOnline(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String text) {
            super.onPostExecute(text);
        }
    }


}
