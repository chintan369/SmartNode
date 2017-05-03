package com.nivida.smartnode.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

/**
 * Created by Chintak Patel on 13-Jul-16.
 */
public class AppPreference {

    public boolean configured=false;
    public boolean loggedIn=false;
    public boolean master=false;
    public String ipaddress;
    public String portnumber;
    public String hashkey;
    public boolean firstTimeInstalled=true;
    public boolean isSlaveActivityFromMaster=false;
    public String masterNameForDevice="";
    public int masterIDForDevice;
    public String token="";
    public String topic="";
    boolean online=false;
    boolean fromDirectMaster=true;

    float pricePerUnit=7;

    boolean masterUser=false;

    String currentIPAddr="";

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    private static final String PREFERENCE_FILE_NAME= "AppPreference";

    public AppPreference(Context context) {
        preferences=context.getSharedPreferences(PREFERENCE_FILE_NAME,Context.MODE_PRIVATE);
        editor=preferences.edit();
        configured=preferences.getBoolean("isConfigured",false);
        loggedIn=preferences.getBoolean("isLoggedIn",false);
        master=preferences.getBoolean("isMaster",true);
        ipaddress=preferences.getString("ip_address","");
        portnumber=preferences.getString("port_number","");
        hashkey=preferences.getString("hash_key","");
        firstTimeInstalled=preferences.getBoolean("firstTimeInstalled",true);
        isSlaveActivityFromMaster=preferences.getBoolean("isSlaveFromMaster",false);
        masterNameForDevice=preferences.getString("masterNameForDevice","ADD DEVICE");
        masterIDForDevice=preferences.getInt("masterIDForDevice",1);
        token=preferences.getString("token","");
        topic=preferences.getString("topic","");
        online=preferences.getBoolean("online",false);
        fromDirectMaster=preferences.getBoolean("fromDirectMaster",true);
        masterUser=preferences.getBoolean("masterUser",false);
        pricePerUnit=preferences.getFloat("pricePerUnit",7);
        currentIPAddr=preferences.getString("currentIP","");
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
        editor.putString("topic",topic).commit();
    }

    public String getCurrentIPAddr() {
        return preferences.getString("currentIP","");
    }

    public void setCurrentIPAddr(String currentIPAddr) {
        this.currentIPAddr = currentIPAddr;
        editor.putString("currentIP",currentIPAddr).commit();
    }

    public boolean isConfigured() {
        return configured;
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
        editor.putBoolean("isConfigured",configured);
        editor.commit();
    }

    public boolean isFirstTimeInstalled() {
        return firstTimeInstalled;
    }

    public void setFirstTimeInstalled(boolean firstTimeInstalled) {
        this.firstTimeInstalled = firstTimeInstalled;
        editor.putBoolean("firstTimeInstalled",firstTimeInstalled);
        editor.commit();
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
        editor.putBoolean("isLoggedIn",loggedIn);
        editor.commit();
    }

    public boolean isMaster() {
        return master;
    }

    public void setMaster(boolean master) {
        this.master = master;
        editor.putBoolean("isMaster",master);
        editor.commit();
    }

    public String getIpaddress() {
        return ipaddress;
    }

    public void setIpaddress(String ipaddress) {
        this.ipaddress = ipaddress;
        editor.putString("ip_address",ipaddress);
        editor.commit();
    }

    public String getPortnumber() {
        return portnumber;
    }

    public void setPortnumber(String portnumber) {
        this.portnumber = portnumber;
        editor.putString("port_number",portnumber);
        editor.commit();
    }

    public String getHashkey() {
        return hashkey;
    }

    public void setHashkey(String hashkey) {
        this.hashkey = hashkey;
        editor.putString("hash_key",hashkey);
        editor.commit();
    }

    public boolean getSlaveActivityFromMaster() {
        return isSlaveActivityFromMaster;
    }

    public void setSlaveActivityFromMaster(boolean slaveActivityFromMaster) {
        isSlaveActivityFromMaster = slaveActivityFromMaster;
        editor.putBoolean("isSlaveFromMaster",slaveActivityFromMaster);
        editor.commit();
    }

    public String getMasterNameForDevice() {
        return masterNameForDevice;
    }

    public void setMasterNameForDevice(String masterNameForDevice) {
        this.masterNameForDevice = masterNameForDevice;
        editor.putString("masterNameForDevice",masterNameForDevice);
        editor.commit();
    }

    public int getMasterIDForDevice() {
        return masterIDForDevice;
    }

    public void setMasterIDForDevice(int masterIDForDevice) {
        this.masterIDForDevice = masterIDForDevice;
        editor.putInt("masterIDForDevice",masterIDForDevice);
        editor.commit();
    }

    public String getToken() {
        return preferences.getString("token","");
    }

    public void setToken(String token) {
        this.token = token;
        editor.putString("token",token).commit();
    }

    public boolean isOnline() {
        return preferences.getBoolean("online",false);
    }

    public void setOnline(boolean online) {
        this.online = online;
        editor.putBoolean("online",online).commit();
    }

    public boolean isFromDirectMaster() {
        return fromDirectMaster;
    }

    public void setFromDirectMaster(boolean fromDirectMaster) {
        this.fromDirectMaster = fromDirectMaster;
        editor.putBoolean("fromDirectMaster",fromDirectMaster).commit();
    }

    public boolean isMasterUser() {
        return preferences.getBoolean("masterUser",false);
    }

    public void setMasterUser(boolean masterUser) {
        this.masterUser = masterUser;
        editor.putBoolean("masterUser",masterUser).commit();
    }

    public float getPricePerUnit() {
        return preferences.getFloat("pricePerUnit",7);
    }

    public void setPricePerUnit(float pricePerUnit) {
        this.pricePerUnit = pricePerUnit;
        editor.putFloat("pricePerUnit",pricePerUnit).commit();
    }
}
