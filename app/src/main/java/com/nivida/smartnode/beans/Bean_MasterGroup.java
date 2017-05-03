package com.nivida.smartnode.beans;

import android.graphics.Bitmap;

/**
 * Created by Chintak Patel on 14-Jul-16.
 */
public class Bean_MasterGroup {

    int id;
    String name;
    Bitmap bitmap;
    String hasSwitches="0";

    public Bean_MasterGroup() {
    }

    public Bean_MasterGroup(int id, String name, Bitmap bitmap, String hasSwitches) {
        this.id = id;
        this.name = name;
        this.bitmap = bitmap;
        this.hasSwitches = hasSwitches;
    }

    public Bean_MasterGroup(String name, Bitmap bitmap, String hasSwitches) {
        this.name = name;
        this.bitmap = bitmap;
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

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getHasSwitches() {
        return hasSwitches;
    }

    public void setHasSwitches(String hasSwitches) {
        this.hasSwitches = hasSwitches;
    }
}
