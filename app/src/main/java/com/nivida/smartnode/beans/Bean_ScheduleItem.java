package com.nivida.smartnode.beans;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Chintak Patel on 20-Aug-16.
 */
public class Bean_ScheduleItem {

    int scheduleID=0;
    int switchID=0;
    String slave_id="";
    String switch_btn_num="00";
    String[] days={"N","N","N","N","N","N","N"};
    int repeat=0;
    boolean repeated=false;
    boolean daily=false;
    boolean once=false;
    boolean alldaysSelected=false;
    boolean switchOn=false;
    boolean schEnabled=false;
    String dimmerValue="5";

    String slot_num="26";

    String time="";

    public Bean_ScheduleItem() {
    }

    public int getScheduleID() {
        return scheduleID;
    }

    public void setScheduleID(int scheduleID) {
        this.scheduleID = scheduleID;
    }

    public int getSwitchID() {
        return switchID;
    }

    public void setSwitchID(int switchID) {
        this.switchID = switchID;
    }

    public String getSlave_id() {
        return slave_id;
    }

    public void setSlave_id(String slave_id) {
        this.slave_id = slave_id;
    }

    public String[] getDays() {
        return days;
    }

    public void setDays(String[] days) {
        this.days = days;
    }

    public void setPerticularDay(int position,String yes){
        this.days[position]=yes;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public boolean isDaily() {
        return daily;
    }

    public void setDaily(boolean daily) {
        this.daily = daily;
    }

    public boolean isOnce() {
        return once;
    }

    public void setOnce(boolean once) {
        this.once = once;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public boolean isAlldaysSelected() {
        return alldaysSelected;
    }

    public void setAlldaysSelected(boolean alldaysSelected) {
        this.alldaysSelected = alldaysSelected;
    }

    public boolean isRepeated() {
        return repeated;
    }

    public void setRepeated(boolean repeated) {
        this.repeated = repeated;
    }

    public String getSwitch_btn_num() {
        return switch_btn_num;
    }

    public void setSwitch_btn_num(String switch_btn_num) {
        this.switch_btn_num = switch_btn_num;
    }

    public boolean isSwitchOn() {
        return switchOn;
    }

    public void setSwitchOn(boolean switchOn) {
        this.switchOn = switchOn;
    }

    public boolean isSchEnabled() {
        return schEnabled;
    }

    public void setSchEnabled(boolean schEnabled) {
        this.schEnabled = schEnabled;
    }

    public String getSlot_num() {
        return slot_num;
    }

    public void setSlot_num(String slot_num) {
        this.slot_num = slot_num;
    }

    public String getDimmerValue() {
        return dimmerValue;
    }

    public void setDimmerValue(String dimmerValue) {
        this.dimmerValue = dimmerValue;
    }

    public boolean areAllDaysSelected(){
        boolean isAll=true;

        for (String day : days) {
            if (day.equals("N")) {
                isAll = false;
                break;
            }
        }

        return isAll;
    }

    public boolean areNoneDaysSelected(){
        boolean isAll=true;

        for (String day : days) {
            if (day.equals("Y")) {
                isAll = false;
                break;
            }
        }

        return isAll;
    }

    public void setSingleDay(int index,String day){
        this.getDays()[index]=day;
    }

    public void setAllDaySelected(){
        for(int i=0; i<days.length; i++){
            this.days[i]="Y";
        }
    }

    public void setNoneDaySelected(){
        for(int i=0; i<days.length; i++){
            this.days[i]="N";
        }
    }

    public void setDefaultDaySelected(){
        for(int i=0; i<days.length; i++){
            if(i==getCurrentDay()-1)
                this.days[i]="Y";
            else
                this.days[i]="N";
        }
    }

    public boolean isSingleDaySelected(){
        int count=0;
        for (String day : days) {
            if (day.equals("Y"))
                count++;
        }

        return count == 1;
    }

    private int getCurrentDay(){
        Calendar calendar=Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_WEEK);
    }
}
