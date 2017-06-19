package com.nivida.smartnode.app;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.NetworkUtility;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;

/**
 * Created by Pratik on 12 Jun 2017.
 */

public class SmartNode extends Application {

    public static ArrayList<String> deviceIDs = new ArrayList<>();
    public static ArrayList<String> slavesInLocal = new ArrayList<>();
    public static ArrayList<String> slavesWorking = new ArrayList<>();
    public static HashMap<String, String> slaveCommands = new HashMap<>();
    public static DatabaseHandler databaseHandler;
    public static AppPreference preference;
    public static NetworkUtility networkUtility;
    public static boolean isConnectedToInternet = false;
    public static ArrayList<Integer> rSerials = new ArrayList<>();

    public static Picasso picassoInstance;
    Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("SmartNode", "Started");
        slavesInLocal.clear();
        slaveCommands.clear();
        rSerials.clear();

        databaseHandler = new DatabaseHandler(this);
        preference = new AppPreference(this);
        networkUtility = new NetworkUtility(this);

        stopService(new Intent(this, AddDeviceService.class));
        startService(new Intent(this, UDPService.class));
        startService(new Intent(this, AddDeviceService.class));


        picasso();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.e("SmartNode", "Terminated");
        slavesInLocal.clear();
        slaveCommands.clear();
        stopService(new Intent(this, UDPService.class));
        stopService(new Intent(this, AddDeviceService.class));
    }

    public void picasso() {
        if (picassoInstance == null) {
            Picasso.Builder builder = new Picasso.Builder(this);
            picassoInstance = builder.build();
        }
    }


}
