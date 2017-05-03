package com.nivida.smartnode.beans;

import java.io.Serializable;

/**
 * Created by Nivida new on 02-Dec-16.
 */

public class Bean_EnergySlave implements Serializable {

    public static final int TODAY=0;
    public static final int LAST_7_DAYS=1;
    public static final int LAST_30_DAYS=2;
    public static final int NOW=3;

    String slaveID="";
    String slaveName="";
    String unit="";
    String price="0";
    int day=TODAY;
    float[] usedWatts=new float[]{};
    String masterName="";
    String currentWatt="0";

    float pricePerRate=7;

    public Bean_EnergySlave() {
    }

    public String getSlaveName() {
        return slaveName;
    }

    public void setSlaveName(String slaveName) {
        this.slaveName = slaveName;
    }

    public String getSlaveID() {
        return slaveID;
    }

    public void setSlaveID(String slaveID) {
        this.slaveID = slaveID;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public float[] getUsedWatts() {
        return usedWatts;
    }

    public void setUsedWatts(float[] usedWatts) {
        this.usedWatts = usedWatts;
    }

    public String getTotalPrice(int type){
        String price="0";

        switch (type){
            case TODAY:
                price=countPrice(1);
                break;
            case LAST_7_DAYS:
                price=countPrice(7);
                break;
            case LAST_30_DAYS:
                price=countPrice(30);
                break;
            case NOW:
                price=getCurrentWattPrice();
                break;
        }

        return price;
    }

    private String countPrice(int days){
        float watts=0;
        float totalPrice=0;

        for(int i=usedWatts.length-1; i>(usedWatts.length-days-1); i--){
            watts += usedWatts[i];
        }

        totalPrice = watts * pricePerRate;

        return String.valueOf(totalPrice);
    }

    public void setCurrentWatt(String currentWatt){
        this.currentWatt=currentWatt;
    }

    public String getCurrentWatt(){
        return this.currentWatt;
    }

    public String getCurrentWattPrice(){
        float price=Integer.parseInt(this.currentWatt)*pricePerRate;

        return String.valueOf(price);
    }

    public String getTotalWatt(int type){
        String watt="0";

        switch (type){
            case TODAY:
                watt=countWatt(1);
                break;
            case LAST_7_DAYS:
                watt=countWatt(7);
                break;
            case LAST_30_DAYS:
                watt=countWatt(30);
                break;
            case NOW:
                watt=getCurrentWatt();
                break;
        }

        return watt;
    }

    private String countWatt(int days){
        float watts=0;

        for(int i=usedWatts.length-1; i>(usedWatts.length-days-1); i--){
            watts += usedWatts[i];
        }

        return String.valueOf(watts);
    }

    public String getMasterName() {
        return masterName;
    }

    public void setMasterName(String masterName) {
        this.masterName = masterName;
    }

    public float getPricePerRate() {
        return pricePerRate;
    }

    public void setPricePerRate(float pricePerRate) {
        this.pricePerRate = pricePerRate;
    }
}
