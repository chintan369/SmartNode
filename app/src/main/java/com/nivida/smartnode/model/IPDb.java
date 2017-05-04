package com.nivida.smartnode.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nivida new on 04-May-17.
 */

public class IPDb extends SQLiteOpenHelper {
    public static final String DB_NAME= Environment.getExternalStorageDirectory()+"/SmartNode/"+"ipdb.db";
    public static final int DB_VER=1;

    public static final String TABLE_IP="ipaddr";

    public static final String KEY_IP="ip_name";

    private static final String TAG="IP DB";

    public IPDb(Context context) {
        super(context, DB_NAME, null, DB_VER);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createIP="CREATE TABLE "+TABLE_IP+"("+KEY_IP+" TEXT"+")";

        db.execSQL(createIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_IP);

        onCreate(db);
    }

    public synchronized void addIP(String ipAddress){
        try{
            SQLiteDatabase db=this.getWritableDatabase();

            Cursor cursor=db.query(TABLE_IP,new String[]{KEY_IP},KEY_IP+"=?",new String[]{ipAddress},null,null,null);

            if(cursor.getCount()==0){
                ContentValues cv=new ContentValues();
                cv.put(KEY_IP,ipAddress);
                db.insert(TABLE_IP,null,cv);
            }

            cursor.close();
            db.close();
        }catch (Exception e){
            Log.e(TAG,"Sorry, IP Not Saved");
        }
    }

    public synchronized List<String> ipList(){
        List<String> ipLists=new ArrayList<>();
        try{
            SQLiteDatabase db=this.getReadableDatabase();

            Cursor cursor=db.query(TABLE_IP,new String[]{KEY_IP},null,null,null,null,null,null);

            if(cursor.moveToFirst()){
                do{
                    ipLists.add(cursor.getString(0));
                }while (cursor.moveToNext());
            }
        }catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return ipLists;
    }

    public synchronized void deleteIP(){
        try{
            SQLiteDatabase db=this.getWritableDatabase();
            db.delete(TABLE_IP,null,null);
        }catch (Exception e){
            Log.e(TAG, e.getMessage() );
        }
    }
}
