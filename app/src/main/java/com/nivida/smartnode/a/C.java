package com.nivida.smartnode.a;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by Chintak Patel on 15-Nov-16.
 */

public class C {

    public static Typeface raleway(Context context){
        return Typeface.createFromAsset(context.getAssets(),"fonts/raleway.ttf");
    }

    public static final String MQTT_ClientID="lens_7RWhmqsyqMKGrTmoNNGJdne5R7E";

    public static String GetDeviceipMobileData(){
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
                 en.hasMoreElements();) {
                NetworkInterface networkinterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkinterface.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (Exception ex) {
            Log.e("Current IP", ex.toString());
        }
        return null;
    }

    public static String GetDeviceipWiFiData(Context context)
    {

        WifiManager wm = (WifiManager) context.getSystemService(WIFI_SERVICE);

        @SuppressWarnings("deprecation")

        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        return ip;

    }

    public static void Toast(Context context, String message) {

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.custom_toast, null);

        Typeface typeface = Typeface.createFromAsset(context.getAssets(), "fonts/raleway.ttf");

        TextView text = (TextView) view.findViewById(R.id.toasttext);
        text.setText(message);
        text.setTypeface(typeface);
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, toast.getXOffset(), toast.getYOffset());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    public static void connectionError(Context context){
        Toast(context,"Error occured in connection establishment!\nTry agin later!");
    }

    public static String GetSubnetMask_WIFI(Context context) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        DhcpInfo dhcp = wifiManager.getDhcpInfo();
        String mask = intToIP(~(dhcp.netmask));

        return mask;
    }

    private static String intToIP(int ipAddress) {
        String ret = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));

        return ret;
    }

    public static boolean isValidIP(String ipaddress) {

        final String PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ipaddress);
        return matcher.matches();
    }

    public static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();
        // handle null somehow

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }

    public static String saveGroupImageToLocal(Bitmap groupPic, String groupNameID) {
        String imagePath="";

        String rootDirectory= Environment.getExternalStorageDirectory()+"/SmartNode/Groups/";
        File rootDir= new File(rootDirectory);
        if(!rootDir.exists()) rootDir.mkdir();

        String imageName=groupNameID+".png";

        File imageFile=new File(rootDir,imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            // Use the compress method on the BitMap object to write image to the OutputStream
            groupPic.compress(Bitmap.CompressFormat.PNG, 50, fos);
            imagePath=imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return imagePath;
    }
}
