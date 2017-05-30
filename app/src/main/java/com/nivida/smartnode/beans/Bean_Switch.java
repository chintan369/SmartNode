package com.nivida.smartnode.beans;

/**
 * Created by Chintak Patel on 18-Jul-16.
 */
public class Bean_Switch {

    int switch_id;
    String isSwitch="s";
    String switch_btn_num="00";
    String switch_name;
    int isSwitchOn;
    String switchInSlave;
    int switchInGroup;
    int isFavourite;
    int dimmerValue;
    boolean checked=false;
    int switch_icon=1;
    int hasSchedule=0;
    int sceneid=0;
    String userLock="N";
    String touchLock="N";
    boolean isLoading = false;


    public Bean_Switch() {
    }

    public Bean_Switch(int switch_id, String switch_name, int isSwitchOn, String switchInSlave, int switchInGroup) {
        this.switch_id = switch_id;
        this.switch_name = switch_name;
        this.isSwitchOn = isSwitchOn;
        this.switchInSlave = switchInSlave;
        this.switchInGroup = switchInGroup;
    }

    public Bean_Switch(String switch_name, int isSwitchOn, String switchInSlave, int switchInGroup) {
        this.switch_name = switch_name;
        this.isSwitchOn = isSwitchOn;
        this.switchInSlave = switchInSlave;
        this.switchInGroup = switchInGroup;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getSwitch_id() {
        return switch_id;
    }

    public void setSwitch_id(int switch_id) {
        this.switch_id = switch_id;
    }

    public String getSwitch_name() {
        return switch_name;
    }

    public void setSwitch_name(String switch_name) {
        this.switch_name = switch_name;
    }

    public int getIsSwitchOn() {
        return isSwitchOn;
    }

    public void setIsSwitchOn(int isSwitchOn) {
        this.isSwitchOn = isSwitchOn;
    }

    public String getSwitchInSlave() {
        return switchInSlave;
    }

    public void setSwitchInSlave(String switchInSlave) {
        this.switchInSlave = switchInSlave;
    }

    public int getSwitchInGroup() {
        return switchInGroup;
    }

    public void setSwitchInGroup(int switchInGroup) {
        this.switchInGroup = switchInGroup;
    }

    public int getIsFavourite() {
        return isFavourite;
    }

    public void setIsFavourite(int isFavourite) {
        this.isFavourite = isFavourite;
    }

    public String getIsSwitch() {
        return isSwitch;
    }

    public void setIsSwitch(String isSwitch) {
        this.isSwitch = isSwitch;
    }

    public int getDimmerValue() {
        return dimmerValue;
    }

    public void setDimmerValue(int dimmerValue) {
        this.dimmerValue = dimmerValue;
    }

    public String getSwitch_btn_num() {
        return switch_btn_num;
    }

    public void setSwitch_btn_num(String switch_btn_num) {
        this.switch_btn_num = switch_btn_num;
    }

    public int getSwitch_icon() {
        return switch_icon;
    }

    public void setSwitch_icon(int switch_icon) {
        this.switch_icon = switch_icon;
    }

    public int getHasSchedule() {
        return hasSchedule;
    }

    public void setHasSchedule(int hasSchedule) {
        this.hasSchedule = hasSchedule;
    }

    public int getSceneid() {
        return sceneid;
    }

    public void setSceneid(int sceneid) {
        this.sceneid = sceneid;
    }

    public String getUserLock() {
        return userLock;
    }

    public void setUserLock(String userLock) {
        this.userLock = userLock;
    }

    public String getTouchLock() {
        return touchLock;
    }

    public void setTouchLock(String touchLock) {
        this.touchLock = touchLock;
    }

    public boolean isLoading() {
        return isLoading;
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
    }
}
