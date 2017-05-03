package com.nivida.smartnode.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Created by Nivida new on 14-Jul-16.
 */
public class BitmapUtility {

    public static byte[] getBytes(Bitmap bitmap){
        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        Bitmap.createScaledBitmap(bitmap,50,50,false);
        bitmap.compress(Bitmap.CompressFormat.JPEG,70,stream);
        return stream.toByteArray();
    }

    public static Bitmap getBitmap(byte[] image){
        return BitmapFactory.decodeByteArray(image,0,image.length);
    }
}
