package com.nivida.smartnode;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.adapter.SwitchItemScheduleAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddMasterService;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SchedulingSwitchActivity extends AppCompatActivity {

    DatabaseHandler databaseHandler;
    Typeface typeface_raleway;

    MqttClient mqttClient;
    String clientID=MqttClient.generateClientId();

    GridView switchItemGrid;

    Toolbar toolbar;
    TextView txt_smartnode,txt_1,txt_2,txt_slave;
    Button btn_addslave;
    ImageView img_add,img_home;
    Typeface tf;

    List<String> slaveList=new ArrayList<>();

    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddMasterService";
    String subscribedMessage="";
    BroadcastReceiver receiver;

    SwitchItemScheduleAdapter scheduleAdapter;

    int groupid=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduling_switch);
        databaseHandler=new DatabaseHandler(getApplicationContext());

        Intent intent=getIntent();
        groupid=intent.getIntExtra("group_id",0);
        startAddMasterService();
        startRecevier();

        slaveList=databaseHandler.getSlaveHexIdsForGroup(groupid);

        for(int i=0; i<slaveList.size(); i++){
            new GetDateOnServer(slaveList.get(i)).execute();
        }

        typeface_raleway= Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        txt_smartnode=(TextView) findViewById(R.id.txt_actionBarTitle);
        txt_smartnode.setTypeface(typeface_raleway);
        txt_smartnode.setText(databaseHandler.getGroupnameById(groupid));
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

    private void fetchID() {
        switchItemGrid=(GridView) findViewById(R.id.switchItemList);
        scheduleAdapter=new SwitchItemScheduleAdapter(getApplicationContext(),databaseHandler.getAllSwitchesByGroupId(groupid));
        switchItemGrid.setAdapter(scheduleAdapter);

        switchItemGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int switchID=scheduleAdapter.getSwitchIDAtPosition(position);
                String switchName=scheduleAdapter.getSwitchNameAtPosition(position);

                Intent intent=new Intent(getApplicationContext(),SetScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("switchID",switchID);
                intent.putExtra("switchName",switchName);
                intent.putExtra("group_id",groupid);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(getApplicationContext(),SchedulingActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    public void startRecevier(){
        receiver=new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle=intent.getExtras();
                String master_name="";
                if(bundle!=null){

                    subscribedMessage=bundle.getString(AddMasterService.MESSAGETOSEND);



                    if(subscribedMessage==null || subscribedMessage.equalsIgnoreCase("")){
                        //Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                        master_name="No Master Found";
                    }
                    else {
                        Log.e("Command from Scheduling",subscribedMessage);
                        try{
                            JSONObject jsonSCH=new JSONObject(subscribedMessage);
                            String cmd=jsonSCH.getString("cmd");
                            if(cmd.equalsIgnoreCase("SCH")){
                                String[] data=jsonSCH.getString("data").split("-");

                                String tag=data[0];

                                if(tag.equalsIgnoreCase("N")){
                                   String time=data[1];
                                    String day=data[2];
                                    String date=data[3];

                                    String mobileHour = new SimpleDateFormat("HH",Locale.getDefault()).format(new Date());
                                    String mobileMinute=new SimpleDateFormat("mm",Locale.getDefault()).format(new Date());
                                    String mobileDate=new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                                    String hour=time.split(":")[0];
                                    String minute=time.split(":")[1];

                                    if(hour.equalsIgnoreCase(mobileHour) && minute.equalsIgnoreCase(mobileMinute) && date.equalsIgnoreCase(mobileDate)){
                                        Log.e("Date Time","IS CORRECT");
                                    }
                                    else {
                                        new SetNewDateOnServer(jsonSCH.getString("slave")).execute();
                                    }
                                }

                            }

                        } catch (JSONException e) {
                            Log.e("JSON Message : ",e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    private void startAddMasterService() {
        if(!serviceIsRunning()){
            final Intent intent=new Intent(this, AddMasterService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startAddMasterService();
        registerReceiver(receiver,new IntentFilter(AddMasterService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        final Intent intent=new Intent(this, AddMasterService.class);
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


    private class SetNewDateOnServer extends AsyncTask<Void, Void, Void> {

        String slaveId;

        public SetNewDateOnServer(String slaveId){
            this.slaveId=slaveId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Calendar calendar=Calendar.getInstance();
            int day=calendar.get(Calendar.DAY_OF_WEEK);
            String currentTime=new SimpleDateFormat("HH:mm:ss").format(new Date());
            String currentDate=new SimpleDateFormat("dd/MM/yy").format(new Date());

            String command= AppConstant.START_CMD_SCH_GET + slaveId + AppConstant.CENETER_CMD_SCHEDULE ;

            command += "T-"+ currentTime + "-" + day + "-" + currentDate + AppConstant.END_CMD_SCH_GET;

            if(NetworkUtility.isOnline(getApplicationContext())){
                try {
                    mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientID,new MemoryPersistence());
                    MqttConnectOptions connectOptions=new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                    MqttMessage mqttMessage=new MqttMessage(command.getBytes());
                    mqttMessage.setRetained(true);
                    mqttMessage.setQos(1);
                    mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    private class GetDateOnServer extends AsyncTask<Void, Void, Void> {

        String slaveId;

        public GetDateOnServer(String slaveId){
            this.slaveId=slaveId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            String command= AppConstant.START_CMD_SCH_GET + slaveId + AppConstant.CENETER_CMD_SCHEDULE ;

            command += "N" + AppConstant.END_CMD_SCH_GET;

            if(NetworkUtility.isOnline(getApplicationContext())){
                try {
                    mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientID,new MemoryPersistence());
                    MqttConnectOptions connectOptions=new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                    MqttMessage mqttMessage=new MqttMessage(command.getBytes());
                    mqttMessage.setRetained(true);
                    mqttMessage.setQos(1);
                    mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}
