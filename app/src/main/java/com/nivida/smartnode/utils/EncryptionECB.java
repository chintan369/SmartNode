package com.nivida.smartnode.utils;

import android.util.Base64;
import android.util.Log;

import java.security.Key;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Nivida new on 26-Dec-16.
 */

public class EncryptionECB {

    public static String encrypt(String plainText, String encKey) {
        byte[] key = encKey.getBytes();

        try {
            byte[]rawKey = getRawKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF8"));


            String encryptedString=new String(Base64.encode(cipherText,Base64.DEFAULT));
            //String encryptedString = Base64.encodeToString(cipherText, Base64.DEFAULT);
            return encryptedString;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exc",e.getMessage());
        }
        return null;
    }

    private static Key generateKey(byte[] keyValue) throws Exception
    {
        return new SecretKeySpec(keyValue, "AES");
    }


    public static String hexToAscii(String hex) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    public static String decrypt(String encryptedText, String encKey) {
        byte[] key = encKey.getBytes();

        try {
            byte[] rawKey = getRawKey(key);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
            SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] cipherText = Base64.decode(encryptedText.getBytes("UTF8"), Base64.DEFAULT);
            String decryptedString = new String(cipher.doFinal(cipherText), "UTF-8");
            return decryptedString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] encryptText(String plainText,String encKey){
        byte[] key = encKey.getBytes();
        // AES defaults to AES/ECB/PKCS5Padding in Java 7
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");

        try{
            Cipher aesCipher = Cipher.getInstance("AES");
            aesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] byteCipherText = aesCipher.doFinal(plainText.getBytes());
            Log.e("AES IO",byteCipherText.toString());
            return byteCipherText;
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return "Nothing".getBytes();
    }

    public static String convertStringToHex(String str){

        char[] chars = str.toCharArray();

        StringBuffer hex = new StringBuffer();
        for(int i = 0; i < chars.length; i++){
            int tempChar=(int)chars[i];



            if(tempChar<0x10){
                hex.append("0");
            }

            hex.append(Integer.toHexString((int)chars[i]));

            Log.e("gen key",""+tempChar+" -- "+hex.toString());
        }

        return hex.toString();
    }



    private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }

    public static String convertHexToString(String hex){

        StringBuilder sb = new StringBuilder();
        StringBuilder temp = new StringBuilder();

        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for( int i=0; i<hex.length()-1; i+=2 ){

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char)decimal);

            temp.append(decimal);
        }
        System.out.println("Decimal : " + temp.toString());

        return sb.toString();
    }


}
