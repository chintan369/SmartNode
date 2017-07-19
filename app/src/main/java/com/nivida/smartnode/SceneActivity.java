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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.adapter.SceneAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_Scenes;
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
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class SceneActivity extends AppCompatActivity {

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    public List<Bean_Scenes> scenesList=new ArrayList<>();
    public SceneAdapter sceneAdapter;
    ImageView img_add, img_home;
    int serialID=0;
    MqttClient mqttClient;
    String clientId=MqttClient.generateClientId();
    String subscribedMessage="";
    String UDPMessage="";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue=false;
    NetworkUtility netcheck;
    PublishMessage publishMessage;
    Handler handler;
    Runnable runnable;
    ProgressDialog dialog;
    int fireTime=0;
    int groupid=0;
    private Toolbar toolbar;
    private TextView actionBarTitle;
    private GridView sceneGrid;
    private Typeface typeface_raleway;
    private List<Bean_Switch> switchList = new ArrayList<>();
    private ArrayList<String> updateCommands = new ArrayList<>();
    private DatabaseHandler dbhandler;
    private AppPreference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene);
        dbhandler=new DatabaseHandler(getApplicationContext());
        netcheck=new NetworkUtility(this);
        preference=new AppPreference(getApplicationContext());
        dialog=new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setMessage("Please Wait while Activating Scene...");
        typeface_raleway=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (handler != null && runnable != null) {
                        handler.removeCallbacks(runnable);
                    }
                    dialog.dismiss();

                }
                return false;
            }
        });

        //startAddSwitchService();
        startReceiver();

        Intent intent=getIntent();
        groupid=intent.getIntExtra("group_id",0);

        switchList=dbhandler.getAllSwitchesByGroupId(groupid);
        //publishMessage=new PublishMessage();

        fetchIDs();
    }

    private void startReceiver() {
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle=intent.getExtras();
                String device_id="";
                String device_name="";
                if(bundle!=null){
                    subscribedMessage=bundle.getString(AddDeviceService.MESSAGETOSEND);
                    UDPMessage=bundle.getString(UDPService.MESSAGEJSON);
                    Log.e("JSOn fr Scene Rcvr ", ""+subscribedMessage);

                    try{
                        if(UDPMessage!=null && !UDPMessage.isEmpty()){
                            Log.e("UDPMsg","--");
                            JSONObject object=new JSONObject(UDPMessage);
                            //serialID=object.getInt("serial");
                            //Log.e("UDPSerial","--"+serialID);
                            handleJSONCommand(UDPMessage);
                        }
                        else if(subscribedMessage!=null && !subscribedMessage.isEmpty()){
                            Log.e("MQTTMsg","--");
                            JSONObject object=new JSONObject(subscribedMessage);
                            //serialID=object.getInt("serial");
                            //Log.e("MQTTSerial","--"+serialID);
                            handleJSONCommand(subscribedMessage);
                        }
                    }catch (Exception e){
                        Log.e("Exception ee",e.getMessage());
                    }

                }
            }
        };
    }

    private void handleJSONCommand(String json) {
        try{
            JSONObject object=new JSONObject(json);

            String cmd=object.getString("cmd");

            if (cmd.equals(Cmd.INTERNET)) {
                C.Toast(getApplicationContext(), object.getString("message"));
                return;
            }
            int serialNum=object.getInt("serial");

            /*if(cmd.equalsIgnoreCase(Cmd.SET)){
                fireTime++;
                if(fireTime==switchList.size()){
                    if(dialog.isShowing())
                        dialog.dismiss();
                    C.Toast(getApplicationContext(),"Scene Activated Successfully!");
                    Log.e("fireTime","->"+fireTime);
                    fireTime=0;
                }
            }
            else */
            if(cmd.equals(Cmd.STS) && updateCommands.size()>0){
               fireTime++;
               if(fireTime==dbhandler.getSlaveHexIdsForGroup(groupid).size()){
                   updateCommands.clear();
                   if(dialog.isShowing()){
                       dialog.dismiss();
                   }
                   if(handler!=null){
                       handler.removeCallbacks(runnable);
                   }
                   C.Toast(getApplicationContext(),"Scene Activated Successfully!");
                   fireTime=0;
               }
            }

        }catch (Exception e){
            Log.e("Exception",e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver,new IntentFilter(AddDeviceService.NOTIFICATION));
        Log.e("Reciever :","Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.e("Reciever :","Unregistered");
    }

    private void startAddSwitchService() {
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

    private void fetchIDs() {
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        actionBarTitle=(TextView) findViewById(R.id.txt_title);
        img_add=(ImageView) findViewById(R.id.img_add);
        img_home=(ImageView) findViewById(R.id.img_home);

        img_add.setVisibility(View.VISIBLE);

        actionBarTitle.setText(dbhandler.getGroupnameById(groupid)+" / Scenes");
        //actionBarTitle.setTypeface(typeface_raleway);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        img_home.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });

        img_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddNewSceneDialog();
            }
        });

        sceneGrid=(GridView) findViewById(R.id.sceneGrid);
        sceneAdapter=new SceneAdapter(getApplicationContext(),dbhandler.getScenesList(groupid));
        sceneGrid.setAdapter(sceneAdapter);

        sceneGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int scene_id=sceneAdapter.getSceneID(position);

                dialog.show();
                if(scene_id==1){
                    setAllSwitchesOn();
                    //Set All Switches ON
                }
                else if(scene_id==2){
                    //Set All Switches OFF
                    setAllSwitchesOff();
                }
                else {
                    setSpecificSwitchesOnOff(scene_id);
                    //Do Coding As Set By User
                }
            }
        });

        sceneGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {

                final int scene_id=sceneAdapter.getSceneID(position);

                if(scene_id==1){
                    //Set All Switches ON
                    return false;
                }
                else if(scene_id==2){
                    //Set All Switches OFF
                    return false;
                }
                else {
                    //Show Popup to Rename or Delete or Edit
                    PopupMenu popupMenu=new PopupMenu(SceneActivity.this,view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_edit_scene,popupMenu.getMenu());


                    popupMenu.setGravity(Gravity.CENTER);

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {

                            switch (item.getItemId()){
                                case R.id.rename:
                                    showRenameSceneDialog(scene_id,position);
                                    break;
                                case R.id.delete:
                                    showDeleteSceneDialog(scene_id,position);
                                    break;
                                case R.id.edit:
                                    Intent intent=new Intent(SceneActivity.this,SceneEditActivity.class);
                                    intent.putExtra("group_id",groupid);
                                    intent.putExtra("scene_id",scene_id);
                                    Log.e("Scene_id",""+scene_id);
                                    startActivity(intent);
                                    finish();
                                    break;
                            }

                            return true;
                        }
                    });

                    popupMenu.show();
                }

                return true;
            }
        });

    }

    private void setAllSwitchesOn() {
        generateCommandsForUpdate(true);
    }

    private void setAllSwitchesOff() {
        generateCommandsForUpdate(false);
    }

    private void setSpecificSwitchesOnOff(int scene_id){
        generateCommandsForUpdateSpecific(scene_id);
    }

    private void generateCommandsForUpdateSpecific(int scene_id) {

        List<Bean_Switch> switchList=dbhandler.getSceneSwitches(scene_id,groupid);
        updateCommands.clear();

        final ArrayList<String> slaveIDsInGrp = new ArrayList<>();
        for(int i=0; i<switchList.size(); i++){
            if(!slaveIDsInGrp.contains(switchList.get(i).getSwitchInSlave())){
                slaveIDsInGrp.add(switchList.get(i).getSwitchInSlave());
            }
        }

        for(int i=0; i<slaveIDsInGrp.size(); i++) {
            final JSONObject object = new JSONObject();
            try {
                object.put("cmd",Cmd.SCENE);
                object.put("slave",slaveIDsInGrp.get(i));

                int totalSwitchesInSlave=dbhandler.getTotalSwitchInSlave(slaveIDsInGrp.get(i));
                String data="";

                for(int j=0; j<totalSwitchesInSlave; j++){

                    boolean switchFound=false;

                    for(int k=0; k<switchList.size(); k++){
                        String currentSwitchNum= (j+1)<10 ? "0"+(j+1) : String.valueOf(j+1);
                        String switchNum=switchList.get(k).getSwitch_btn_num();
                        Log.e("CurrentSwitch",currentSwitchNum+" -- "+switchNum);


                        if(currentSwitchNum.equals(switchNum) && switchList.get(k).getSwitchInSlave().equals(slaveIDsInGrp.get(i))){
                            if(dbhandler.getSlaveUserType(slaveIDsInGrp.get(i)).equals(Cmd.LIN) ||
                                    (dbhandler.getSlaveUserType(slaveIDsInGrp.get(i)).equals(Cmd.ULN) && !dbhandler.switchHasUserLock(switchNum,slaveIDsInGrp.get(i)))){
                                data += switchList.get(k).getIsSwitchOn()==1  ? "A": "0";
                                data += switchList.get(k).getIsSwitch().equalsIgnoreCase("s") ? "X" : String.valueOf(switchList.get(k).getDimmerValue());
                                switchFound=true;
                            }
                            break;
                        }
                    }

                    if(!switchFound) data += "XX";
                }

                object.put("data",data);
                object.put("token",dbhandler.getSlaveToken(slaveIDsInGrp.get(i)));

                Log.e("SCommand",object.toString()+"\n");

                updateCommands.add(object.toString());

                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(i))) {
                    new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else {
                    new PublishMessage(object.toString(), slaveIDsInGrp.get(i)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                handler = new Handler();
                final int finalI = i;
                final int DELAY_TIME[] = {7000};
                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(i))) DELAY_TIME[0] = 500;
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isShowing()) {
                            boolean hasToGo = true;
                            if (!SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(finalI)) && SmartNode.slavesWorking.contains(slaveIDsInGrp.get(finalI))) {
                                hasToGo = false;
                                dialog.dismiss();
                                C.Toast(getApplicationContext(), "Device is Offline!\nPlease Try Again Later!");
                                handler.removeCallbacks(runnable);
                            }
                            if (hasToGo) {
                                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(finalI))) {
                                    new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                } else {
                                    new PublishMessage(object.toString(), slaveIDsInGrp.get(finalI)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                                handler.postDelayed(this, DELAY_TIME[0]);
                            }
                        } else {
                            handler.removeCallbacks(runnable);
                        }
                    }
                };
                handler.postDelayed(runnable, DELAY_TIME[0]);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateCommandsForUpdate(boolean onOff) {
        updateCommands.clear();

        final ArrayList<String> slaveIDsInGrp = new ArrayList<>();

        for(int i=0; i<switchList.size(); i++){
            if(!slaveIDsInGrp.contains(switchList.get(i).getSwitchInSlave())){
                slaveIDsInGrp.add(switchList.get(i).getSwitchInSlave());
            }
        }

        for(int i=0; i<slaveIDsInGrp.size(); i++){
            final JSONObject object = new JSONObject();

            try {
                object.put("cmd",Cmd.SCENE);
                object.put("slave",slaveIDsInGrp.get(i));

                int totalSwitchesInSlave=dbhandler.getTotalSwitchInSlave(slaveIDsInGrp.get(i));
                String data="";

                for(int j=0; j<totalSwitchesInSlave; j++){

                    boolean switchFound=false;

                    for(int k=0; k<switchList.size(); k++){
                        String currentSwitchNum= (j+1)<10 ? "0"+(j+1) : String.valueOf(j+1);
                        String switchNum=switchList.get(k).getSwitch_btn_num();
                        Log.e("CurrentSwitch",currentSwitchNum+" -- "+switchNum);


                        if(currentSwitchNum.equals(switchNum) && switchList.get(k).getSwitchInSlave().equals(slaveIDsInGrp.get(i))){
                            if(dbhandler.getSlaveUserType(slaveIDsInGrp.get(i)).equals(Cmd.LIN) ||
                                    (dbhandler.getSlaveUserType(slaveIDsInGrp.get(i)).equals(Cmd.ULN) && !dbhandler.switchHasUserLock(switchNum,slaveIDsInGrp.get(i)))){
                                data += onOff ? "A": "0";
                                data += switchList.get(k).getIsSwitch().equalsIgnoreCase("s") ? "X" : " ";
                                switchFound=true;
                            }
                            break;
                        }
                    }

                    if(!switchFound) data += "XX";
                }

                object.put("data",data);
                object.put("token",dbhandler.getSlaveToken(slaveIDsInGrp.get(i)));

                Log.e("SCommand",object.toString()+"\n");

                updateCommands.add(object.toString());

                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(i))) {
                    new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
                else {
                    new PublishMessage(object.toString(), slaveIDsInGrp.get(i)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }

                handler = new Handler();
                final int finalI = i;
                final int DELAY_TIME[] = {7000};
                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(i))) DELAY_TIME[0] = 500;
                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (dialog.isShowing()) {
                            boolean hasToGo = true;
                            if (!SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(finalI)) && SmartNode.slavesWorking.contains(slaveIDsInGrp.get(finalI))) {
                                hasToGo = false;
                                SmartNode.slavesWorking.remove(slaveIDsInGrp.get(finalI));
                                dialog.dismiss();
                                C.Toast(getApplicationContext(), "Device is Offline!\nPlease Try Again Later!");
                                handler.removeCallbacks(runnable);
                            }
                            if (hasToGo) {
                                if (SmartNode.slavesInLocal.contains(slaveIDsInGrp.get(finalI))) {
                                    new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                } else {
                                    new PublishMessage(object.toString(), slaveIDsInGrp.get(finalI)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                                }
                                handler.postDelayed(this, DELAY_TIME[0]);
                            }
                        }
                        else {
                            handler.removeCallbacks(runnable);
                        }
                    }
                };
                handler.postDelayed(runnable, DELAY_TIME[0]);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void showDeleteSceneDialog(final int scene_id, int position) {
        AlertDialog.Builder confirmDelete=new AlertDialog.Builder(SceneActivity.this);
        confirmDelete.setTitle("Confirm to Delete");
        confirmDelete.setMessage("Are you sure to delete this scene ?");
        confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dbhandler.deleteScene(scene_id);
                Toast.makeText(SceneActivity.this, "Scene deleted successfully", Toast.LENGTH_SHORT).show();
                sceneAdapter.newDataAdded(groupid);
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

    private void showAddNewSceneDialog() {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_addnewscene, null);
        dialogBuilder.setView(dialogView);

        final TextView txt_scene=(TextView) dialogView.findViewById(R.id.txt_scene_name);
        final EditText edt_scene_name=(EditText) dialogView.findViewById(R.id.edt_scene_name);


        dialogBuilder.setTitle("Add New Scene");
        dialogBuilder.setPositiveButton("Add Scene", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();
        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if(edt_scene_name.getText().toString().trim().equals("")){
                            Toast.makeText(getApplicationContext(), "Please Enter Scene name", Toast.LENGTH_SHORT).show();
                        }
                        else if(dbhandler.isSameSceneName(edt_scene_name.getText().toString(),groupid)){
                            Toast.makeText(getApplicationContext(), "Scene name you enetered is already exist", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Bean_Scenes scenes=new Bean_Scenes();
                            scenes.setSceneName(edt_scene_name.getText().toString().trim());
                            scenes.setSceneGroup(groupid);

                            boolean isAdded=dbhandler.addSceneItem(scenes);

                            /*if(isAdded){*/

                                C.Toast(getApplicationContext(), "Scene Created Successfully");
                                sceneAdapter.newDataAdded(groupid);
                                b.dismiss();
                                Intent intent=new Intent(SceneActivity.this,SceneEditActivity.class);
                                intent.putExtra("group_id",groupid);
                                intent.putExtra("scene_id",dbhandler.getLastAddedSceneID());
                                Log.e("Scene_id",""+dbhandler.getLastAddedSceneID());
                                startActivity(intent);
                                finish();
                            /*}
                            else {
                                C.Toast(getApplicationContext(),"");
                            }*/

                        }
                    }
                });
            }
        });
        b.show();
    }

    public void showRenameSceneDialog(final int scene_id, int position){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_addnewscene, null);
        dialogBuilder.setView(dialogView);

        final TextView txt_scene=(TextView) dialogView.findViewById(R.id.txt_scene_name);
        final EditText edt_scene_name=(EditText) dialogView.findViewById(R.id.edt_scene_name);

        edt_scene_name.setText(sceneAdapter.getSceneName(position));

        dialogBuilder.setTitle("Rename Scene");
        dialogBuilder.setPositiveButton("Rename", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();
        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                final Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if(edt_scene_name.getText().toString().trim().equals("")){
                            Toast.makeText(getApplicationContext(), "Please enter scene name", Toast.LENGTH_SHORT).show();
                        }
                        else if(dbhandler.isSameSceneName(edt_scene_name.getText().toString(),groupid)){
                            Toast.makeText(getApplicationContext(), "Scene name you enetered is already exist", Toast.LENGTH_SHORT).show();
                        }
                        else {

                            dbhandler.renameSceneItem(scene_id,edt_scene_name.getText().toString().trim());

                            Toast.makeText(SceneActivity.this, "Scene renamed Successfully", Toast.LENGTH_SHORT).show();
                            sceneAdapter.newDataAdded(groupid);
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
        Intent intent=new Intent(getApplicationContext(),GroupSwitchOnOffActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("group_id",groupid);
        startActivity(intent);
        finish();
    }

    public class PublishMessage extends AsyncTask<Void, Void, String>{

        String command="";
        String slaveID="";

         PublishMessage(String command,String slave){
             this.command=command;
             this.slaveID=slave;
            if(netcheck.isOnline()){
                try{

                } catch(Exception e){
                    Toast.makeText(SceneActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(SceneActivity.this,"Internet connection not available\nplease check your connection first",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected String doInBackground(Void... params) {
            if(netcheck.isOnline()){

                try {
                    mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
                    MqttConnectOptions connectOptions = new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                } catch (MqttException e) {
                    e.printStackTrace();
                }

                    MqttMessage mqttMessage=new MqttMessage(command.getBytes());
                    mqttMessage.setQos(0);
                    mqttMessage.setRetained(false);
                try {
                    mqttClient.publish(dbhandler.getSlaveTopic(slaveID)+AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                } catch (MqttException e) {
                    e.printStackTrace();
                    Log.e("Exception",e.getMessage());
                }

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
                DatagramSocket socket = new DatagramSocket();
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
