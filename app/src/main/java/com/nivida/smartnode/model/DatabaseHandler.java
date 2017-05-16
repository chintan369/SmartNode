package com.nivida.smartnode.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.nivida.smartnode.R;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.beans.Bean_Dimmer;
import com.nivida.smartnode.beans.Bean_Master;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.beans.Bean_Scenes;
import com.nivida.smartnode.beans.Bean_ScheduleItem;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.beans.Bean_SwitchIcons;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Chintak Patel on 14-Jul-16.
 */
public class DatabaseHandler extends SQLiteOpenHelper {

    public static final String Lock = "dblock";
    //Define Database version
    private static final int DATABASE_VERSION = 4;
    // Put your Database name
    private static final String DATABASE_NAME = Environment.getExternalStorageDirectory() + "/Smartnode/" + "smartnodedb.db";
    //put your Table name here
    private static final String TABLE_GROUPS = "groups";
    private static final String TABLE_MASTER = "master";
    private static final String TABLE_SLAVES = "slaves";
    private static final String TABLE_SWITCHES = "switches";
    private static final String TABLE_DIMMERS = "dimmers";
    private static final String TABLE_SWITCH_ICONS = "switch_icons";
    private static final String TABLE_SCENES = "scenes";
    private static final String TABLE_SCENE_SWITCH = "scene_switch";
    private static final String TABLE_SCHEDULE = "schedule";
    //define columns for TABLE_GROUPs here
    private static final String GROUP_GEN_ID = "id";
    private static final String GROUP_NAME = "g_name";
    private static final String GROUP_IMAGE = "g_image";
    private static final String GROUP_HAS_SWITCHES = "has_switches";
    //define columns for TABLE_GROUPs here
    private static final String MASTER_GEN_ID = "id";
    private static final String MASTER_NAME = "m_name";
    private static final String MASTER_TYPE = "m_type";
    private static final String MASTER_TOPIC = "m_topic";
    private static final String MASTER_ENCKEY = "m_enckey";
    private static final String MASTER_USERTYPE = "m_usertype";
    private static final String MASTER_ID = "m_masterID";
    private static final String MASTER_IP = "m_ipAddress";
    //define columns for TABLE_SLAVEs here
    private static final String SLAVE_GEN_ID = "id";
    private static final String SLAVE_HEX_ID = "hex_id";
    private static final String SLAVE_NAME = "s_name";
    private static final String SLAVE_HAS_SWITCHES = "has_switches";
    private static final String SLAVE_HAS_DIMMERS = "has_dimmers";
    private static final String SLAVE_IN_GROUP = "slave_in_grp";
    private static final String SLAVE_TOKEN = "slave_token";
    private static final String SLAVE_TOPIC = "slave_topic";
    private static final String SLAVE_USERTYPE = "slave_utype";
    private static final String SLAVE_TOTAL_SWITCH="slave_tsw";
    //define columns for TABLE_SWITCH_ICONS here
    private static final String SWICON_GEN_ID = "id";
    private static final String SWICON_ON = "sw_on";
    private static final String SWICON_OFF = "sw_off";
    //define columns for TABLE_SWITCHEs here
    private static final String SWITCH_GEN_ID = "id";
    private static final String SWITCH_BUTTON_NUM = "sw_btn_num";
    private static final String SWITCH_NAME = "sw_name";
    private static final String IS_SWITCH = "isSwitch";
    private static final String SWITCH_IS_ON = "isSwitchOn";
    private static final String SWITCH_IS_ADDED = "isSwitchAdded";
    private static final String SWITCH_IN_SLAVE = "inSlave";
    private static final String SWITCH_IN_GRP = "inGroup";
    private static final String SWITCH_IS_FAV = "isFav";
    private static final String DIMMER_VALUE = "dimmerValue";
    private static final String SWITCH_ICON = "sw_icon";
    private static final String SWITCH_HAS_SCHEDULE = "sw_has_schdule";
    private static final String SWITCH_USERLOCK = "sw_userlock";
    private static final String SWITCH_TOUCHLOCK = "sw_touchlock";
    //define columns for TABLE_SCENE_SWITCHEs here
    private static final String SCENE_SWITCH_GEN_ID = "id";
    private static final String SCENE_SWITCH_SCENE_ID = "scene_id";
    private static final String SCENE_SWITCH_BUTTON_NUM = "sw_btn_num";
    private static final String SCENE_SWITCH_NAME = "sw_name";
    private static final String SCENE_IS_SWITCH = "isSwitch";
    private static final String SCENE_SWITCH_IS_ON = "isSwitchOn";
    private static final String SCENE_SWITCH_IN_SLAVE = "inSlave";
    private static final String SCENE_SWITCH_IN_GRP = "inGroup";
    private static final String SCENE_DIMMER_VALUE = "dimmerValue";
    //define columns for TABLE_DIMMERs here
    private static final String DIMMER_GEN_ID = "id";
    private static final String DIMMER_NAME = "dm_name";
    private static final String DIMMER_IS_ADDED = "isDimmerAdded";
    private static final String DIMMER_IN_SLAVE = "inSlave";
    private static final String DIMMER_IN_GRP = "inGroup";
    private static final String DIMMER_IS_FAV = "isFav";
    //define columns for TABLE_SCENES here
    private static final String SCENE_GEN_ID = "id";
    private static final String SCENE_NAME = "scn_name";
    private static final String SCENE_FOR_GROUP = "scn_grp";
    //define columns for TABLE_SCHEDULES here
    private static final String SCHEDULE_GEN_ID = "id";
    private static final String SCH_SWITCH_ID = "sw_id";
    private static final String SCH_SLAVE_ID = "sw_slave";
    private static final String SCH_DAYS = "sw_days";
    private static final String SCH_REPEAT = "sw_repeat";
    private static final String SCH_IS_REPEAT = "sw_is_repeat";
    private static final String SCH_IS_DAILY = "sw_daily";
    private static final String SCH_IS_ONCE = "sw_once";
    private static final String SCH_SW_BTN_NUM = "sw_btn_num";
    private static final String SCH_IS_ON = "sw_on";
    private static final String SCH_IS_ENABLE = "sw_enabled";
    private static final String SCH_TIME = "sw_time";
    private static final String SCH_SLOT_NUM = "sw_slot_num";
    String[] switch_names = {"Bulb 1", "Refrigerator", "Fan 1", "Motor", "Bulb 2", "AC 1", "AC 2", "Tubelight 1", "Fan 2", "TV", "Oven", "Refrigerator", "Fan 3", "Lamp 1"};
    String[] dimmer_names = {"Dimmer 1", "Dimmer 2", "Dimmer 3"};
    int[] sw_icons_on = {R.drawable.fluorescent_bulb_on, R.drawable.idea_on, R.drawable.lamp_on,
            R.drawable.spiral_bulb_on, R.drawable.fridge_on, R.drawable.microwave_on,
            R.drawable.air_conditioner_on, R.drawable.stepper_motor_on, R.drawable.fan_on};
    int[] sw_icons_off = {R.drawable.fluorescent_bulb_off, R.drawable.idea_off, R.drawable.lamp_off,
            R.drawable.spiral_bulb_off, R.drawable.fridge_off, R.drawable.microwave_off,
            R.drawable.air_conditioner_off, R.drawable.stepper_motor_off, R.drawable.fan_off};
    private Context context;

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.setLocale(Locale.ENGLISH);
        db.setLockingEnabled(true);

        String createTableGroup = "CREATE TABLE " + TABLE_GROUPS + "(" +
                GROUP_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                GROUP_NAME + " TEXT," + GROUP_IMAGE + " TEXT," + GROUP_HAS_SWITCHES + " TEXT" + ")";

        String createTableMaster = "CREATE TABLE " + TABLE_MASTER + "(" +
                MASTER_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MASTER_NAME + " TEXT," + MASTER_TYPE + " TEXT, " + MASTER_TOPIC + " TEXT, " + MASTER_ENCKEY + " TEXT," + MASTER_USERTYPE + " TEXT," + MASTER_ID + " TEXT, " + MASTER_IP + " TEXT" + ")";

        String createTableSwitchIcons = "CREATE TABLE " + TABLE_SWITCH_ICONS + "(" +
                SWICON_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SWICON_ON + " INT," + SWICON_OFF + " INT" + ")";

        String createTableSlave = "CREATE TABLE " + TABLE_SLAVES + "(" +
                SLAVE_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SLAVE_NAME + " TEXT," + SLAVE_HAS_SWITCHES + " TEXT," + SLAVE_HAS_DIMMERS + " TEXT," +
                SLAVE_HEX_ID + " TEXT," + SLAVE_IN_GROUP + " INT," + SLAVE_TOKEN + " TEXT, " + SLAVE_TOPIC + " TEXT, " + SLAVE_USERTYPE + " TEXT,"+ SLAVE_TOTAL_SWITCH+ " INT" + ")";

        String createTableSwitch = "CREATE TABLE " + TABLE_SWITCHES + "(" +
                SWITCH_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SWITCH_NAME + " TEXT," + IS_SWITCH + " TEXT," + SWITCH_IS_ON + " INT," + SWITCH_IS_ADDED + " INT," + SWITCH_IN_SLAVE + " INT," + SWITCH_IN_GRP + " INT,"
                + SWITCH_IS_FAV + " INT," + DIMMER_VALUE + " INT," + SWITCH_BUTTON_NUM + " TEXT," + SWITCH_ICON + " INT," + SWITCH_HAS_SCHEDULE + " INT," + SWITCH_USERLOCK + " TEXT," + SWITCH_TOUCHLOCK + " TEXT" + ")";

        String createTableDimmer = "CREATE TABLE " + TABLE_DIMMERS + "(" +
                DIMMER_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DIMMER_NAME + " TEXT," + DIMMER_VALUE + " INT," + DIMMER_IS_ADDED + " INT," + DIMMER_IN_SLAVE + " INT," + DIMMER_IN_GRP + " INT,"
                + DIMMER_IS_FAV + " INT" + ")";

        String createTableScene = "CREATE TABLE " + TABLE_SCENES + "(" + SCENE_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + SCENE_NAME + " TEXT," + SCENE_FOR_GROUP + " INT" + ")";

        String createTableSceneSwitch = "CREATE TABLE " + TABLE_SCENE_SWITCH + "(" + SCENE_SWITCH_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + SCENE_SWITCH_SCENE_ID + " INT," + SCENE_SWITCH_BUTTON_NUM + " TEXT," + SCENE_SWITCH_IN_SLAVE + " TEXT," + SCENE_SWITCH_IN_GRP + " INT," + SCENE_SWITCH_IS_ON + " INT," + SCENE_DIMMER_VALUE + " INT," + SCENE_SWITCH_NAME + " TEXT," + SCENE_IS_SWITCH + " TEXT" + ")";

        String createTableSchedule = "CREATE TABLE " + TABLE_SCHEDULE + "(" + SCHEDULE_GEN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + SCH_SWITCH_ID + " INT," + SCH_SW_BTN_NUM + " TEXT," + SCH_SLAVE_ID + " TEXT," + SCH_DAYS + " TEXT," + SCH_REPEAT + " INT," + SCH_IS_REPEAT + " INT," + SCH_IS_DAILY + " INT," + SCH_IS_ONCE + " INT," + SCH_IS_ON + " INT," + SCH_IS_ENABLE + " INT," + SCH_TIME + " TEXT," + SCH_SLOT_NUM + " TEXT" + ")";


        //create tables
        db.execSQL(createTableGroup);
        db.execSQL(createTableMaster);
        db.execSQL(createTableSwitchIcons);
        db.execSQL(createTableSlave);
        db.execSQL(createTableSwitch);
        db.execSQL(createTableScene);
        db.execSQL(createTableSceneSwitch);
        db.execSQL(createTableSchedule);
        //db.execSQL(createTableDimmer);

        //add Default data
        //addDefaultSwitches(db);
        addDefaultPlusIcon(db);
        addDefaultSwitchIcons(db);
        addDefaultScenes(db);
        //addDefaultDimmers(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MASTER);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SLAVES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SWITCHES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DIMMERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCENE_SWITCH);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SWITCH_ICONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCENES);

        onCreate(db);
    }

    public void setMasterSlaveIP(String slaveHex, String currentIP) {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(MASTER_IP, currentIP);

            db.update(TABLE_MASTER, cv, MASTER_TOPIC + "=?", new String[]{slaveHex});

            db.close();
        }
    }

    public String getSlaveIPAddr(String slaveID) {
        SQLiteDatabase db = this.getReadableDatabase();

        String ipAddress = "";

        Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_IP}, MASTER_TOPIC + "=?", new String[]{slaveID}, null, null, null);

        if (cursor.moveToFirst()) {
            ipAddress = cursor.getString(0);
        }

        cursor.close();
        db.close();

        return ipAddress;
    }

    private void addDefaultSwitchIcons(SQLiteDatabase db) {

        synchronized (Lock){
            for (int i = 0; i < sw_icons_off.length; i++) {
                ContentValues cv = new ContentValues();
                cv.put(SWICON_GEN_ID, i + 1);
                cv.put(SWICON_ON, sw_icons_on[i]);
                cv.put(SWICON_OFF, sw_icons_off[i]);
                db.insert(TABLE_SWITCH_ICONS, null, cv);
            }
        }
    }

    private void addDefaultPlusIcon(SQLiteDatabase db) {

        ContentValues contentValuesSlave = new ContentValues();
        contentValuesSlave.put(SLAVE_GEN_ID, 200);
        contentValuesSlave.put(SLAVE_NAME, "+");
        db.insert(TABLE_SLAVES, null, contentValuesSlave);

        contentValuesSlave.clear();
        contentValuesSlave.put(MASTER_GEN_ID, 200);
        contentValuesSlave.put(MASTER_NAME, "+");
        db.insert(TABLE_MASTER, null, contentValuesSlave);

    }

    public int getLastIDForMaster() {
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            String query = "SELECT * FROM " + TABLE_MASTER + " WHERE " + MASTER_GEN_ID + "!=200";
            Cursor cursor = db.rawQuery(query, null);
            if (cursor.moveToFirst()) {
                int count = cursor.getCount();
                query = "SELECT * FROM " + TABLE_MASTER + " LIMIT " + (count - 1) + ",1";
                cursor = db.rawQuery(query, null);

                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
        }
        return 1;
    }

    private void addDefaultScenes(SQLiteDatabase db) {

        ContentValues cv = new ContentValues();
        cv.put(SCENE_GEN_ID, 1);
        cv.put(SCENE_NAME, "All On");
        cv.put(SCENE_FOR_GROUP, 0);

        db.insert(TABLE_SCENES, null, cv);

        cv.clear();
        cv.put(SCENE_GEN_ID, 2);
        cv.put(SCENE_NAME, "All Off");
        cv.put(SCENE_FOR_GROUP, 0);

        db.insert(TABLE_SCENES, null, cv);
    }

    public List<Bean_Master> getAllMasterDeviceData() {
        List<Bean_Master> masterList = new ArrayList<>();

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            String selectMaster = "SELECT * FROM " + TABLE_MASTER;
            Cursor cursor = db.rawQuery(selectMaster, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Master master = new Bean_Master();
                    master.setId(cursor.getInt(0));
                    master.setName(cursor.getString(1));
                    master.setType(cursor.getString(2));
                    master.setTopic(cursor.getString(3));
                    master.setEnckey(cursor.getString(4));
                    master.setUserType(cursor.getString(5));
                    master.setMasterID(cursor.getString(6));
                    master.setIpAddress(cursor.getString(7));
                    masterList.add(master);
                } while (cursor.moveToNext());
            }
        }

        return masterList;
    }

    public List<Bean_Master> getAllMasterDeviceDataWOADD() {
        List<Bean_Master> masterList = new ArrayList<>();

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            String selectMaster = "SELECT * FROM " + TABLE_MASTER;
            Cursor cursor = db.rawQuery(selectMaster, null);

            if (cursor.moveToFirst()) {
                do {
                    int ID = cursor.getInt(0);
                    if (ID != 200) {
                        Bean_Master master = new Bean_Master();
                        master.setId(cursor.getInt(0));
                        master.setName(cursor.getString(1));
                        master.setType(cursor.getString(2));
                        master.setTopic(cursor.getString(3));
                        master.setEnckey(cursor.getString(4));
                        master.setUserType(cursor.getString(5));
                        master.setMasterID(cursor.getString(6));
                        master.setIpAddress(cursor.getString(7));
                        masterList.add(master);
                    }
                } while (cursor.moveToNext());
            }
        }
        return masterList;
    }

    public List<Bean_SwitchIcons> getAllSwitchIconData() {
        List<Bean_SwitchIcons> iconList = new ArrayList<>();

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            String selectMaster = "SELECT * FROM " + TABLE_SWITCH_ICONS;
            Cursor cursor = db.rawQuery(selectMaster, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_SwitchIcons switchIcons = new Bean_SwitchIcons();
                    switchIcons.setIconid(cursor.getInt(0));
                    switchIcons.setSwOnId(cursor.getInt(1));
                    switchIcons.setSwOffId(cursor.getInt(2));
                    iconList.add(switchIcons);
                } while (cursor.moveToNext());
            }

            cursor.close();
            db.close();
        }

        return iconList;
    }

    public Bean_SwitchIcons getSwitchIconData(int switch_icon) {
        Bean_SwitchIcons switchIcon = new Bean_SwitchIcons();

        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCH_ICONS, new String[]{SWICON_GEN_ID, SWICON_ON, SWICON_OFF}, SWICON_GEN_ID + "=?",
                        new String[]{String.valueOf(switch_icon)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    switchIcon.setIconid(cursor.getInt(0));
                    switchIcon.setSwOnId(cursor.getInt(1));
                    switchIcon.setSwOffId(cursor.getInt(2));
                }

                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e){

                waitFor();
                switchIcon=getSwitchIconData(switch_icon);
            }
        }

        return switchIcon;
    }

    public List<Bean_MasterGroup> getAllMasterGroupData() {
        List<Bean_MasterGroup> masterGroupList = new ArrayList<>();

        synchronized (Lock){
            try{
                String selectQuery = "SELECT * FROM " + TABLE_GROUPS;

                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.rawQuery(selectQuery, null);

                if (cursor.moveToFirst()) {
                    do {
                        Bean_MasterGroup masterGroup = new Bean_MasterGroup();
                        masterGroup.setId(cursor.getInt(0));
                        masterGroup.setName(cursor.getString(1));
                        masterGroup.setImgLocalPath(cursor.getString(2));
                        masterGroup.setHasSwitches(cursor.getString(3));
                        masterGroupList.add(masterGroup);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getAllMasterGroupData();
            }
        }

        return masterGroupList;
    }

    public List<Bean_MasterGroup> getAllMasterGroupDataNoAdd() {
        List<Bean_MasterGroup> masterGroupList = new ArrayList<>();

        synchronized (Lock){
            try{
                String selectQuery = "SELECT * FROM " + TABLE_GROUPS;

                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.rawQuery(selectQuery, null);

                if (cursor.moveToFirst()) {
                    do {
                        if (cursor.getInt(0) != 100) {
                            Bean_MasterGroup masterGroup = new Bean_MasterGroup();
                            masterGroup.setId(cursor.getInt(0));
                            masterGroup.setName(cursor.getString(1));
                            masterGroup.setImgLocalPath(cursor.getString(2));
                            masterGroup.setHasSwitches(cursor.getString(3));
                            masterGroupList.add(masterGroup);
                        }
                    } while (cursor.moveToNext());
                }

                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getAllMasterGroupDataNoAdd();
            }
        }

        return masterGroupList;
    }

    public void addDefaultRows(int[] images, String[] names) {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            for (int i = 0; i < images.length; i++) {
                Bitmap image = BitmapFactory.decodeResource(context.getResources(), images[i]);
                ContentValues contentValues = new ContentValues();
                if (i == (images.length - 1)) {
                    contentValues.put(GROUP_GEN_ID, 100);
                } else {
                    contentValues.put(GROUP_GEN_ID, i + 1);
                }

                contentValues.put(GROUP_NAME, names[i]);

                String imageName=names[i].replace(" ","_")+"_"+(i+1);
                String imagePath= C.saveGroupImageToLocal(image,imageName);

                contentValues.put(GROUP_IMAGE, imagePath);
                contentValues.put(GROUP_HAS_SWITCHES, "0");

                db.insert(TABLE_GROUPS, null, contentValues);
            }

            db.close();
        }
    }


    public int getGroupDataCounts() {
        int count = 0;

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_GROUPS;
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            cursor.close();
            db.close();
        }

        return count;
    }

    public int getSlaveDataCounts() {
        int count = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_SLAVES;
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            cursor.close();
            db.close();
        }

        return count;
    }

    public int getMastersCounts() {
        int count = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_MASTER;
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            cursor.close();
            db.close();
        }

        return count;
    }

    public boolean isSameSlaveName(String slaveName) {
        boolean isSame = false;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_GEN_ID}, SLAVE_NAME + "=? COLLATE NOCASE", new String[]{slaveName},
                    null, null, null, null);
            int count = cursor.getCount();

            if (count == 0) {
                isSame = false;
            } else {
                isSame = true;
            }

            cursor.close();
            db.close();
        }

        return isSame;
    }

    public boolean isSameSlaveId(String slave_hex_id) {
        boolean isSame = false;

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_GEN_ID}, SLAVE_HEX_ID + "=? COLLATE NOCASE", new String[]{slave_hex_id},
                    null, null, null, null);
            int count = cursor.getCount();

            if (count == 0) {
                isSame = false;
            } else {
                isSame = true;
            }

            cursor.close();
            db.close();
        }

        return isSame;
    }

    public boolean isSameMasterName(String masterName) {
        boolean isSame = false;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_NAME}, MASTER_NAME + "=? COLLATE NOCASE", new String[]{masterName},
                        null, null, null, null);
                int count = cursor.getCount();

                if (count == 0) {
                    isSame = false;
                } else {
                    isSame = true;
                }

                cursor.close();
                db.close();
            }
            catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e){
                Log.e("Exception",e.getMessage());
            }

        }

        return isSame;
    }

    public boolean isSameSceneName(String sceneName, int groupid) {
        boolean isSame = false;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SCENES, new String[]{SCENE_GEN_ID}, SCENE_NAME + "=? COLLATE NOCASE AND " + SCENE_FOR_GROUP + "=?", new String[]{sceneName, String.valueOf(groupid)}, null, null, null, null);
            int count = cursor.getCount();

            if (count == 0) {
                isSame = false;
            } else {
                isSame = true;
            }

            cursor.close();
            db.close();
        }

        return isSame;
    }

    public int getGroupLastId() {
        int id = 9, count = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_GROUPS + " WHERE " + SLAVE_GEN_ID + "!=100";
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            selectQuery = "SELECT * FROM " + TABLE_GROUPS + " LIMIT " + (count - 1) + ",1";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
        }

        return id;
    }

    public int getSlaveLastId() {
        int id = 0, count = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_SLAVES + " WHERE " + SLAVE_GEN_ID + "!=200";
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            selectQuery = "SELECT * FROM " + TABLE_SLAVES + " LIMIT " + (count - 1) + ",1";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }
        }

        return id;
    }

    public int getMasterDeviceLastId() {
        int id = 0, count = 0;
        SQLiteDatabase db = this.getWritableDatabase();
        String selectQuery = "SELECT * FROM " + TABLE_MASTER;
        Cursor cursor = db.rawQuery(selectQuery, null);
        count = cursor.getCount();
        selectQuery = "SELECT * FROM " + TABLE_MASTER + " LIMIT " + (count - 2) + ",1";
        cursor = db.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            id = cursor.getInt(0);
        }
        return id;
    }

    public void addMasterGroupItem(Bean_MasterGroup beanMasterGroup) {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put(GROUP_GEN_ID, beanMasterGroup.getId());
            contentValues.put(GROUP_NAME, beanMasterGroup.getName());
            contentValues.put(GROUP_IMAGE, beanMasterGroup.getImgLocalPath());
            contentValues.put(GROUP_HAS_SWITCHES, beanMasterGroup.getHasSwitches());

            db.insert(TABLE_GROUPS, null, contentValues);

            Log.i("GRoup Content", "Data Added...");
            //Toast.makeText(context,""+beanMasterGroup.getId(),Toast.LENGTH_LONG).show();
            db.close();
        }

    }

    public void addSlaveItem(Bean_SlaveGroup bean_slaveGroup) {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValues = new ContentValues();
            contentValues.put(SLAVE_GEN_ID, bean_slaveGroup.getId());
            contentValues.put(SLAVE_NAME, bean_slaveGroup.getName());
            contentValues.put(SLAVE_HAS_SWITCHES, bean_slaveGroup.getHasSwitches());
            contentValues.put(SLAVE_HAS_DIMMERS, bean_slaveGroup.getHasDimmers());
            contentValues.put(SLAVE_HEX_ID, bean_slaveGroup.getHex_id());
            contentValues.put(SLAVE_IN_GROUP, bean_slaveGroup.getMaster_id());
            contentValues.put(SLAVE_TOPIC, bean_slaveGroup.getSlaveTopic());
            contentValues.put(SLAVE_TOKEN, bean_slaveGroup.getSlaveToken());
            contentValues.put(SLAVE_USERTYPE, bean_slaveGroup.getSlaveUserType());

            db.insert(TABLE_SLAVES, null, contentValues);
            db.close();
        }
    }

    public void renameSlave(Bean_SlaveGroup slaveItem) {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValuesGroup = new ContentValues();
            contentValuesGroup.put(SLAVE_NAME, slaveItem.getName());
            db.update(TABLE_SLAVES, contentValuesGroup, SLAVE_GEN_ID + "=? AND " + SLAVE_HEX_ID + "=?",
                    new String[]{String.valueOf(slaveItem.getId()), slaveItem.getHex_id()});

            db.close();
        }
    }

    public void renameSlave(@NonNull String slaveHexID, @NonNull String slaveName) {

        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValuesGroup = new ContentValues();
                contentValuesGroup.put(SLAVE_NAME, slaveName);
                db.update(TABLE_SLAVES, contentValuesGroup, SLAVE_HEX_ID + "=?",
                        new String[]{slaveHexID});

                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                renameSlave( slaveHexID, slaveName);
            }
        }
    }

    public void renameMaster(int master_id, String master_name) {

        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValuesGroup = new ContentValues();
                contentValuesGroup.put(MASTER_NAME, master_name);
                db.update(TABLE_MASTER, contentValuesGroup, MASTER_GEN_ID + "=?",
                        new String[]{String.valueOf(master_id)});

                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                renameMaster(master_id, master_name);
            }
        }
    }

    public void addMasterDeviceItem(Bean_Master bean_master) {

        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                contentValues.put(MASTER_GEN_ID, bean_master.getId());
                contentValues.put(MASTER_NAME, bean_master.getName());
                contentValues.put(MASTER_TYPE, bean_master.getType());
                contentValues.put(MASTER_TOPIC, bean_master.getTopic());
                contentValues.put(MASTER_ENCKEY, bean_master.getEnckey());
                contentValues.put(MASTER_USERTYPE, bean_master.getUserType());
                contentValues.put(MASTER_ID, bean_master.getMasterID());
                contentValues.put(MASTER_IP, bean_master.getIpAddress());

                db.insert(TABLE_MASTER, null, contentValues);
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                addMasterDeviceItem(bean_master);
            }
        }
    }

    public List<Bean_SlaveGroup> getAllSlaveGroupData(int groupid) {
        List<Bean_SlaveGroup> slaveGroupList = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_GEN_ID, SLAVE_NAME, SLAVE_HAS_SWITCHES, SLAVE_HAS_DIMMERS, SLAVE_HEX_ID, SLAVE_TOPIC, SLAVE_TOKEN, SLAVE_USERTYPE}, SLAVE_IN_GROUP + "=? OR " + SLAVE_GEN_ID + "=?", new String[]{String.valueOf(groupid), String.valueOf(200)}, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_SlaveGroup slaveGroup = new Bean_SlaveGroup();
                    slaveGroup.setId(cursor.getInt(0));
                    slaveGroup.setName(cursor.getString(1));
                    slaveGroup.setHasSwitches(cursor.getString(2));
                    slaveGroup.setHasDimmers(cursor.getString(3));
                    slaveGroup.setHex_id(cursor.getString(4));
                    slaveGroup.setSlaveTopic(cursor.getString(5));
                    slaveGroup.setSlaveToken(cursor.getString(6));
                    slaveGroup.setSlaveUserType(cursor.getString(7));
                    slaveGroupList.add(slaveGroup);
                } while (cursor.moveToNext());
            }


            cursor.close();
            db.close();
        }

        return slaveGroupList;
    }

    public List<String> getAllSlaveIDs() {
        List<String> slaveIDs = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_HEX_ID}, null, null, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    String slaveId = cursor.getString(0);

                    if (slaveId != null && !slaveId.isEmpty()) {
                        slaveIDs.add(slaveId);
                    }
                } while (cursor.moveToNext());
            }


            cursor.close();
            db.close();
        }

        return slaveIDs;
    }

    public List<Bean_SlaveGroup> getAllSlaveData() {
        List<Bean_SlaveGroup> slaveGroupList = new ArrayList<>();
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_GEN_ID, SLAVE_NAME, SLAVE_HAS_SWITCHES, SLAVE_HAS_DIMMERS, SLAVE_HEX_ID, SLAVE_TOPIC, SLAVE_TOKEN, SLAVE_USERTYPE}, null, null, null, null, null, null);

                if (cursor.moveToFirst()) {
                    do {
                        Bean_SlaveGroup slaveGroup = new Bean_SlaveGroup();
                        slaveGroup.setId(cursor.getInt(0));
                        slaveGroup.setName(cursor.getString(1));
                        slaveGroup.setHasSwitches(cursor.getString(2));
                        slaveGroup.setHasDimmers(cursor.getString(3));
                        slaveGroup.setHex_id(cursor.getString(4));
                        slaveGroup.setSlaveTopic(cursor.getString(5));
                        slaveGroup.setSlaveToken(cursor.getString(6));
                        slaveGroup.setSlaveUserType(cursor.getString(7));
                        slaveGroupList.add(slaveGroup);
                    } while (cursor.moveToNext());
                }


                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getAllSlaveData();
            }
        }

        return slaveGroupList;
    }

    public String getSlaveHexIDForSwitch(int switch_id) {

        String slave_Hex = "0";
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_IN_SLAVE}, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(switch_id)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    slave_Hex = cursor.getString(0);
                }


                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSlaveHexIDForSwitch(switch_id);
            }
        }

        return slave_Hex;
    }

    public String getSwitchButtonNum(int switch_id) {

        String switch_button = "0";
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_BUTTON_NUM}, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(switch_id)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    switch_button = cursor.getString(0);
                }


                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSwitchButtonNum(switch_id);
            }
        }

        return switch_button;
    }

    public List<Bean_SlaveGroup> getAllSlaveGroupData(String slave_hex) {
        List<Bean_SlaveGroup> slaveGroupList = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_GEN_ID, SLAVE_NAME, SLAVE_HAS_SWITCHES, SLAVE_HAS_DIMMERS, SLAVE_HEX_ID}, SLAVE_GEN_ID + "=?", new String[]{slave_hex}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Bean_SlaveGroup slaveGroup = new Bean_SlaveGroup();
                slaveGroup.setId(cursor.getInt(0));
                slaveGroup.setName(cursor.getString(1));
                slaveGroup.setHasSwitches(cursor.getString(2));
                slaveGroup.setHasDimmers(cursor.getString(3));
                slaveGroup.setHex_id(cursor.getString(4));
                slaveGroupList.add(slaveGroup);
            } while (cursor.moveToNext());
        }


        cursor.close();
        db.close();

        return slaveGroupList;
    }

    public int getMasterGroupIdAtCurrentPosition(int position) {
        int id = 0;
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();
                String selectQuery = "SELECT * FROM " + TABLE_GROUPS + " LIMIT " + position + ",1";
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    id = cursor.getInt(0);
                }
                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterGroupIdAtCurrentPosition( position);
            }
        }

        return id;
    }

    public void deleteMasterGroupByGroupId(int group_id) {
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                db.delete(TABLE_GROUPS, GROUP_GEN_ID + "=?", new String[]{String.valueOf(group_id)});

                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteMasterGroupByGroupId(group_id);
            }
        }

    }

    public void deleteMasterDevice(int master_id) {
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                db.delete(TABLE_MASTER, MASTER_GEN_ID + "=?", new String[]{String.valueOf(master_id)});

                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteMasterDevice( master_id);
            }
        }

    }

    public void deleteSlaveDevice(int device_id, String slave_hex_id) {
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getWritableDatabase();

                db.delete(TABLE_SLAVES, SLAVE_GEN_ID + "=? AND " + SLAVE_HEX_ID + "=?", new String[]{String.valueOf(device_id), slave_hex_id});

                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteSlaveDevice( device_id,  slave_hex_id);
            }
        }


    }

    public int getSlaveIdAtCurrentPosition(int position) {
        int id = 0;
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();
                String selectQuery = "SELECT * FROM " + TABLE_SLAVES + " LIMIT " + position + ",1";
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    id = cursor.getInt(0);
                }
                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSlaveIdAtCurrentPosition(position);
            }
        }
        return id;

    }

    public int getMasterDeviceIdAtCurrentPosition(int position) {
        int id = 0;
        synchronized (Lock){
            try{
                SQLiteDatabase db = this.getReadableDatabase();
                String selectQuery = "SELECT * FROM " + TABLE_MASTER + " LIMIT " + position + ",1";
                Cursor cursor = db.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) {
                    id = cursor.getInt(0);
                }
                cursor.close();
                db.close();
            }catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterDeviceIdAtCurrentPosition(position);
            }
        }

        return id;

    }

    public void addDefaultSwitches(SQLiteDatabase db) {

        for (int i = 1; i <= switch_names.length; i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(SWITCH_GEN_ID, i);
            contentValues.put(SWITCH_NAME, switch_names[i - 1]);
            if (switch_names[i - 1].contains("Fan") || switch_names[i - 1].contains("fan")) {
                contentValues.put(IS_SWITCH, "d");
            } else contentValues.put(IS_SWITCH, "s");
            contentValues.put(SWITCH_IS_ON, 0);
            contentValues.put(SWITCH_IS_ADDED, 0);
            contentValues.put(SWITCH_IS_FAV, 0);
            contentValues.put(DIMMER_VALUE, 0);

            db.insert(TABLE_SWITCHES, null, contentValues);
        }
    }

    public void addDefaultDimmers(SQLiteDatabase db) {

        for (int i = 1; i <= dimmer_names.length; i++) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DIMMER_GEN_ID, i);
            contentValues.put(DIMMER_NAME, dimmer_names[i - 1]);
            contentValues.put(DIMMER_IS_ADDED, 0);
            contentValues.put(DIMMER_VALUE, 0);
            contentValues.put(DIMMER_IS_FAV, 0);

            db.insert(TABLE_DIMMERS, null, contentValues);
        }
    }

    public List<Bean_Switch> getAllUnusedSwitches() {
        List<Bean_Switch> switchList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME}, SWITCH_IS_ADDED + "=?",
                new String[]{"0"}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Bean_Switch beanSwitch = new Bean_Switch();
                beanSwitch.setSwitch_id(cursor.getInt(0));
                beanSwitch.setSwitch_name(cursor.getString(1));

                switchList.add(beanSwitch);
            } while (cursor.moveToNext());

        }

        cursor.close();
        db.close();
        return switchList;
    }

    public List<Bean_Dimmer> getAllUnusedDimmers() {
        List<Bean_Dimmer> dimmerList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_DIMMERS, new String[]{DIMMER_GEN_ID, DIMMER_NAME}, DIMMER_IS_ADDED + "=?",
                new String[]{"0"}, null, null, null, null);

        if (cursor.moveToFirst()) {

            do {
                Bean_Dimmer beanDimmer = new Bean_Dimmer();
                beanDimmer.setDimmer_id(cursor.getInt(0));
                beanDimmer.setDimmer_name(cursor.getString(1));

                dimmerList.add(beanDimmer);
            } while (cursor.moveToNext());

        }
        cursor.close();
        db.close();
        return dimmerList;
    }

    public void addSwitchtoGroup(int groupid, List<Bean_Switch> checkedSwitches) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                for (int i = 0; i < checkedSwitches.size(); i++) {
                    Bean_Switch beanSwitch = checkedSwitches.get(i);

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(SWITCH_IN_SLAVE, beanSwitch.getSwitchInSlave());
                    contentValues.put(SWITCH_NAME, beanSwitch.getSwitch_name());
                    contentValues.put(SWITCH_IS_ADDED, 1);
                    contentValues.put(IS_SWITCH, beanSwitch.getIsSwitch());
                    contentValues.put(DIMMER_VALUE, beanSwitch.getDimmerValue());
                    contentValues.put(SWITCH_IN_GRP, groupid);
                    contentValues.put(SWITCH_IS_ON, beanSwitch.getIsSwitchOn());
                    contentValues.put(SWITCH_IS_FAV, 0);
                    contentValues.put(SWITCH_BUTTON_NUM, beanSwitch.getSwitch_btn_num());
                    contentValues.put(SWITCH_ICON, beanSwitch.getSwitch_icon());
                    contentValues.put(SWITCH_USERLOCK, beanSwitch.getUserLock());
                    contentValues.put(SWITCH_TOUCHLOCK, beanSwitch.getTouchLock());

                    db.insert(TABLE_SWITCHES, null, contentValues);
                }

                int hasSwitchesInGroupID = hasSwitchesInGroup(groupid);
                hasSwitchesInGroupID += checkedSwitches.size();
                Log.e("Switch in grp " + groupid, "" + hasSwitchesInGroupID);

                updateGrouphasSwitches(groupid, hasSwitchesInGroupID);

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                addSwitchtoGroup(groupid, checkedSwitches);
            }
        }

    }

    private void updateGrouphasSwitches(int groupid, int hasSwitchesInGroupID) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValuesGroup = new ContentValues();
                contentValuesGroup.put(GROUP_HAS_SWITCHES, hasSwitchesInGroupID);
                db.update(TABLE_GROUPS, contentValuesGroup, GROUP_GEN_ID + "=?", new String[]{String.valueOf(groupid)});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                updateGrouphasSwitches(groupid, hasSwitchesInGroupID);
            }
        }

    }

    public void addDimmerstoGroup(int groupid, List<Bean_Dimmer> checkedDimmers) {
        List<Bean_Dimmer> dimmerList = checkedDimmers;

        SQLiteDatabase db = this.getWritableDatabase();

        for (int i = 0; i < dimmerList.size(); i++) {
            Bean_Dimmer beanDimmer = dimmerList.get(i);

            ContentValues contentValues = new ContentValues();
            contentValues.put(DIMMER_IS_ADDED, 1);
            contentValues.put(DIMMER_IN_GRP, groupid);

            db.update(TABLE_DIMMERS, contentValues, DIMMER_GEN_ID + "=?", new String[]{String.valueOf(beanDimmer.getDimmer_id())});
        }

        int hasSwitchesInGroup = hasSwitchesInGroup(groupid);
        hasSwitchesInGroup += dimmerList.size();
        ContentValues contentValuesGroup = new ContentValues();
        contentValuesGroup.put(GROUP_HAS_SWITCHES, hasSwitchesInGroup);

        db.update(TABLE_GROUPS, contentValuesGroup, GROUP_GEN_ID + "=?", new String[]{String.valueOf(groupid)});

        db.close();
    }

    public List<Bean_Switch> getAllSwitchesByGroupId(int groupid) {
        List<Bean_Switch> switchList = new ArrayList<>();
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME, SWITCH_IS_ON, SWITCH_IN_GRP, SWITCH_IS_FAV, IS_SWITCH,
                                DIMMER_VALUE, SWITCH_IN_SLAVE, SWITCH_BUTTON_NUM, SWITCH_ICON, SWITCH_USERLOCK, SWITCH_TOUCHLOCK, SWITCH_HAS_SCHEDULE},
                        SWITCH_IN_GRP + "=? AND " + SWITCH_IS_ADDED + "=?",
                        new String[]{String.valueOf(groupid), String.valueOf(1)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    do {
                        Bean_Switch beanSwitch = new Bean_Switch();
                        beanSwitch.setSwitch_id(cursor.getInt(0));
                        beanSwitch.setSwitch_name(cursor.getString(1));
                        beanSwitch.setIsSwitchOn(cursor.getInt(2));
                        beanSwitch.setSwitchInGroup(cursor.getInt(3));
                        beanSwitch.setIsFavourite(cursor.getInt(4));
                        beanSwitch.setIsSwitch(cursor.getString(5));
                        beanSwitch.setDimmerValue(cursor.getInt(6));
                        beanSwitch.setSwitchInSlave(cursor.getString(7));
                        beanSwitch.setSwitch_btn_num(cursor.getString(8));
                        beanSwitch.setSwitch_icon(cursor.getInt(9));
                        beanSwitch.setUserLock(cursor.getString(10));
                        beanSwitch.setTouchLock(cursor.getString(11));
                        beanSwitch.setHasSchedule(cursor.getInt(12));
                        switchList.add(beanSwitch);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                //waitFor();
                //getAllSwitchesByGroupId(groupid);
            }
        }

        return switchList;
    }

    public Bean_Switch getSingleSwitchesByGroupId(int switch_id) {
        Bean_Switch beanSwitch = new Bean_Switch();
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME, SWITCH_IS_ON, SWITCH_IN_GRP, SWITCH_IS_FAV, IS_SWITCH,
                                DIMMER_VALUE, SWITCH_IN_SLAVE, SWITCH_BUTTON_NUM, SWITCH_ICON},
                        SWITCH_GEN_ID + "=?",
                        new String[]{String.valueOf(switch_id)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    beanSwitch.setSwitch_id(cursor.getInt(0));
                    beanSwitch.setSwitch_name(cursor.getString(1));
                    beanSwitch.setIsSwitchOn(cursor.getInt(2));
                    beanSwitch.setSwitchInGroup(cursor.getInt(3));
                    beanSwitch.setIsFavourite(cursor.getInt(4));
                    beanSwitch.setIsSwitch(cursor.getString(5));
                    beanSwitch.setDimmerValue(cursor.getInt(6));
                    beanSwitch.setSwitchInSlave(cursor.getString(7));
                    beanSwitch.setSwitch_btn_num(cursor.getString(8));
                    beanSwitch.setSwitch_icon(cursor.getInt(9));
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSingleSwitchesByGroupId(switch_id);
            }
        }


        return beanSwitch;
    }

    public List<Bean_Dimmer> getAllDimmersByGroupId(int groupid) {
        List<Bean_Dimmer> dimmerList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_DIMMERS, new String[]{DIMMER_GEN_ID, DIMMER_NAME, DIMMER_VALUE, DIMMER_IN_GRP, DIMMER_IS_FAV},
                DIMMER_IN_GRP + "=? AND " + DIMMER_IS_ADDED + "=?",
                new String[]{String.valueOf(groupid), String.valueOf(1)}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Bean_Dimmer beanDimmer = new Bean_Dimmer();
                beanDimmer.setDimmer_id(cursor.getInt(0));
                beanDimmer.setDimmer_name(cursor.getString(1));
                beanDimmer.setDimmerValue(cursor.getInt(2));
                beanDimmer.setDimmerInGroup(cursor.getInt(3));
                beanDimmer.setIsFavourite(cursor.getInt(4));

                dimmerList.add(beanDimmer);
            } while (cursor.moveToNext());
        }

        return dimmerList;
    }

    public String getGroupnameById(int groupid) {
        String groupname = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_GROUPS, new String[]{GROUP_NAME}, GROUP_GEN_ID + "=?", new String[]{String.valueOf(groupid)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    groupname = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getGroupnameById(groupid);
            }
        }


        return groupname;
    }

    public String getMasterNameById(int masterId) {
        String mastername = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_NAME}, MASTER_GEN_ID + "=?", new String[]{String.valueOf(masterId)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    mastername = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterNameById(masterId);
            }
        }

        return mastername;
    }

    public String getMasterIPById(int masterId) {
        String masterIP = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_IP}, MASTER_GEN_ID + "=?", new String[]{String.valueOf(masterId)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    masterIP = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterIPById(masterId);
            }
        }

        return masterIP;
    }

    public String getMasterIPBySlaveID(String slaveID) {
        String masterIP = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_IP}, MASTER_ID + "=?", new String[]{slaveID},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    masterIP = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterIPBySlaveID(slaveID);
            }
        }

        return masterIP;
    }

    public String getEncryptionKeyBySlaveID(String slaveID) {
        String encryptionKey = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_MASTER, new String[]{MASTER_ENCKEY}, MASTER_ID + "=?", new String[]{slaveID},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    encryptionKey = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getEncryptionKeyBySlaveID(slaveID);
            }
        }

        return encryptionKey;
    }

    public String getMasterNameBySlaveHexID(String slaveID) {
        String mastername = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(true, TABLE_SLAVES, new String[]{SLAVE_IN_GROUP}, SLAVE_HEX_ID + "=?", new String[]{slaveID}, null, null, null, null);

                int masterId = 0;
                if (cursor.moveToFirst()) {
                    masterId = cursor.getInt(0);
                }

                cursor = db.query(TABLE_MASTER, new String[]{MASTER_NAME}, MASTER_GEN_ID + "=?", new String[]{String.valueOf(masterId)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    mastername = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getMasterNameBySlaveHexID(slaveID);
            }
        }

        return mastername;
    }

    public String getDeviceName(int deviceId) {
        String mastername = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_NAME}, SLAVE_GEN_ID + "=?", new String[]{String.valueOf(deviceId)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    mastername = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getDeviceName(deviceId);
            }
        }

        return mastername;
    }

    public void setFavouriteSwitchById(int switchId, boolean fav) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                if (fav) contentValues.put(SWITCH_IS_FAV, 1);
                else contentValues.put(SWITCH_IS_FAV, 0);

                db.update(TABLE_SWITCHES, contentValues, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(switchId)});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                setFavouriteSwitchById(switchId, fav);
            }
        }
    }

    public void setSwitchIsOnById(int switchId, boolean onoff) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = SQLiteDatabase.openDatabase(DATABASE_NAME, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS);

                ContentValues contentValues = new ContentValues();
                if (onoff) contentValues.put(SWITCH_IS_ON, 1);
                else contentValues.put(SWITCH_IS_ON, 0);

                db.update(TABLE_SWITCHES, contentValues, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(switchId)});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                setSwitchIsOnById(switchId, onoff);
            }
        }

    }

    public void setFavouriteDimmerById(int dimmerId, boolean fav) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                if (fav) contentValues.put(DIMMER_IS_FAV, 1);
                else contentValues.put(DIMMER_IS_FAV, 0);

                db.update(TABLE_DIMMERS, contentValues, DIMMER_GEN_ID + "=?", new String[]{String.valueOf(dimmerId)});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                setFavouriteDimmerById(dimmerId, fav);
            }
        }


    }

    public void setDimmerValue(int dimmerId, int dimmerValue) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues contentValues = new ContentValues();
                contentValues.put(DIMMER_VALUE, dimmerValue);

                db.update(TABLE_SWITCHES, contentValues, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(dimmerId)});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                setDimmerValue(dimmerId, dimmerValue);
            }
        }

    }

    public int hasSwitchesInGroup(int groupid) {
        int hasSwitches = 0;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_GROUPS, new String[]{GROUP_HAS_SWITCHES}, GROUP_GEN_ID + "=?", new String[]{String.valueOf(groupid)},
                        null, null, null, null);

                if (cursor.moveToFirst()) {
                    hasSwitches = Integer.parseInt(cursor.getString(0));
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                hasSwitchesInGroup(groupid);
            }
        }

        return hasSwitches;
    }

    public int getSwitchesInGroup(int groupid) {
        int switches = 0;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID}, SWITCH_IN_GRP + "=?", new String[]{String.valueOf(groupid)},
                        null, null, null, null);
                switches = cursor.getCount();

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSwitchesInGroup(groupid);
            }
        }

        return switches;
    }

    public int getDimmersInGroup(int groupid) throws SQLiteCantOpenDatabaseException {
        int dimmers = 0;

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_DIMMERS, new String[]{DIMMER_GEN_ID}, DIMMER_IN_GRP + "=?", new String[]{String.valueOf(groupid)},
                null, null, null, null);
        dimmers = cursor.getCount();

        cursor.close();
        db.close();
        return dimmers;
    }

    public int getSwitchesInFavoutite() {
        int switches = 0;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID}, SWITCH_IS_FAV + "=?", new String[]{String.valueOf(1)},
                        null, null, null, null);
                switches = cursor.getCount();

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSwitchesInFavoutite();
            }
        }

        return switches;
    }

    public int getDimmersInFavourite() {
        int dimmers = 0;

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_DIMMERS, new String[]{DIMMER_GEN_ID}, DIMMER_IS_FAV + "=?", new String[]{String.valueOf(1)},
                null, null, null, null);
        dimmers = cursor.getCount();

        cursor.close();
        db.close();
        return dimmers;
    }

    public List<Bean_Switch> getAllSwitchesInFavourite() throws SQLiteCantOpenDatabaseException {
        List<Bean_Switch> switchList = new ArrayList<>();
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME, SWITCH_IS_ON, SWITCH_IN_GRP,
                                SWITCH_IS_FAV, IS_SWITCH, DIMMER_VALUE, SWITCH_IN_SLAVE, SWITCH_BUTTON_NUM, SWITCH_ICON, SWITCH_USERLOCK, SWITCH_TOUCHLOCK, SWITCH_HAS_SCHEDULE},
                        SWITCH_IS_FAV + "=?",
                        new String[]{String.valueOf(1)}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    do {
                        Bean_Switch beanSwitch = new Bean_Switch();
                        beanSwitch.setSwitch_id(cursor.getInt(0));
                        beanSwitch.setSwitch_name(cursor.getString(1));
                        beanSwitch.setIsSwitchOn(cursor.getInt(2));
                        beanSwitch.setSwitchInGroup(cursor.getInt(3));
                        beanSwitch.setIsFavourite(cursor.getInt(4));
                        beanSwitch.setIsSwitch(cursor.getString(5));
                        beanSwitch.setDimmerValue(cursor.getInt(6));
                        beanSwitch.setSwitchInSlave(cursor.getString(7));
                        beanSwitch.setSwitch_btn_num(cursor.getString(8));
                        beanSwitch.setSwitch_icon(cursor.getInt(9));
                        beanSwitch.setUserLock(cursor.getString(10));
                        beanSwitch.setTouchLock(cursor.getString(11));
                        beanSwitch.setHasSchedule(cursor.getInt(12));

                        switchList.add(beanSwitch);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getAllSwitchesInFavourite();
            }
        }

        return switchList;
    }

    public List<Bean_Dimmer> getAllDimmersInFavourite() {
        List<Bean_Dimmer> dimmerList = new ArrayList<>();

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_DIMMERS, new String[]{DIMMER_GEN_ID, DIMMER_NAME, DIMMER_VALUE, DIMMER_IN_GRP, DIMMER_IS_FAV},
                DIMMER_IS_FAV + "=?",
                new String[]{String.valueOf(1)}, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                Bean_Dimmer beanDimmer = new Bean_Dimmer();
                beanDimmer.setDimmer_id(cursor.getInt(0));
                beanDimmer.setDimmer_name(cursor.getString(1));
                beanDimmer.setDimmerValue(cursor.getInt(2));
                beanDimmer.setDimmerInGroup(cursor.getInt(3));
                beanDimmer.setIsFavourite(cursor.getInt(4));

                dimmerList.add(beanDimmer);
            } while (cursor.moveToNext());
        }

        return dimmerList;
    }

    public int getUnusedSwitchesCount() {
        int hasSwitches = 0;

        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID}, SWITCH_IS_ADDED + "=?", new String[]{"0"},
                null, null, null, null);

        hasSwitches = cursor.getCount();
        cursor.close();
        db.close();

        return hasSwitches;
    }

    public void removeSwitchFromGroup(int switchID) throws SQLiteCantOpenDatabaseException {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            db.delete(TABLE_SWITCHES, SWITCH_GEN_ID + "=?", new String[]{String.valueOf(switchID)});

            db.close();
        }

    }

    public String getSlaveHexIdAtCurrentPosition(int position) throws SQLiteCantOpenDatabaseException {
        String hex_id = "";

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_SLAVES + " LIMIT " + position + ",1";
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                hex_id = cursor.getString(4);
            }
            cursor.close();
            db.close();
        }
        return hex_id;
    }

    public String getSlaveHexIdForMaster(int masterID) throws SQLiteCantOpenDatabaseException {
        String hex_id = "";
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_SLAVES + " WHERE " + SLAVE_IN_GROUP + "=" + masterID;
            Cursor cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                hex_id = cursor.getString(4);
            }
            cursor.close();
            db.close();
        }

        return hex_id;
    }

    public void renameSwitch(int group_id, int switch_id, String switch_name) throws SQLiteCantOpenDatabaseException {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues contentValuesGroup = new ContentValues();
            contentValuesGroup.put(SWITCH_NAME, switch_name);
            db.update(TABLE_SWITCHES, contentValuesGroup, SWITCH_GEN_ID + "=? AND " + SWITCH_IN_GRP + "=?",
                    new String[]{String.valueOf(switch_id), String.valueOf(group_id)});

            db.close();
        }

    }

    public void changeSwitchType(int groupid, int switchID, String switchType) throws SQLiteCantOpenDatabaseException {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(IS_SWITCH, switchType);

        db.update(TABLE_SWITCHES, cv, SWITCH_GEN_ID + "=? AND " + SWITCH_IN_GRP + "=?",
                new String[]{String.valueOf(switchID), String.valueOf(groupid)});

        db.close();
    }

    public void changeSwitchIcon(int groupid, int switchID, int switchIcon) throws SQLiteCantOpenDatabaseException {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SWITCH_ICON, switchIcon);

            db.update(TABLE_SWITCHES, cv, SWITCH_GEN_ID + "=? AND " + SWITCH_IN_GRP + "=?",
                    new String[]{String.valueOf(switchID), String.valueOf(groupid)});

            db.close();
        }

    }

    public List<String> getSlaveHexIdsForGroup(int groupid) throws SQLiteCantOpenDatabaseException {
        List<String> slave_ids = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(true, TABLE_SWITCHES, new String[]{SWITCH_IN_SLAVE}, SWITCH_IN_GRP + "=?", new String[]{String.valueOf(groupid)}, SWITCH_IN_SLAVE, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    slave_ids.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        }

        return slave_ids;
    }

    public List<String> getSlaveHexIdsForFavourite() throws SQLiteCantOpenDatabaseException {
        List<String> slave_ids = new ArrayList<>();

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(true, TABLE_SWITCHES, new String[]{SWITCH_IN_SLAVE}, SWITCH_IS_FAV + "=?", new String[]{String.valueOf(1)}, SWITCH_IN_SLAVE, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    slave_ids.add(cursor.getString(0));
                } while (cursor.moveToNext());
            }
        }

        return slave_ids;
    }

    public List<Bean_Switch> getAllSwitches(int groupid, String slave_hex_id) throws SQLiteCantOpenDatabaseException {
        List<Bean_Switch> switchList = new ArrayList<>();

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME, SWITCH_IS_ON, SWITCH_IN_GRP, SWITCH_IS_FAV, IS_SWITCH,
                            DIMMER_VALUE, SWITCH_IN_SLAVE, SWITCH_BUTTON_NUM, SWITCH_ICON},
                    SWITCH_IN_GRP + "=? AND " + SWITCH_IS_ADDED + "=? AND " + SWITCH_IN_SLAVE + "=?",
                    new String[]{String.valueOf(groupid), String.valueOf(1), slave_hex_id}, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Switch beanSwitch = new Bean_Switch();
                    beanSwitch.setSwitch_id(cursor.getInt(0));
                    beanSwitch.setSwitch_name(cursor.getString(1));
                    beanSwitch.setIsSwitchOn(cursor.getInt(2));
                    beanSwitch.setSwitchInGroup(cursor.getInt(3));
                    beanSwitch.setIsFavourite(cursor.getInt(4));
                    beanSwitch.setIsSwitch(cursor.getString(5));
                    beanSwitch.setDimmerValue(cursor.getInt(6));
                    beanSwitch.setSwitchInSlave(cursor.getString(7));
                    beanSwitch.setSwitch_btn_num(cursor.getString(8));
                    beanSwitch.setSwitch_icon(cursor.getInt(9));

                    switchList.add(beanSwitch);
                } while (cursor.moveToNext());
            }

            cursor.close();
            db.close();
        }

        return switchList;
    }

    public void updateSwitchItemAsLive(Bean_Switch switchLive) throws SQLiteCantOpenDatabaseException {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(SWITCH_IS_ON, switchLive.getIsSwitchOn());

        db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?", new String[]{switchLive.getSwitch_btn_num(), switchLive.getSwitchInSlave()});

        db.close();
    }

    public void updateSwitch(int groupid, String slave_id, String button_num, int onOff, int dval, String type, String userLock, String touchLock) throws SQLiteCantOpenDatabaseException {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SWITCH_IS_ON, onOff);
            cv.put(DIMMER_VALUE, dval);
            cv.put(IS_SWITCH, type);
            cv.put(SWITCH_USERLOCK, userLock);
            cv.put(SWITCH_TOUCHLOCK, touchLock);

            db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_GRP + "=? AND " + SWITCH_IN_SLAVE + "=?",
                    new String[]{button_num, String.valueOf(groupid), slave_id});

            db.close();
        }
    }

    public void setSwitchHasSchedule(String slaveID, String buttonID, boolean hasSchedule) {
        synchronized (Lock) {
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SWITCH_HAS_SCHEDULE, hasSchedule ? 1 : 0);

            db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?", new String[]{buttonID, slaveID});

            db.close();
        }
    }

    public void updateSwitchInFavourite(String slave_id, String button_num, int onOff, int dval, String type, String userLock, String touchLock) throws SQLiteCantOpenDatabaseException {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SWITCH_IS_ON, onOff);
            cv.put(DIMMER_VALUE, dval);
            cv.put(IS_SWITCH, type);
            cv.put(SWITCH_USERLOCK, userLock);
            cv.put(SWITCH_TOUCHLOCK, touchLock);

            db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?",
                    new String[]{button_num, slave_id});

            db.close();
        }

    }

    public List<Bean_Switch> getAllSwitchesByFavourite(String slave_hex_id) throws SQLiteCantOpenDatabaseException {
        List<Bean_Switch> switchList = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_GEN_ID, SWITCH_NAME, SWITCH_IS_ON, SWITCH_IN_GRP,
                            SWITCH_IS_FAV, IS_SWITCH, DIMMER_VALUE, SWITCH_IN_SLAVE, SWITCH_BUTTON_NUM, SWITCH_ICON},
                    SWITCH_IS_FAV + "=? AND " + SWITCH_IN_SLAVE + "=?",
                    new String[]{String.valueOf(1), slave_hex_id}, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Switch beanSwitch = new Bean_Switch();
                    beanSwitch.setSwitch_id(cursor.getInt(0));
                    beanSwitch.setSwitch_name(cursor.getString(1));
                    beanSwitch.setIsSwitchOn(cursor.getInt(2));
                    beanSwitch.setSwitchInGroup(cursor.getInt(3));
                    beanSwitch.setIsFavourite(cursor.getInt(4));
                    beanSwitch.setIsSwitch(cursor.getString(5));
                    beanSwitch.setDimmerValue(cursor.getInt(6));
                    beanSwitch.setSwitchInSlave(cursor.getString(7));
                    beanSwitch.setSwitch_btn_num(cursor.getString(8));
                    beanSwitch.setSwitch_icon(cursor.getInt(9));

                    switchList.add(beanSwitch);
                } while (cursor.moveToNext());
            }
        }

        return switchList;
    }

    public boolean isSwitchAdded(String switch_btn_num, String slave_hex_id) throws SQLiteCantOpenDatabaseException {
        boolean isAdded = false;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_IN_GRP}, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?", new String[]{switch_btn_num, slave_hex_id}, null, null, null, null);

            if (cursor.getCount() > 0) {
                isAdded = true;
            }
        }

        return isAdded;
    }

    public List<Bean_Scenes> getScenesList(int groupid) throws SQLiteCantOpenDatabaseException {
        List<Bean_Scenes> scenesList = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SCENES, new String[]{SCENE_GEN_ID, SCENE_NAME, SCENE_FOR_GROUP}, SCENE_FOR_GROUP + "=?", new String[]{String.valueOf(0)}, null, null, SCENE_GEN_ID + " ASC", null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Scenes scenes = new Bean_Scenes();
                    scenes.setSceneId(cursor.getInt(0));
                    scenes.setSceneName(cursor.getString(1));
                    scenes.setSceneGroup(cursor.getInt(2));

                    scenesList.add(scenes);
                } while (cursor.moveToNext());
            }

            cursor = db.query(TABLE_SCENES, new String[]{SCENE_GEN_ID, SCENE_NAME, SCENE_FOR_GROUP}, SCENE_FOR_GROUP + "=?", new String[]{String.valueOf(groupid)}, null, null, SCENE_GEN_ID + " ASC", null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Scenes scenes = new Bean_Scenes();
                    scenes.setSceneId(cursor.getInt(0));
                    scenes.setSceneName(cursor.getString(1));
                    scenes.setSceneGroup(cursor.getInt(2));

                    scenesList.add(scenes);
                } while (cursor.moveToNext());
            }

            cursor.close();
            db.close();
        }
        return scenesList;
    }

    public boolean addSceneItem(Bean_Scenes scenes) throws SQLiteCantOpenDatabaseException {
        boolean isAdded = false;

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

        /*Cursor cursor=db.query(TABLE_SCENES,null,SCENE_SWITCH_NAME+"=?",new String[]{scenes.getSceneName()},null,null,null);

        if(cursor.getCount()>0){*/
            isAdded = true;
            ContentValues cv = new ContentValues();
            cv.put(SCENE_NAME, scenes.getSceneName());
            cv.put(SCENE_FOR_GROUP, scenes.getSceneGroup());

            db.insert(TABLE_SCENES, null, cv);
            db.close();

            List<Bean_Switch> switchList = getAllSwitchesByGroupId(scenes.getSceneGroup());
            SQLiteDatabase db1 = this.getWritableDatabase();

            for (int i = 0; i < switchList.size(); i++) {
                Bean_Switch bean_switch = switchList.get(i);

                ContentValues contentValues = new ContentValues();
                contentValues.put(SCENE_SWITCH_SCENE_ID, getSceneLastID());
                contentValues.put(SCENE_SWITCH_BUTTON_NUM, bean_switch.getSwitch_btn_num());
                contentValues.put(SCENE_SWITCH_IN_GRP, bean_switch.getSwitchInGroup());
                contentValues.put(SCENE_SWITCH_IN_SLAVE, bean_switch.getSwitchInSlave());
                contentValues.put(SCENE_SWITCH_NAME, bean_switch.getSwitch_name());
                contentValues.put(SCENE_SWITCH_IS_ON, 0);
                contentValues.put(SCENE_IS_SWITCH, bean_switch.getIsSwitch());
                contentValues.put(SCENE_DIMMER_VALUE, 0);

                db1.insert(TABLE_SCENE_SWITCH, null, contentValues);
                Log.e("Values ", "Put " + getSceneLastID());
            }

            db1.close();
        }

        /*}

        cursor.close();*/

        return isAdded;
    }

    public int getLastAddedSceneID() throws SQLiteCantOpenDatabaseException {
        int id = 0;

        id = getSceneLastID();

        return id;
    }

    public void renameSceneItem(int scene_id, String scene_name) throws SQLiteCantOpenDatabaseException {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SCENE_NAME, scene_name);

            db.update(TABLE_SCENES, cv, SCENE_GEN_ID + "=?", new String[]{String.valueOf(scene_id)});

            db.close();
        }

    }

    public void deleteScene(int scene_id) throws SQLiteCantOpenDatabaseException {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            db.delete(TABLE_SCENES, SCENE_GEN_ID + "=?", new String[]{String.valueOf(scene_id)});

            db.close();
        }
    }

    public String getSceneName(int scene_id) throws SQLiteCantOpenDatabaseException {
        String sceneName = "";
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.query(TABLE_SCENES, new String[]{SCENE_NAME}, SCENE_GEN_ID + "=?", new String[]{String.valueOf(scene_id)}, null, null, null, null);

            if (cursor.moveToFirst()) {
                sceneName = cursor.getString(0);
            }
        }

        return sceneName;
    }

    public int getSceneLastID() throws SQLiteCantOpenDatabaseException {
        int count = 0, scene_id = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            String selectQuery = "SELECT * FROM " + TABLE_SCENES;
            Cursor cursor = db.rawQuery(selectQuery, null);
            count = cursor.getCount();
            selectQuery = "SELECT * FROM " + TABLE_SCENES + " LIMIT " + (count - 1) + ",1";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                scene_id = cursor.getInt(0);
            }
        }


        return scene_id;
    }

    public List<Bean_Switch> getSceneSwitches(int scene_id, int groupid) throws SQLiteCantOpenDatabaseException {
        List<Bean_Switch> switchList = new ArrayList<>();
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();

            Cursor cursor = db.query(TABLE_SCENE_SWITCH, new String[]{SCENE_SWITCH_GEN_ID, SCENE_SWITCH_SCENE_ID, SCENE_SWITCH_IN_SLAVE, SCENE_SWITCH_IN_GRP, SCENE_SWITCH_NAME, SCENE_SWITCH_BUTTON_NUM, SCENE_SWITCH_IS_ON, SCENE_IS_SWITCH, SCENE_DIMMER_VALUE}, SCENE_SWITCH_SCENE_ID + "=? AND " + SCENE_SWITCH_IN_GRP + "=?", new String[]{String.valueOf(scene_id), String.valueOf(groupid)}, null, null, null, null);

            if (cursor.moveToFirst()) {
                do {
                    Bean_Switch beanSwitch = new Bean_Switch();
                    beanSwitch.setSwitch_id(cursor.getInt(0));
                    beanSwitch.setSceneid(cursor.getInt(1));
                    beanSwitch.setSwitchInSlave(cursor.getString(2));
                    beanSwitch.setSwitchInGroup(cursor.getInt(3));
                    beanSwitch.setSwitch_name(cursor.getString(4));
                    beanSwitch.setSwitch_btn_num(cursor.getString(5));
                    beanSwitch.setIsSwitchOn(cursor.getInt(6));
                    beanSwitch.setIsSwitch(cursor.getString(7));
                    beanSwitch.setDimmerValue(cursor.getInt(8));
                    switchList.add(beanSwitch);
                } while (cursor.moveToNext());
            }
        }

        return switchList;
    }

    public void updateSceneSwitches(List<Bean_Switch> edited_switches) throws SQLiteCantOpenDatabaseException {

        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            for (int i = 0; i < edited_switches.size(); i++) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(SCENE_SWITCH_IS_ON, edited_switches.get(i).getIsSwitchOn());
                contentValues.put(SCENE_DIMMER_VALUE, edited_switches.get(i).getDimmerValue());

                db.update(TABLE_SCENE_SWITCH, contentValues, SCENE_GEN_ID + "=? AND " + SCENE_SWITCH_SCENE_ID + "=?", new String[]{String.valueOf(edited_switches.get(i).getSwitch_id()), String.valueOf(edited_switches.get(i).getSceneid())});
            }

            db.close();
        }

    }


    public void renameSwitchInScenes(Bean_Switch item, String name) throws SQLiteCantOpenDatabaseException {
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();
            cv.put(SCENE_SWITCH_NAME, name);

            db.update(TABLE_SCENE_SWITCH, cv, SCENE_SWITCH_IN_GRP + "=? AND " + SCENE_SWITCH_IN_SLAVE + "=? AND " + SCENE_SWITCH_BUTTON_NUM + "=?", new String[]{String.valueOf(item.getSwitchInGroup()), item.getSwitchInSlave(), item.getSwitch_btn_num()});

            db.close();
        }
    }

    public int removeSwitchFromScenes(Bean_Switch item) throws SQLiteCantOpenDatabaseException {
        int totalSwitchInGroup = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getWritableDatabase();



            db.delete(TABLE_SCENE_SWITCH, SCENE_SWITCH_IN_GRP + "=? AND " + SCENE_SWITCH_IN_SLAVE + "=? AND " + SCENE_SWITCH_BUTTON_NUM + "=?", new String[]{String.valueOf(item.getSwitchInGroup()), item.getSwitchInSlave(), item.getSwitch_btn_num()});

            Cursor cursor = db.query(TABLE_GROUPS, new String[]{GROUP_HAS_SWITCHES}, GROUP_GEN_ID + "=?", new String[]{String.valueOf(item.getSwitchInGroup())}, null, null, null);

            if (cursor.moveToFirst()) {
                int totalSwitches = Integer.parseInt(cursor.getString(0));
                totalSwitches--;

                ContentValues cv = new ContentValues();
                cv.put(GROUP_HAS_SWITCHES, String.valueOf(totalSwitches));

                totalSwitchInGroup = totalSwitches;

                db.update(TABLE_GROUPS, cv, GROUP_GEN_ID + "=?", new String[]{String.valueOf(item.getSwitchInGroup())});

            }

            cursor.close();
            db.close();
        }

        return totalSwitchInGroup;
    }

    public List<Bean_ScheduleItem> getAllSchedulesForSwitch(int switchID) throws SQLiteCantOpenDatabaseException {
        List<Bean_ScheduleItem> scheduleItemList = new ArrayList<>();

        String sqlQuery = "SELECT * FROM " + TABLE_SCHEDULE + " WHERE " + SCH_SWITCH_ID + "=?";

        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(sqlQuery, new String[]{String.valueOf(switchID)});

            if (cursor.moveToFirst()) {
                do {
                    Bean_ScheduleItem scheduleItem = new Bean_ScheduleItem();
                    scheduleItem.setScheduleID(cursor.getInt(0));
                    scheduleItem.setSwitchID(cursor.getInt(1));
                    scheduleItem.setSwitch_btn_num(cursor.getString(2));
                    scheduleItem.setSlave_id(cursor.getString(3));
                    String days = cursor.getString(4);

                    String[] days_array = new String[days.length()];
                    for (int i = 0; i < days.length(); i++) {
                        days_array[i] = String.valueOf(days.charAt(i));
                    }
                    scheduleItem.setDays(days_array);
                    scheduleItem.setRepeat(cursor.getInt(5));
                    scheduleItem.setRepeated(cursor.getInt(6) != 0);
                    scheduleItem.setDaily(cursor.getInt(7) != 0);
                    scheduleItem.setOnce(cursor.getInt(8) != 0);
                    scheduleItem.setSwitchOn(cursor.getInt(9) != 0);
                    scheduleItem.setSchEnabled(cursor.getInt(10) != 0);
                    scheduleItem.setTime(cursor.getString(11));
                    scheduleItem.setSlot_num(cursor.getString(12));

                    scheduleItemList.add(scheduleItem);
                } while (cursor.moveToNext());
            }
        }
        return scheduleItemList;
    }

    public int getNewIDForSCH() throws SQLiteCantOpenDatabaseException {
        int id = 0;
        synchronized (Lock){
            SQLiteDatabase db = this.getReadableDatabase();
            String selectQuery = "SELECT * FROM " + TABLE_SCHEDULE;
            Cursor cursor = db.rawQuery(selectQuery, null);

            int count = cursor.getCount();
            selectQuery = "SELECT * FROM " + TABLE_SCHEDULE + " LIMIT " + (count - 1) + ",1";
            cursor = db.rawQuery(selectQuery, null);
            if (cursor.moveToFirst()) {
                id = cursor.getInt(0);
            }

            cursor.close();
            db.close();
        }

        return id + 1;
    }

    public void addScheduleItem(Bean_ScheduleItem scheduleItem) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues cv = new ContentValues();

                cv.put(SCHEDULE_GEN_ID, scheduleItem.getScheduleID());
                cv.put(SCH_SWITCH_ID, scheduleItem.getSwitchID());
                cv.put(SCH_SLAVE_ID, scheduleItem.getSlave_id());
                cv.put(SCH_SW_BTN_NUM, scheduleItem.getSwitch_btn_num());

                String days = "";
                for (int i = 0; i < scheduleItem.getDays().length; i++) {
                    days += scheduleItem.getDays()[i];
                }
                cv.put(SCH_DAYS, days);
                cv.put(SCH_IS_ON, (scheduleItem.isSwitchOn()) ? 1 : 0);
                cv.put(SCH_REPEAT, scheduleItem.getRepeat());
                cv.put(SCH_TIME, scheduleItem.getTime());
                cv.put(SCH_IS_ENABLE, (scheduleItem.isSchEnabled() ? 1 : 0));
                cv.put(SCH_IS_REPEAT, (scheduleItem.isRepeated() ? 1 : 0));
                cv.put(SCH_IS_DAILY, (scheduleItem.isDaily() ? 1 : 0));
                cv.put(SCH_IS_ONCE, scheduleItem.isOnce() ? 1 : 0);
                cv.put(SCH_SLOT_NUM, scheduleItem.getSlot_num());

                db.insert(TABLE_SCHEDULE, null, cv);

                Log.e("SCH", "New Item Added");

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                addScheduleItem(scheduleItem);
            }
        }
    }

    public void removeScheduleItem(Bean_ScheduleItem bean_scheduleItem) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                db.delete(TABLE_SCHEDULE, SCHEDULE_GEN_ID + "=?", new String[]{String.valueOf(bean_scheduleItem.getScheduleID())});
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                removeScheduleItem(bean_scheduleItem);
            }
        }

    }

    public void updateScheduleItem(Bean_ScheduleItem scheduleItem) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues cv = new ContentValues();

                cv.put(SCHEDULE_GEN_ID, scheduleItem.getScheduleID());
                cv.put(SCH_SWITCH_ID, scheduleItem.getSwitchID());
                cv.put(SCH_SLAVE_ID, scheduleItem.getSlave_id());
                cv.put(SCH_SW_BTN_NUM, scheduleItem.getSwitch_btn_num());

                String days = "";
                for (int i = 0; i < scheduleItem.getDays().length; i++) {
                    days += scheduleItem.getDays()[i];
                }
                cv.put(SCH_DAYS, days);
                cv.put(SCH_IS_ON, (scheduleItem.isSwitchOn()) ? 1 : 0);
                cv.put(SCH_REPEAT, scheduleItem.getRepeat());
                cv.put(SCH_TIME, scheduleItem.getTime());
                cv.put(SCH_IS_ENABLE, (scheduleItem.isSchEnabled() ? 1 : 0));
                cv.put(SCH_IS_REPEAT, (scheduleItem.isRepeated() ? 1 : 0));
                cv.put(SCH_IS_DAILY, (scheduleItem.isDaily() ? 1 : 0));
                cv.put(SCH_IS_ONCE, scheduleItem.isOnce() ? 1 : 0);
                cv.put(SCH_SLOT_NUM, scheduleItem.getSlot_num());

                db.update(TABLE_SCHEDULE, cv, SCHEDULE_GEN_ID + "=?", new String[]{String.valueOf(scheduleItem.getScheduleID())});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                updateScheduleItem(scheduleItem);
            }
        }

    }

    public boolean isSwitch(String switch_btn_num, String slaveid) {
        boolean isASwitch = false;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{IS_SWITCH}, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?", new String[]{switch_btn_num, slaveid}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    if (cursor.getString(0).equalsIgnoreCase("s"))
                        isASwitch = true;
                }
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                isSwitch(switch_btn_num, slaveid);
            }
        }
        return isASwitch;
    }

    public void deleteAllSwitchesFromMaster(int master_id) {
        synchronized (Lock){
            try {
                List<Bean_SlaveGroup> slaveGroupList = getAllSlaveGroupData(master_id);
                SQLiteDatabase db = this.getWritableDatabase();

                Cursor cursor = null;

                for (int i = 0; i < slaveGroupList.size() - 1; i++) {
                    cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_IN_GRP}, SWITCH_IN_SLAVE + "=?", new String[]{slaveGroupList.get(i).getHex_id()}, null, null, null, null);

                    if (cursor.moveToFirst()) {
                        do {
                            ContentValues contentValuesGroup = new ContentValues();
                            contentValuesGroup.put(GROUP_HAS_SWITCHES, 0);
                            db.update(TABLE_GROUPS, contentValuesGroup, GROUP_GEN_ID + "=?", new String[]{String.valueOf(cursor.getInt(0))});
                        } while (cursor.moveToNext());
                    }
                    String slave_hex = slaveGroupList.get(i).getHex_id();
                    int success = db.delete(TABLE_SWITCHES, SWITCH_IN_SLAVE + "=?", new String[]{slave_hex});
                    db.delete(TABLE_SCHEDULE, SCH_SLAVE_ID + "=?", new String[]{slave_hex});
                    Log.e("Deleted:", slave_hex + " " + success);

                }

                if (cursor != null)
                    cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteAllSwitchesFromMaster(master_id);
            }
        }

    }

    public void deleteAllSwitchesFromGroup(int group_id) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                db.delete(TABLE_SWITCHES, SWITCH_IN_GRP + "=?", new String[]{String.valueOf(group_id)});
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteAllSwitchesFromGroup(group_id);
            }
        }
    }

    public void deleteAllSwitchesFromSlave(String Slave) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                Cursor cursor = db.query(TABLE_SWITCHES, new String[]{SWITCH_IN_GRP}, SWITCH_IN_SLAVE + "=?", new String[]{Slave}, null, null, null, null);

                if (cursor.moveToFirst()) {
                    do {
                        ContentValues contentValuesGroup = new ContentValues();
                        contentValuesGroup.put(GROUP_HAS_SWITCHES, 0);
                        db.update(TABLE_GROUPS, contentValuesGroup, GROUP_GEN_ID + "=?", new String[]{String.valueOf(cursor.getInt(0))});
                    } while (cursor.moveToNext());
                }

                int success = db.delete(TABLE_SWITCHES, SWITCH_IN_SLAVE + "=?", new String[]{Slave});
                Log.e("Deleted:", Slave + " " + success);

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteAllSwitchesFromSlave(Slave);
            }
        }
    }

    public List<Bean_Switch> getSwitchDataBySlave(String slave_hexID) {
        List<Bean_Switch> switchList = new ArrayList<>();
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                String query = "SELECT * FROM " + TABLE_SWITCHES + " WHERE " + SWITCH_IN_SLAVE + "='" + slave_hexID + "' ORDER BY " + SWITCH_BUTTON_NUM + " ASC";

                Cursor cursor = db.rawQuery(query, null);

                if (cursor.moveToFirst()) {
                    do {
                        Bean_Switch bean_switch = new Bean_Switch();
                        bean_switch.setSwitch_btn_num(cursor.getString(9));
                        bean_switch.setUserLock(cursor.getString(12));
                        bean_switch.setTouchLock(cursor.getString(13));

                        switchList.add(bean_switch);
                    } while (cursor.moveToNext());
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSwitchDataBySlave(slave_hexID);
            }
        }
        return switchList;
    }

    public void updateSwitchLocks(Bean_Switch switchItem, boolean touchLock) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues cv = new ContentValues();
                if (touchLock)
                    cv.put(SWITCH_TOUCHLOCK, switchItem.getTouchLock());
                else
                    cv.put(SWITCH_USERLOCK, switchItem.getUserLock());

                db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?",
                        new String[]{switchItem.getSwitch_btn_num(), switchItem.getSwitchInSlave()});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                updateSwitchLocks(switchItem, touchLock);
            }
        }

    }

    public void updateSwitchTypes(String slaveID, String switchButtonNum, String type) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues cv = new ContentValues();
                cv.put(IS_SWITCH, type);

                db.update(TABLE_SWITCHES, cv, SWITCH_BUTTON_NUM + "=? AND " + SWITCH_IN_SLAVE + "=?",
                        new String[]{switchButtonNum, slaveID});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                updateSwitchTypes(slaveID, switchButtonNum, type);
            }
        }
    }

    public void updateScheduleSlot(String slotNumber, String slaveID) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();

                ContentValues cv = new ContentValues();

                cv.put(SCH_SLOT_NUM, "26");
                cv.put(SCH_IS_ENABLE, 0);

                db.update(TABLE_SCHEDULE, cv, SCH_SLOT_NUM + "=? AND " + SCH_SLAVE_ID + "=?", new String[]{slotNumber, slaveID});

                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                updateScheduleSlot(slotNumber, slaveID);
            }
        }
    }

    public boolean isAlreadyScheduleAdded(String slaveID, String slotNum, String switchNum) {
        boolean isAvailable = false;
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Log.e("params", slaveID + " " + slotNum + " " + switchNum);

                Cursor cursor = db.query(TABLE_SCHEDULE, null, SCH_SLAVE_ID + "=? AND " + SCH_SW_BTN_NUM + "=? AND " + SCH_SLOT_NUM + "=?", new String[]{slaveID, switchNum, slotNum}, null, null, null);

                Log.e("Count Sch", "" + cursor.getCount());

                if (cursor.moveToFirst()) {
                    if (cursor.getCount() > 0) {
                        isAvailable = true;
                    }
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                isAlreadyScheduleAdded(slaveID, slotNum, switchNum);
            }
        }
        return isAvailable;
    }

    public String getSlaveTopic(String slaveID) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                String slaveTopic = "";

                Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_TOPIC}, SLAVE_HEX_ID + "=?", new String[]{slaveID}, null, null, null);

                if (cursor.moveToFirst()) {
                    slaveTopic = cursor.getString(0);
                }

                cursor.close();
                db.close();
                return slaveTopic;
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSlaveTopic(slaveID);
            }
        }
        return "";
    }

    public String getSlaveToken(String slaveID) {
        String slaveToken = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();

                Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_TOKEN}, SLAVE_HEX_ID + "=?", new String[]{slaveID}, null, null, null);

                if (cursor.moveToFirst()) {
                    slaveToken = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSlaveToken(slaveID);
            }
        }
        return slaveToken;
    }

    public String getSlaveUserType(String slaveID) {
        String slaveUserType = "";
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getReadableDatabase();


                Cursor cursor = db.query(TABLE_SLAVES, new String[]{SLAVE_USERTYPE}, SLAVE_HEX_ID + "=?", new String[]{slaveID}, null, null, null);

                if (cursor.moveToFirst()) {
                    slaveUserType = cursor.getString(0);
                }

                cursor.close();
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                getSlaveUserType(slaveID);
            }
        }
        return slaveUserType;
    }

    public boolean hasScheduleSet(String slaveID, String switchID) throws SQLiteCantOpenDatabaseException {
        boolean isScheduleSet = false;
        try {
            SQLiteDatabase db = this.getWritableDatabase();

            Cursor cursor = db.query(TABLE_SCHEDULE, null, SCH_SLAVE_ID + "=? AND " + SCH_SWITCH_ID + "=?", new String[]{slaveID, switchID}, null, null, null, null);

            if (cursor.getCount() > 0)
                isScheduleSet = true;

            cursor.close();
            db.close();
        } catch (SQLiteCantOpenDatabaseException e) {
        }

        return isScheduleSet;
    }

    public void deleteAllSlaveFromMaster(int master_id) {
        synchronized (Lock){
            try {
                SQLiteDatabase db = this.getWritableDatabase();
                db.delete(TABLE_SLAVES, SLAVE_IN_GROUP + "=?", new String[]{String.valueOf(master_id)});
                db.close();
            } catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                waitFor();
                deleteAllSlaveFromMaster(master_id);
            }
        }
    }

    private void waitFor() {

        /*while (this.getReadableDatabase().isDbLockedByCurrentThread())
        {
            //TODO NOTHING
        }*/
        /*try {
            wait(500);
        } catch (InterruptedException | IllegalMonitorStateException e1) {
            e1.printStackTrace();
        }*/
    }

    public synchronized void updateTotalSwitchInSlave(int size,String slaveID) {

        synchronized (Lock){
            SQLiteDatabase db=this.getWritableDatabase();

            ContentValues cv=new ContentValues();
            cv.put(SLAVE_TOTAL_SWITCH,size);

            db.update(TABLE_SLAVES,cv,SLAVE_HEX_ID+"=?",new String[]{slaveID});
            db.close();
        }
    }

    public int getTotalSwitchInSlave(String slaveIDs) {
        int totalSwitches=0;

        synchronized (Lock){
            SQLiteDatabase db=this.getReadableDatabase();

            Cursor cursor=db.query(TABLE_SLAVES,new String[]{SLAVE_TOTAL_SWITCH},SLAVE_HEX_ID+"=?",new String[]{slaveIDs},null,null,null,null);
            if(cursor.moveToFirst()){
                totalSwitches=cursor.getInt(0);
            }

            cursor.close();
            db.close();
        }

        return totalSwitches;
    }

    public boolean isMasterAdded(String slaveID) {
        boolean isAdded=false;
        synchronized (Lock){
            SQLiteDatabase db=this.getReadableDatabase();

            Cursor cursor=db.query(TABLE_MASTER,null,MASTER_ID+"=?",new String[]{slaveID},null,null,null,null);

            if(cursor.getCount()>0){
                isAdded=true;
            }

            cursor.close();
            db.close();
        }

        return isAdded;
    }

    public boolean switchHasUserLock(String switchButton, String slaveID){
        boolean hasLock=false;

        synchronized (Lock){
            SQLiteDatabase db=this.getReadableDatabase();

            Cursor cursor=db.query(TABLE_SWITCHES,new String[]{SWITCH_USERLOCK},SWITCH_BUTTON_NUM+"=? AND "+SWITCH_IN_SLAVE+"=?",new String[]{switchButton,slaveID},null,null,null,null);

            if(cursor.moveToFirst()){
                hasLock=cursor.getString(0).equalsIgnoreCase("Y");
            }
        }

        return hasLock;
    }

    public synchronized void setSwitchHasNoSchedule(String slaveHexID, String switchButtonNumber) {
        synchronized (Lock){
            try{
                SQLiteDatabase db=this.getWritableDatabase();

                ContentValues cv=new ContentValues();
                cv.put(SCH_SLOT_NUM, "26");
                cv.put(SCH_IS_ENABLE, 0);

                db.update(TABLE_SCHEDULE, cv, SCH_SW_BTN_NUM + "=? AND " + SCH_SLAVE_ID + "=?", new String[]{switchButtonNumber, slaveHexID});

                db.close();
            }
            catch (SQLiteCantOpenDatabaseException | SQLiteDatabaseLockedException e) {
                Log.e("Exception",e.getMessage());
            }
        }


    }
}
