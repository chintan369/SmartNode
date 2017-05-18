package com.nivida.smartnode;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;

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

import static android.Manifest.permission.CHANGE_WIFI_MULTICAST_STATE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SplashActivity extends AppCompatActivity {

    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.UDPService";
    public static final String MQTTSERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    public static final String CHECKINGSERVICE_CLASSNAME = "com.nivida.smartnode.services.CheckStatusService";
    private static int SPLASH_TIME_OUT = 2500;
    AppPreference preference;
    DatabaseHandler databaseHandler;
    String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_DOCUMENTS, Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.CALL_PHONE, CHANGE_WIFI_MULTICAST_STATE};
    int code = 1;
    private int[] images={R.drawable.kitchen,R.drawable.bedroom,
            R.drawable.mainroom,
            R.drawable.drawingroom,R.drawable.add_new};
    private String[] names={"Kitchen","Bed Room","Main Room","Drawing Room",
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
        preference = new AppPreference(getApplicationContext());

        if (preference.isFirstTimeInstalled()) {
            deletePreviousData();
            preference.setMqttClientID(MqttClient.generateClientId());
        }

        new IPDb(this).deleteIP();

        String dbFileName = Environment.getExternalStorageDirectory() + "/SmartNode/" + "smartnodedb.db";

        File dbFile = new File(dbFileName);
        if (!dbFile.exists()) {
            DatabaseHandler handler = new DatabaseHandler(this);
        }

        databaseHandler = new DatabaseHandler(this);

        //Log.e("Client ID", MqttClient.generateClientId());

        if(NetworkUtility.isOnline(this)){
            startService(new Intent(getApplicationContext(), UDPService.class));
        }

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


        if (Globals.isConnectingToInternet1(getApplicationContext())) {
            new ReceiveUDP().execute();
        } else {
            C.Toast(getApplicationContext(), "Please Connect to the Network First");
        }


        if (preference.isFirstTimeInstalled()) {
            // splash code
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (databaseHandler.getGroupDataCounts() == 0) {
                        databaseHandler.addDefaultRows(images, names);
                    }
                    goToSpecificActivity();
                    preference.setFirstTimeInstalled(false);
                }
            }, SPLASH_TIME_OUT);
        } else {
            goToSpecificActivity();
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
        if(databaseHandler.getMastersCounts()>1){
            Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
            startActivity(intent);
        }
        else {
            Intent intent=new Intent(getApplicationContext(),AddMasterActivity.class);
            startActivity(intent);
        }
        finish();
    }

    @Override
    protected void onResume() {
        startRequiredServices();
        super.onResume();
    }

    private void startRequiredServices(){
        if(serviceIsRunning()){
            stopService(new Intent(getApplicationContext(),UDPService.class));
        }
        if(!serviceIsRunning()){
            Intent intent=new Intent(this, UDPService.class);
            startService(intent);
        }

        if(!MQTTserviceIsRunning()){
            Intent intent=new Intent(this, AddDeviceService.class);
            startService(intent);
        }

        /*if(!CheckingServiceIsRunning()){
            Intent intent=new Intent(this, CheckStatusService.class);
            startService(intent);
            Log.e("Check Service","Started");
        }*/
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

    public void run() {
        Looper.prepare();
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

                Log.e("message",message);

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
            } catch (SocketException s) {
                preference.setOnline(true);
                preference.setCurrentIPAddr("");
                //C.Toast(getApplicationContext(), s.getLocalizedMessage());
                Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                preference.setOnline(true);
                preference.setCurrentIPAddr("");
                //C.Toast(getApplicationContext(), e.getLocalizedMessage());
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s == null || s.isEmpty()) {
                preference.setOnline(true);
                //C.Toast(getApplicationContext(), "You are Connected with Internet");
            } else {
                preference.setOnline(false);
                C.Toast(getApplicationContext(), "You are Connected in LAN");
            }
        }
    }
}
