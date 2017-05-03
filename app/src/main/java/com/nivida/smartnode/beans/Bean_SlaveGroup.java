package com.nivida.smartnode.beans;

import android.graphics.Bitmap;

/**
 * Created by Chintak Patel on 14-Jul-16.
 */
public class Bean_SlaveGroup {

    int id;
    String name="";
    String hasSwitches;
    String hasDimmers;
    String hex_id="";
    int master_id;
    String slaveTopic="";
    String slaveToken="";
    String slaveUserType="";

    public Bean_SlaveGroup() {
    }

    public Bean_SlaveGroup(int id, String name, String hasSwitches, String hasDimmers) {
        this.id = id;
        this.name = name;
        this.hasDimmers = hasDimmers;
        this.hasSwitches = hasSwitches;
    }

    public Bean_SlaveGroup(String name, Bitmap bitmap, String hasSwitches, String hasDimmers) {
        this.name = name;
        this.hasDimmers = hasDimmers;
        this.hasSwitches = hasSwitches;
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

    public String getHasSwitches() {
        return hasSwitches;
    }

    public void setHasSwitches(String hasSwitches) {
        this.hasSwitches = hasSwitches;
    }

    public String getHasDimmers() {
        return hasDimmers;
    }

    public void setHasDimmers(String hasDimmers) {
        this.hasDimmers = hasDimmers;
    }

    public String getHex_id() {
        return hex_id;
    }

    public void setHex_id(String hex_id) {
        this.hex_id = hex_id;
    }

    public int getMaster_id() {
        return master_id;
    }

    public void setMaster_id(int master_id) {
        this.master_id = master_id;
    }

    public String getSlaveTopic() {
        return slaveTopic;
    }

    public void setSlaveTopic(String slaveTopic) {
        this.slaveTopic = slaveTopic;
    }

    public String getSlaveToken() {
        return slaveToken;
    }

    public void setSlaveToken(String slaveToken) {
        this.slaveToken = slaveToken;
    }

    public String getSlaveUserType() {
        return slaveUserType;
    }

    public void setSlaveUserType(String slaveUserType) {
        this.slaveUserType = slaveUserType;
    }
}
