package com.nivida.smartnode;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.GroupSwitchService;
import com.nivida.smartnode.services.UDPService;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

import static android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SplashActivity extends AppCompatActivity {

    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.UDPService";
    public static final String MQTTSERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    public static final String CHECKINGSERVICE_CLASSNAME = "com.nivida.smartnode.services.CheckStatusService";
    private static int SPLASH_TIME_OUT = 2500;

    private static int CHECK_AND_SEND_STS_DELAY = 2000;

    BroadcastReceiver receiver;

    AppPreference preference;
    DatabaseHandler databaseHandler;
    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_DOCUMENTS, Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE, CHANGE_WIFI_MULTICAST_STATE};
    int code = 1;
    GifImageView splash_image;
    int deviceSTSFound = 0;
    int totalDeviceFound = 0;
    Handler mHandler;
    Runnable mRunnable;
    private int[] images = {R.drawable.kitchen, R.drawable.bedroom,
            R.drawable.mainroom,
            R.drawable.drawingroom, R.drawable.add_new};
    private String[] names = {"Kitchen", "Bed Room", "Main Room", "Drawing Room",
            "Add New Group"};

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
        setContentView(R.layout.activity_splash);

        splash_image = (GifImageView) findViewById(R.id.splash_image);

        startReceiver();

        preference = new AppPreference(getApplicationContext());

        if (preference.isFirstTimeInstalled()) {
            deletePreviousData();
            preference.setMqttClientID(MqttClient.generateClientId());
        }

        String dbFileName = Environment.getExternalStorageDirectory() + "/SmartNode/" + "smartnodedb.db";

        File dbFile = new File(dbFileName);
        if (!dbFile.exists()) {
            DatabaseHandler handler = new DatabaseHandler(this);
        }

        databaseHandler = new DatabaseHandler(this);

        //Log.e("Client ID", MqttClient.generateClientId());

        try {
            Process process = new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isMarshmallowPlusDevice();
        isPermissionRequestRequired(this, perms, code);

        //startService(new Intent(getApplicationContext(), UDPService.class));
        //startService(new Intent(this,AddDeviceService.class));

        if (Globals.isConnectingToInternet1(getApplicationContext())) {
            new ReceiveUDP().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            C.Toast(getApplicationContext(), "Please Connect to the Network First");
        }


        if (preference.isFirstTimeInstalled()) {
            preference.setFirstTimeInstalled(false);
        }
        // splash code
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (databaseHandler.getGroupDataCounts() == 0) {
                    databaseHandler.addDefaultRows(images, names);
                }
                preference.setFirstTimeInstalled(false);
                if (SmartNode.deviceIDs.size() > 0) {
                    C.Toast(getApplicationContext(), "You are connected in LAN");
                }
                if (databaseHandler.getMastersCounts() <= 1) {
                    Intent intent = new Intent(getApplicationContext(), AddMasterActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    sendSTSCommands();
                }
            }
        }, CHECK_AND_SEND_STS_DELAY);

    }

    private void startReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();

                if (bundle != null) {
                    String subscribedMessage = bundle.getString(AddDeviceService.MESSAGETOSEND);
                    String UDPMessage = bundle.getString(UDPService.MESSAGEJSON);

                    //Log.e("JSOn fr group ", "" + subscribedMessage);
                    if (UDPMessage != null) {
                        handleCommands(UDPMessage);
                    } else if (subscribedMessage != null) {
                        handleCommands(subscribedMessage);
                    }
                }
            }
        };
    }

    private void handleCommands(String message) {
        try {
            JSONObject object = new JSONObject(message);

            if (object.getString("cmd").equals(Cmd.STS)) {
                deviceSTSFound++;

                if (deviceSTSFound == totalDeviceFound) {
                    if (mHandler != null) {
                        mHandler.removeCallbacks(mRunnable);
                    }

                    goToSpecificActivity();
                }
            }
        } catch (JSONException e) {
            Log.e("Exception", e.getMessage());
        }
    }

    private void sendSTSCommands() {
        totalDeviceFound = SmartNode.deviceIDs.size();

        List<Bean_SlaveGroup> slaveIDs = databaseHandler.getAllSlaveHex();

        List<String> commands = new ArrayList<>();

        for (int i = 0; i < slaveIDs.size(); i++) {
            final JSONObject object = new JSONObject();

            try {
                object.put("cmd", Cmd.STS);
                object.put("token", slaveIDs.get(i).getSlaveToken());
                object.put("slave", slaveIDs.get(i).getHex_id());

                Log.e("slaveInLocal_" + (i + 1), slaveIDs.get(i).getHex_id());

                if (!SmartNode.slavesInLocal.contains(slaveIDs.get(i).getHex_id()) && SmartNode.deviceIDs.contains(slaveIDs.get(i).getMasterDeviceID())) {
                    SmartNode.slavesInLocal.add(slaveIDs.get(i).getHex_id());
                }

                commands.add(object.toString());

                new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (int i = slaveIDs.size() - 1; i >= 0; i--) {
            new SendUDP(commands.get(i)).execute();
            new SendMQTT(slaveIDs.get(i).getSlaveTopic(), commands.get(i)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        sendScheduleCommands(slaveIDs);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                goToSpecificActivity();
            }
        };
        mHandler.postDelayed(mRunnable, 3000);
    }

    private void sendScheduleCommands(List<Bean_SlaveGroup> slaveIDs) {

        List<String> commands = new ArrayList<>();

        for (int i = slaveIDs.size() - 1; i >= 0; i--) {
            JSONObject object = new JSONObject();

            try {
                object.put("cmd", Cmd.SCH);
                object.put("data", "ALL");
                object.put("token", slaveIDs.get(i).getSlaveToken());
                object.put("slave", slaveIDs.get(i).getHex_id());

                commands.add(object.toString());

                if (!SmartNode.slavesInLocal.contains(slaveIDs.get(i).getHex_id()) && SmartNode.deviceIDs.contains(slaveIDs.get(i).getMasterDeviceID())) {
                    SmartNode.slavesInLocal.add(slaveIDs.get(i).getHex_id());
                }

                new SendUDP(object.toString()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (int i = slaveIDs.size() - 1; i >= 0; i--) {
            new SendMQTT(slaveIDs.get(i).getSlaveTopic(), commands.get(i)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void deletePreviousData() {
        String root = Environment.getExternalStorageDirectory() + "/SmartNode/";
        File rootFile = new File(root);

        if (rootFile.exists()) {
            deleteFileFolder(rootFile);
        }
    }

    private void deleteFileFolder(File file) {
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles.length > 0) {
                for (File subFile : subFiles) {
                    if (!subFile.isDirectory())
                        deleteFileFolder(subFile);
                }
            }
        }

        file.delete();
    }

    public void goToSpecificActivity() {
        unregisterReceiver(receiver);
        if (databaseHandler.getMastersCounts() > 1) {
            Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getApplicationContext(), AddMasterActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void onResume() {
        registerReceiver(receiver, new IntentFilter(GroupSwitchService.NOTIFICATION));
        startRequiredServices();
        super.onResume();
    }

    private void startRequiredServices() {


        /*Intent intent = new Intent(this, AddDeviceService.class);
        startService(intent);*/
    }

    private void showWarningDialog() {
        final android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(this);

        dialogBuilder.setTitle("Warning !");
        dialogBuilder.setMessage("Your app has expired.\nplease contact to app vendor.");
        dialogBuilder.setPositiveButton("Ok", null);
        //dialogBuilder.setNegativeButton("Cancel", null);
        final android.app.AlertDialog b = dialogBuilder.create();

        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn_postive = b.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
                //Button btn_cancel = b.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        b.dismiss();
                        finish();
                    }
                });
            }
        });
        b.show();
    }

    public boolean isExpired() {
        boolean expired = false;

        String timeSettings = android.provider.Settings.System.getString(
                this.getContentResolver(),
                android.provider.Settings.System.AUTO_TIME);
        Log.e("Auto Time", timeSettings);
        if (timeSettings.contentEquals("0")) {
            android.provider.Settings.System.putString(
                    this.getContentResolver(),
                    Settings.System.AUTO_TIME, "1");
        }
        Date now = new Date(System.currentTimeMillis());
        Log.d("Date", now.toString());

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
        String date = dateFormat.format(Calendar.getInstance().getTime());
        Log.e("Today date :", date);

        String endDate = "09-09-2016";

        try {
            if (dateFormat.parse(date).before(dateFormat.parse(endDate)))
                expired = false;
            else if (dateFormat.parse(date).equals(dateFormat.parse(endDate)))
                expired = true;
            else expired = true;


        } catch (ParseException e) {
            e.printStackTrace();
        }

        return expired;
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

    private boolean MQTTserviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MQTTSERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private boolean CheckingServiceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (CHECKINGSERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint("WifiManagerLeak")
    public void run() {
        //Looper.prepare();
        try {
            WifiManager.MulticastLock lock = null;
            WifiManager wifi;

            wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                if (lock == null)
                    lock = wifi.createMulticastLock("WiFi_Lock");
                lock.setReferenceCounted(true);
                lock.acquire();
            }
        } catch (Exception e) {
            Log.d("Wifi Exception", "" + e.getMessage().toString());
        }
    }

    private class ReceiveUDP extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            String message = AppConstant.CMD_GET_MASTER_TOKEN;

            //String encrypted=CustomEncryption.EncryptedCommand(message,"fe434d98558ce2b347171198542f112d");

            //Log.e("ASCII", encrypted);
            //Log.e("ASCII De", CustomEncryption.decryptedCommand(encrypted,"fe434d98558ce2b347171198542f112d"));
            // Log.e("Hex to Int", "-- "+CustomEncryption.hexToInteger("fe434d98558ce2b347171198542f112d"));

            run();
            try {
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                Log.e("message", message);

                Log.e("IP Address Saved", "->" + preference.getIpaddress());

                /*if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {*/
                server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);
                /*} else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                }*/

                DatagramSocket client_socket;
                client_socket = new DatagramSocket(13000);
                client_socket.setSoTimeout(2500);
                client_socket.setReuseAddress(true);

                String text = "";
                byte[] recieve_data = new byte[2048];
                DatagramPacket recvpacket = new DatagramPacket(recieve_data, recieve_data.length);
                Log.e("Packet", "Object created");
                //client_socket.setSoTimeout(60000);
                client_socket.receive(recvpacket);
                Log.e("Packet :", "Recieved");

                Log.e("Recived IP :", recvpacket.getAddress().getHostAddress());

                preference.setIpaddress(recvpacket.getAddress().getHostAddress());
                preference.setCurrentIPAddr(recvpacket.getAddress().getHostAddress());

                text = new String(recieve_data, 0, recvpacket.getLength());
                Log.e("Received Data :", text);

                socket.disconnect();
                socket.close();
                client_socket.disconnect();
                client_socket.close();
                return text;
                /*int port = recvpacket.getPort();

                Log.e("Received port :", String.valueOf(port));
                Log.e("Received Pac Data", recvpacket.getData().toString());*/
            } catch (IOException e) {
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
            //progressbar.setVisibility(View.VISIBLE);
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

                server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                packet = new DatagramPacket(senddata, senddata.length, server_addr);
                socket.setReuseAddress(true);
                socket.setBroadcast(true);
                socket.send(packet);

                socket.disconnect();
                socket.close();
            } catch (SocketException s) {
                Log.e("Exception", "-->" + s.getLocalizedMessage());
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

    public class SendMQTT extends AsyncTask<Void, Void, Void> {

        String topic = "";
        String command = "";

        public SendMQTT(String topic, String command) {
            this.topic = topic;
            this.command = command;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                String clientID = MqttClient.generateClientId();
                MqttClient mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientID, new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                connectOptions.setConnectionTimeout(3);
                mqttClient.connect(connectOptions);


                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(topic + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);

            } catch (MqttException e) {
                Log.e("Exception M: ", e.getMessage());
                e.printStackTrace();
                SmartNode.isConnectedToInternet = false;
            } catch (Exception e) {
                Log.e("Exception : ", e.getMessage());
            }
            return null;
        }
    }
}
