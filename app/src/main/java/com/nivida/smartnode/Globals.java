package com.nivida.smartnode;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by chaitalee on 3/14/2016.
 */
public class Globals {

    //public static String server_link ="";

    public static final String FAVOURITE="FAVOURITE";
    public static final String GROUP="GROUP";

    public static String share ="https://play.google.com/store/apps/details?id=com.nivida.smartnode&hl=en";
    public static String server_link ="http://app.nivida.in/agraeta/";

   //public static String server_link1 ="http://192.168.1.114/agraeta/";


    static boolean connect = true;
    public static boolean isConnectingToInternet1(Context con){

        BroadcastReceiver mConnReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                boolean noConnectivity = intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
	        /*String reason = intent
	                .getStringExtra(ConnectivityManager.EXTRA_REASON);*/
                boolean isFailover = intent.getBooleanExtra(
                        ConnectivityManager.EXTRA_IS_FAILOVER, false);

            //    ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);



                @SuppressWarnings("deprecation")
                NetworkInfo currentNetworkInfo = (NetworkInfo) intent
                        .getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
                // NetworkInfo otherNetworkInfo = (NetworkInfo)
                // intent.getParcelableExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

                if (noConnectivity){
                    connect = false;

                }
                else if (currentNetworkInfo.isConnected()) {
                    connect = true;


                } else if (isFailover) {
                    connect = false;
                }  else {
                    connect = true;
                }
            }
        };
        return connect;
    }
    public static boolean isConnectingToInternet(Context con){
        ConnectivityManager connectivity = (ConnectivityManager) con.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
        }
        return false;
    }



}
