package com.nivida.smartnode.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Nivida new on 04-May-17.
 */

public class IPDb extends SQLiteOpenHelper {
    public static final String DB_NAME= Environment.getExternalStorageDirectory()+"/SmartNode/"+"ipdb.db";
    public static final int DB_VER=2;

    public static final String TABLE_IP="ipaddr";

    public static final String KEY_IP="ip_name";

    private static final String TAG="IP DB";

    public IPDb(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.setLocale(Locale.ENGLISH);
        db.setLockingEnabled(false);

        String createIP="CREATE TABLE "+TABLE_IP+"("+KEY_IP+" TEXT"+")";

        db.execSQL(createIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_IP);

        onCreate(db);
    }

    public synchronized void addIP(String ipAddress){
        SQLiteDatabase db = null;
        try{
            db = this.getWritableDatabase();

            Cursor cursor=db.query(TABLE_IP,new String[]{KEY_IP},KEY_IP+"=?",new String[]{ipAddress},null,null,null);

            if(cursor.getCount()==0){
                ContentValues cv=new ContentValues();
                cv.put(KEY_IP,ipAddress);
                db.insert(TABLE_IP,null,cv);
            }

            cursor.close();
        } catch (SQLiteCantOpenDatabaseException e) {
            Log.e(TAG,"Sorry, IP Not Saved");
        } finally {
            if (db != null) db.close();
        }
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
            Cursor c = db.rawQuery("PRAGMA foreign_keys", null);
            if (c.moveToFirst()) {
                int result = c.getInt(0);
            }
            if (!c.isClosed()) {
                c.close();
            }
        }
    }

    public synchronized List<String> ipList(){
        List<String> ipLists=new ArrayList<>();
        SQLiteDatabase db = null;
        try{
            db = this.getReadableDatabase();

            Cursor cursor=db.query(TABLE_IP,new String[]{KEY_IP},null,null,null,null,null,null);

            if(cursor.moveToFirst()){
                do{
                    ipLists.add(cursor.getString(0));
                }while (cursor.moveToNext());
            }

            cursor.close();
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        } finally {
            if (db != null) db.close();
        }
        return ipLists;
    }

    public synchronized void deleteIP(){
        try{
            SQLiteDatabase db=this.getWritableDatabase();
            db.delete(TABLE_IP,null,null);
            db.close();
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
    }
}
