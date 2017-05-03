package com.nivida.smartnode.beans;

/**
 * Created by Chintak Patel +91 8999799399 on 18-Jul-16.
 */
public class Bean_Dimmer {

    int dimmer_id;
    String dimmer_name;
    int dimmerValue;
    int dimmerInSlave;
    int dimmerInGroup;
    int isFavourite;
    boolean checked=false;

    public Bean_Dimmer() {
    }

    public Bean_Dimmer(int dimmer_id, String dimmer_name, int dimmerValue, int dimmerInSlave, int dimmerInGroup) {
        this.dimmer_id = dimmer_id;
        this.dimmer_name = dimmer_name;
        this.dimmerValue = dimmerValue;
        this.dimmerInSlave = dimmerInSlave;
        this.dimmerInGroup = dimmerInGroup;
    }

    public Bean_Dimmer(String dimmer_name, int dimmerValue, int dimmerInSlave, int dimmerInGroup) {
        this.dimmer_name = dimmer_name;
        this.dimmerValue = dimmerValue;
        this.dimmerInSlave = dimmerInSlave;
        this.dimmerInGroup = dimmerInGroup;
    }

    public int getDimmer_id() {
        return dimmer_id;
    }

    public void setDimmer_id(int dimmer_id) {
        this.dimmer_id = dimmer_id;
    }

    public String getDimmer_name() {
        return dimmer_name;
    }

    public void setDimmer_name(String dimmer_name) {
        this.dimmer_name = dimmer_name;
    }

    public int getDimmerValue() {
        return dimmerValue;
    }

    public void setDimmerValue(int dimmerValue) {
        this.dimmerValue = dimmerValue;
    }

    public int getDimmerInSlave() {
        return dimmerInSlave;
    }

    public void setDimmerInSlave(int dimmerInSlave) {
        this.dimmerInSlave = dimmerInSlave;
    }

    public int getDimmerInGroup() {
        return dimmerInGroup;
    }

    public void setDimmerInGroup(int dimmerInGroup) {
        this.dimmerInGroup = dimmerInGroup;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public int getIsFavourite() {
        return isFavourite;
    }

    public void setIsFavourite(int isFavourite) {
        this.isFavourite = isFavourite;
    }
}
