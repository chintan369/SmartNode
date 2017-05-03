package com.nivida.smartnode.beans;

/**
 * Created by Chintak Patel on 06-Aug-16.
 */
public class Bean_SwitchIcons {

    int iconid;
    int swOnId;
    int swOffId;
    boolean checked=false;

    public Bean_SwitchIcons(int iconid, int swOnId, int swOffId) {
        this.iconid = iconid;
        this.swOnId = swOnId;
        this.swOffId = swOffId;
    }

    public Bean_SwitchIcons(int swOnId, int swOffId) {
        this.swOnId = swOnId;
        this.swOffId = swOffId;
    }

    public Bean_SwitchIcons() {
    }

    public int getIconid() {
        return iconid;
    }

    public void setIconid(int iconid) {
        this.iconid = iconid;
    }

    public int getSwOnId() {
        return swOnId;
    }

    public void setSwOnId(int swOnId) {
        this.swOnId = swOnId;
    }

    public int getSwOffId() {
        return swOffId;
    }

    public void setSwOffId(int swOffId) {
        this.swOffId = swOffId;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
