package com.nivida.smartnode;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.adapter.SwitchScheduleItemAdapter2;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_ScheduleItem;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
import com.nivida.smartnode.services.AddDeviceService;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.nivida.smartnode.GroupSwitchOnOffActivity.UDPSERVICE_CLASSNAME;

public class SetScheduleActivity extends AppCompatActivity implements SwitchScheduleItemAdapter2.OnScheduleViewSelection {

    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    int groupid = 0;
    int switchID = 0;
    String switchName = "";
    Toolbar toolbar;
    TextView txt_smartnode, txt_1, txt_2, txt_slave;
    Button btn_addslave;
    ImageView img_add, img_home;
    Typeface tf;
    boolean forAvailInfo = true;
    boolean forAddingNewItem = true;
    boolean callfromthis = false;
    boolean setList = false;
    boolean now = false;
    String subscribedMessage = "";
    String UDPMessage = "";
    BroadcastReceiver receiver;

    AppPreference preference;
    DatabaseHandler databaseHandler;
    SwitchScheduleItemAdapter2 adapter;
    List<Bean_ScheduleItem> scheduleItemList = new ArrayList<>();

    ListView scheduleList;

    NetworkUtility networkUtility;

    MqttClient mqttClient;
    String clientID = MqttClient.generateClientId();

    View footerViewLoaidng;

    AlertDialog.Builder builder;
    AlertDialog dialog;

    String switchButtonNumber="";
    String slaveHexID="";
    String slaveToken="";

    int positionForDelete = -1;

    ProgressDialog deleteDialog, createSchuduleDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_schedule);

        preference = new AppPreference(getApplicationContext());
        databaseHandler = new DatabaseHandler(getApplicationContext());
        networkUtility = new NetworkUtility(getApplicationContext());

        deleteDialog = new ProgressDialog(this);
        deleteDialog.setCancelable(false);
        deleteDialog.setMessage("Removing Schedule");

        createSchuduleDialog = new ProgressDialog(this);
        createSchuduleDialog.setCancelable(false);
        createSchuduleDialog.setMessage("Creating Schedule");

        builder = new AlertDialog.Builder(this);

        Intent intent = getIntent();
        groupid = intent.getIntExtra("group_id", 0);
        switchID = intent.getIntExtra("switchID", 0);
        switchName = intent.getStringExtra("switchName");
        switchButtonNumber=databaseHandler.getSwitchButtonNum(switchID);
        slaveHexID=databaseHandler.getSlaveHexIDForSwitch(switchID);
        slaveToken=databaseHandler.getSlaveToken(slaveHexID);
        footerViewLoaidng=getLayoutInflater().inflate(R.layout.list_footer_loading,null);

        startServices();
        startRecevier();
        setToolBar();
        fetchID();
        getDateFromServer();


    }

    private void startServices() {
        if (!UDPServiceIsRunning()) {
            Intent intent = new Intent(this, UDPService.class);
            startService(intent);
        }

        if (!serviceIsRunning()) {
            final Intent intent = new Intent(this, AddDeviceService.class);
            //startService(intent);
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

    public void startRecevier() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                String master_name = "";
                if (bundle != null) {


                    subscribedMessage = bundle.getString(AddMasterService.MESSAGETOSEND);
                    UDPMessage = bundle.getString(UDPService.NOTIFICATION);

                    if (UDPMessage != null && !UDPMessage.isEmpty()) {
                        handleJSONCommand(UDPMessage);
                    } else if (subscribedMessage == null || subscribedMessage.equalsIgnoreCase("")) {
                        //Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
                        master_name = "No Master Found";
                    } else {
                        handleJSONCommand(subscribedMessage);
                    }
                }
            }
        };
    }

    private void handleJSONCommand(String json) {
        try {
            JSONObject object = new JSONObject(json);

            String cmd = object.getString("cmd");

            String slave = object.getString("slave");

            if (cmd.equalsIgnoreCase(Cmd.SCH)) {

                String data = object.getString("data");
                String[] dataItems = data.split("-");
                String tag = dataItems[0];

                if (tag.equalsIgnoreCase("A")) {

                    String availableSlots = dataItems[1];

                    if (!forAvailInfo) {
                        boolean isAnyAvail = false;
                        String NN = "26";
                        //To get Information which slot is available for creating schudule
                        for (int i = 0; i < availableSlots.length(); i+=2) {

                            String currentSwitchNumber=String.valueOf(availableSlots.charAt(i))+String.valueOf(availableSlots.charAt(i+1));

                            boolean isAvail = currentSwitchNumber.equals("00");

                            if (isAvail) {
                                NN = String.valueOf(i/2);
                                if ((i/2) < 10)
                                    NN = "0" + NN;
                                isAnyAvail = true;
                                break;
                            }
                        }

                        if (isAnyAvail) {
                            adapter.setAvailableSlotForSlave(slave, NN);
                        } else {
                            adapter.setAvailableSlotForSlave(slave, NN);
                            C.Toast(getApplicationContext(), "No Slot Available To Create Schedule");
                            createSchuduleDialog.dismiss();
                        }
                    } else {

                        List<String> ipList=new IPDb(this).ipList();
                        boolean isOnLAN=ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveHexID));

                        boolean isFoundAny=false;

                        for (int i = 0; i < availableSlots.length(); i+=2) {
                            scheduleList.removeFooterView(footerViewLoaidng);

                            String currentSlotNum=String.valueOf(availableSlots.charAt(i))+String.valueOf(availableSlots.charAt(i+1));

                            if(currentSlotNum.equals(switchButtonNumber)){
                                isFoundAny=true;
                                try {

                                    JSONObject command = new JSONObject();
                                    command.put("cmd", Cmd.SCH);
                                    command.put("slave",slaveHexID);
                                    command.put("token", slaveToken);

                                    String getData = "G-";
                                    if ((i/2) < 10)
                                        getData += "0" + (i/2);
                                    else
                                        getData += (i/2);

                                    command.put("data", getData);

                                    Log.e("Command Get", command.toString());

                                    if (isOnLAN)
                                        new SendUDP(command.toString()).execute();
                                    else
                                        new PublishMessage(command.toString(), slaveHexID).execute();

                                    forAddingNewItem = true;

                                } catch (Exception e) {
                                    Log.e("Exception", e.getMessage());
                                }
                            } else {
                                String slotNumber = "";
                                if ((i / 2) < 10)
                                    slotNumber += "0" + (i / 2);
                                else
                                    slotNumber += (i / 2);

                                if (databaseHandler.isAlreadyScheduleAdded(slaveHexID, slotNumber, switchButtonNumber)) {
                                    adapter.updateScheduleDeleted(slotNumber, slave, -1);
                                }
                            }
                        }

                        if(!isFoundAny){
                            scheduleList.removeFooterView(footerViewLoaidng);
                            adapter.setHasNoSchedule();
                        }
                    }
                } else if (tag.equalsIgnoreCase("I")) {
                    if (forAddingNewItem) {
                        scheduleList.removeFooterView(footerViewLoaidng);
                        if (!dataItems[2].equalsIgnoreCase("EMPTY")) {
                            String time = dataItems[2] + "-" + dataItems[3];
                            String slotNumber = dataItems[1];
                            String days = dataItems[4];
                            String switchNum = dataItems[5];
                            String switchStatus = dataItems[6];
                            String dimmerValue = dataItems[7];
                            String repeatValue = dataItems[8];

                            if (!databaseHandler.isAlreadyScheduleAdded(slave, slotNumber, switchNum)) {
                                Bean_ScheduleItem scheduleItem = new Bean_ScheduleItem();
                                scheduleItem.setScheduleID(databaseHandler.getNewIDForSCH());
                                scheduleItem.setSlave_id(slave);
                                scheduleItem.setSwitchID(switchID);
                                scheduleItem.setTime(time);
                                scheduleItem.setSlot_num(slotNumber);
                                scheduleItem.setSwitch_btn_num(switchNum);
                                scheduleItem.setSwitchOn(switchStatus.equalsIgnoreCase("ON"));
                                scheduleItem.setDimmerValue(dimmerValue);
                                scheduleItem.setRepeat(Integer.parseInt(repeatValue));
                                scheduleItem.setSchEnabled(true);

                                for (int i = 0; i < days.length(); i++) {
                                    scheduleItem.setPerticularDay(i, String.valueOf(days.charAt(i)));
                                }

                                if (scheduleItem.areAllDaysSelected())
                                    scheduleItem.setDaily(true);
                                else if (scheduleItem.getRepeat() == 1)
                                    scheduleItem.setOnce(true);

                                databaseHandler.addScheduleItem(scheduleItem);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } else {
                        if (dataItems[2].equalsIgnoreCase("EMPTY")) {
                            C.Toast(getApplicationContext(),"Schedule Removed Successfully");
                            adapter.updateScheduleDeleted(dataItems[1], slave, positionForDelete);
                            deleteDialog.dismiss();
                            positionForDelete = -1;
                        } else {
                            C.Toast(getApplicationContext(), "Schedule created Succeddfully");
                            adapter.setScheduleCreated(slave, dataItems[5], dataItems[1]);
                            createSchuduleDialog.dismiss();
                        }
                    }
                } else if (tag.equalsIgnoreCase("N")) {
                    String time = dataItems[1];
                    String day = dataItems[2];
                    String date = dataItems[3];

                    String mobileHour = new SimpleDateFormat("HH", Locale.getDefault()).format(new Date());
                    String mobileMinute = new SimpleDateFormat("mm", Locale.getDefault()).format(new Date());
                    String mobileDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                    String hour = time.split(":")[0];
                    String minute = time.split(":")[1];

                    if (hour.equalsIgnoreCase(mobileHour) && minute.equalsIgnoreCase(mobileMinute) && date.equalsIgnoreCase(mobileDate)) {
                        Log.e("Date Time", "IS CORRECT");
                    } else {
                        setDeviceTimeOnServer(mobileHour, mobileMinute, mobileDate, slave);
                    }
                }
            }

        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void setDeviceTimeOnServer(String mobileHour, String mobileMinute, final String mobileDate, final String slave) {

        if(dialog==null || !dialog.isShowing()){
            builder.setTitle("Change Device Time");
            builder.setMessage("Your device time is inaccurate. Do you want to change it as in your mobile ?");
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        JSONObject object = new JSONObject();
                        object.put("cmd", Cmd.SCH);
                        object.put("slave", slaveHexID);
                        object.put("token", slaveToken);

                        String data = "T-";
                        data += new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()) + "-";
                        data += Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + "-";
                        data += new SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(new Date());

                        object.put("data", data);

                        String command = object.toString();
                        Log.e("command", command);

                        List<String> ipList=new IPDb(getApplicationContext()).ipList();

                        if (ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveHexID))) {
                            new SendUDP(command).execute();
                        } else {
                            new PublishMessage(command, slaveHexID).execute();
                        }
                    } catch (Exception e) {
                        Log.e("Exception", e.getMessage());
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            dialog = builder.create();
            dialog.show();
        }


    }

    private void handleCommands(String json) {
        final int[] countUnAvail = {0};
        try {
            JSONObject jsonSCH = new JSONObject(json);
            String cmd = jsonSCH.getString("cmd");
            if (cmd.equalsIgnoreCase("SCH")) {
                String[] data = jsonSCH.getString("data").split("-");

                String tag = data[0];

                if (tag.equalsIgnoreCase("A")) {
                    String availables = data[1];
                    if (callfromthis) {
                                        /*for (int j = 0; j < scheduleItemList.size(); j++) {
                                            int slot_num = Integer.parseInt(scheduleItemList.get(j).getSlot_num());
                                            for (int i = 0; i < availables.length(); i++) {
                                                String isAvail = String.valueOf(availables.charAt(i));
                                                if (slot_num != 26 && slot_num == i) {
                                                    if (isAvail.equalsIgnoreCase("N")) {
                                                        scheduleItemList.get(j).setSchEnabled(false);
                                                        if (countUnAvail[0] == 0) {
                                                            Toast.makeText(getApplicationContext(), "Unable to set Schedule due to no slot available", Toast.LENGTH_SHORT).show();
                                                            adapter.notifyDataSetChanged();
                                                            countUnAvail[0] = 1;
                                                            Log.e("countUnAVail", "count to");
                                                        } else if (countUnAvail[0] == 1) {
                                                            countUnAvail[0] = 0;
                                                        }
                                                    }
                                                }
                                            }
                                        }*/

                        for (int i = 0; i < availables.length(); i++) {
                            String isAvail = String.valueOf(availables.charAt(i));
                            if (isAvail.equalsIgnoreCase("Y")) {
                                String NN = "00";

                                if (i < 10) {
                                    NN = "0" + i;
                                } else {
                                    NN = String.valueOf(i);
                                }

                                String command = "{\"cmd\":\"SCH\",\"slave\":\"" + databaseHandler.getSlaveHexIDForSwitch(switchID) + "\",\"data\":\"G-" + NN + "\"}";

                                new PublishMessage(command, databaseHandler.getSlaveHexIDForSwitch(switchID)).execute();
                            }

                            if (i == availables.length() - 1)
                                now = true;
                        }

                        if (now)
                            callfromthis = false;
                    } else {
                        int isAny = 0;
                        for (int i = 0; i < availables.length(); i++) {
                            String isAvail = String.valueOf(availables.charAt(i));
                            if (isAvail.equalsIgnoreCase("N")) {
                                isAny = 1;
                                adapter.setAvailableSlotForSlave(jsonSCH.getString("slave"), String.valueOf(i));
                                break;
                            }

                        }
                        if (isAny == 0) {
                            adapter.setAvailableSlotForSlave(jsonSCH.getString("slave"), String.valueOf(26));
                        }
                    }
                } else if (tag.equalsIgnoreCase("I")) {
                    if (setList) {
                        if (!data[2].equalsIgnoreCase("EMPTY")) {
                            String switch_button = data[5];
                            if (switch_button.equalsIgnoreCase(databaseHandler.getSwitchButtonNum(switchID))) {
                                Bean_ScheduleItem scheduleItem = new Bean_ScheduleItem();

                                scheduleItem.setSlot_num(data[1]);
                                scheduleItem.setTime(data[2] + "-" + data[3]);
                                String[] days = new String[data[4].length()];

                                for (int i = 0; i < data[4].length(); i++) {
                                    days[i] = String.valueOf(data[4].charAt(i));
                                }

                                scheduleItem.setDays(days);

                                scheduleItem.setSchEnabled(true);
                                scheduleItem.setSwitch_btn_num(data[5]);
                                scheduleItem.setSlave_id(jsonSCH.getString(""));
                                scheduleItem.setDimmerValue(data[7]);
                                scheduleItem.setRepeat(Integer.parseInt(data[8]));
                                scheduleItem.setRepeated(true);


                                if (data[6].equalsIgnoreCase("ON"))
                                    scheduleItem.setSwitchOn(true);
                                else
                                    scheduleItem.setSwitchOn(false);

                                scheduleItemList.add(scheduleItem);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    } else {
                        if (data[2].equalsIgnoreCase("EMPTY")) {
                            if (countUnAvail[0] == 0) {
                                Toast.makeText(getApplicationContext(), "Unable to set Schedule due to no slot available", Toast.LENGTH_SHORT).show();
                                adapter.notifyDataSetChanged();
                                countUnAvail[0] = 1;
                                Log.e("countUnAVail", "count from");
                            } else if (countUnAvail[0] == 1) {
                                countUnAvail[0] = 0;
                            }

                            //adapter.deleteItem(data[1]);
                        } else {
                            adapter.setSlotNumber(data[1]);
                        }
                    }


                }


            }

        } catch (JSONException e) {
            Log.e("JSON Message : ", e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startServices();
        registerReceiver(receiver, new IntentFilter(AddDeviceService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private void getDateFromServer() {
        try {
            String slaveID = databaseHandler.getSlaveHexIDForSwitch(switchID);
            JSONObject object = new JSONObject();
            object.put("cmd", Cmd.SCH);
            object.put("slave", slaveID);
            object.put("data", "N");
            object.put("token", databaseHandler.getSlaveToken(slaveID));

            String command = object.toString();
            Log.e("command", command);

            List<String> ipList=new IPDb(this).ipList();

            if (NetworkUtility.isOnline(getApplicationContext())) {
                if (ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveHexID))) {
                    new SendUDP(command).execute();
                } else {
                    new PublishMessage(command, slaveID).execute();
                }
            } else {
                C.Toast(getApplicationContext(), "Failed To get Device Time due to no Internet Connection");
            }
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
    }


    private void fetchID() {
        scheduleList = (ListView) findViewById(R.id.scheduleList);
        scheduleItemList = databaseHandler.getAllSchedulesForSwitch(switchID);
        adapter = new SwitchScheduleItemAdapter2(this, scheduleItemList, this, switchID);

        scheduleList.addFooterView(footerViewLoaidng);

        String slave_id = databaseHandler.getSlaveHexIDForSwitch(switchID);
        Log.e("slave_id", switchID + "->" + slave_id);

        if (!slave_id.isEmpty() && !slave_id.equalsIgnoreCase("0")) {
            try {
                JSONObject object = new JSONObject();
                object.put("cmd", Cmd.SCH);
                object.put("slave", slave_id);
                object.put("data", "ALL");
                object.put("token", databaseHandler.getSlaveToken(slave_id));

                String command = object.toString();

                if (preference.isOnline() || (!preference.isOnline() && !preference.getCurrentIPAddr().equalsIgnoreCase(databaseHandler.getMasterIPBySlaveID(slave_id)))) {
                    new PublishMessage(command, slave_id).execute();
                } else {
                    new SendUDP(command).execute();
                }
            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }

        Log.e("Schedule Items", "" + scheduleItemList.size());
        scheduleList.setAdapter(adapter);
    }

    private void setToolBar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txt_smartnode = (TextView) findViewById(R.id.txt_toolbar_title);
        img_add = (ImageView) findViewById(R.id.img_add);
        img_home = (ImageView) findViewById(R.id.img_home);

        img_home.setVisibility(View.GONE);

        img_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bean_ScheduleItem scheduleItem = new Bean_ScheduleItem();
                Bean_Switch beanSwitch = databaseHandler.getSingleSwitchesByGroupId(switchID);

                scheduleItem.setScheduleID(databaseHandler.getNewIDForSCH());
                scheduleItem.setSwitchID(beanSwitch.getSwitch_id());
                scheduleItem.setOnce(true);
                scheduleItem.setSwitch_btn_num(beanSwitch.getSwitch_btn_num());
                scheduleItem.setTime("");
                scheduleItem.setSlave_id(beanSwitch.getSwitchInSlave());

                databaseHandler.addScheduleItem(scheduleItem);

                scheduleItemList.add(scheduleItem);
                adapter.notifyDataSetChanged();

            }
        });

        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setTitle("");

        txt_smartnode.setText(switchName);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        adapter.updateAllItemstoDatabase();
        finish();
    }

    @Override
    public void deleteSchedule(int position, Bean_ScheduleItem scheduleItem, int deletePosition) {
        if (!scheduleItem.getSlot_num().equalsIgnoreCase("26")) {
            deleteDialog.show();
            positionForDelete = deletePosition;
            forAddingNewItem = false;
            try {
                JSONObject object = new JSONObject();
                object.put("cmd", Cmd.SCH);
                object.put("slave", scheduleItem.getSlave_id());
                object.put("token", databaseHandler.getSlaveToken(scheduleItem.getSlave_id()));

                String data = "D-" + scheduleItem.getSlot_num();
                object.put("data", data);

                String command = object.toString();
                Log.e("Command", command);

                List<String> ipList=new IPDb(this).ipList();

                if (ipList.contains(databaseHandler.getMasterIPBySlaveID(scheduleItem.getSlave_id()))) {
                    new SendUDP(command).execute();
                } else {
                    new PublishMessage(command, scheduleItem.getSlave_id()).execute();
                }
            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        }
    }

    @Override
    public void getAllSlotsInfo(String slaveID) {
        forAvailInfo = false;
        createSchuduleDialog.setMessage("Gathering Slot Information");
        if (NetworkUtility.isOnline(getApplicationContext())) {
            createSchuduleDialog.show();

            List<String> ipList=new IPDb(this).ipList();
            try {
                JSONObject object = new JSONObject();
                object.put("cmd", Cmd.SCH);
                object.put("slave", slaveID);
                object.put("data", "ALL");
                object.put("token", databaseHandler.getSlaveToken(slaveID));

                String command = object.toString();
                Log.e("command", command);

                if (preference.isOnline() || (!preference.isOnline() && !preference.getCurrentIPAddr().equalsIgnoreCase(databaseHandler.getMasterIPBySlaveID(slaveID)))) {
                    new PublishMessage(command, slaveID).execute();
                } else {
                    new SendUDP(command).execute();
                }

            } catch (Exception e) {
                Log.e("Exception", e.getMessage());
            }
        } else {
            C.Toast(getApplicationContext(), getString(R.string.nointernet));
        }

    }

    @Override
    public void createSchedule(int position, String createCommand, String slaveID) {

        Log.e("create SCH", "Called");
        List<String> ipList=new IPDb(this).ipList();
        forAddingNewItem = false;
        if (NetworkUtility.isOnline(getApplicationContext())) {
            if (createSchuduleDialog.isShowing())
                createSchuduleDialog.setMessage("Creating Schedule");
            if(ipList.contains(databaseHandler.getMasterIPBySlaveID(slaveID))){
                new SendUDP(createCommand).execute();
            }
            else {
                new PublishMessage(createCommand, slaveID).execute();
            }
        } else {
            C.Toast(getApplicationContext(), getString(R.string.nointernet));
        }
    }

    private class PublishMessage extends AsyncTask<Void, Void, Void> {

        String command;
        String slaveID = "";
        boolean setList1 = false;

        public PublishMessage(String command, String slaveID) {
            this.command = command;
            this.slaveID = slaveID;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.e("command PM", command);
            if (networkUtility.isOnline()) {
                try {
                    mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientID, new MemoryPersistence());
                    MqttConnectOptions connectOptions = new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                    MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                    mqttMessage.setRetained(false);
                    mqttMessage.setQos(0);
                    mqttClient.publish(databaseHandler.getSlaveTopic(slaveID) + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                    mqttClient.disconnect();

                    //Log.e("MQTT SCH", "called");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    private class SendUDP extends AsyncTask<Void, Void, String> {
        String message;

        public SendUDP(String message) {
            this.message = message;
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


}