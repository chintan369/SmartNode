package com.nivida.smartnode.beans;

/**
 * Created by Chintak Patel on 14-Jul-16.
 */
public class Bean_Master {

    int id=0;
    String name="";
    String type="";
    String topic="";
    String enckey="";
    String hasSlaves;
    String userType="";
    String masterID="";
    String ipAddress="";
    String deviceID = "";

    public Bean_Master() {
    }

    public Bean_Master(int id, String name) {
        this.id = id;
        this.name = name;

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getEnckey() {
        return enckey;
    }

    public void setEnckey(String enckey) {
        this.enckey = enckey;
    }

    public String getHasSlaves() {
        return hasSlaves;
    }

    public void setHasSlaves(String hasSlaves) {
        this.hasSlaves = hasSlaves;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getMasterID() {
        return masterID;
    }

    public void setMasterID(String masterID) {
        this.masterID = masterID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }
}
