package com.nivida.smartnode.utils;

import android.content.Intent;

import java.util.Random;

/**
 * Created by Nivida new on 18-Jan-17.
 */

public class CustomEncryption {

    public static String EncryptedCommand(String command,String key){
        String encryptedMessage="";

        String ASCIICommand="";

        if(!command.isEmpty()){
            for(int i=0; i<command.length(); i++){
                char tmp=command.charAt(i);

                int currentASCII=(int) tmp;

                String ascii=getCeaserBy2(String.valueOf(currentASCII),key);

                if(ascii.length()==1){
                    ASCIICommand += "00"+ascii;
                }
                else if(ascii.length()==2){
                    ASCIICommand += "0"+ascii;
                }
                else {
                    ASCIICommand += ascii;
                }

            }
        }

        encryptedMessage=ASCIICommand;

        return encryptedMessage;
    }

    public static String getCeaserBy2(String ascii, String hexSlave){

        int currentASCII=Integer.parseInt(ascii);

        currentASCII = (currentASCII*3)+hexToInteger(hexSlave);

        return String.valueOf(currentASCII);
    }

    public static String decryptCeaser(String value, String hexSlave){
        int currentASCII=Integer.parseInt(value);

        currentASCII = (currentASCII-hexToInteger(hexSlave))/3;

        return String.valueOf(currentASCII);
    }

    public static String decryptedCommand(String encrypted,String key){
        String decryptedCommnad="";

        if(!encrypted.isEmpty()){
            for(int i=0; i<encrypted.length(); i+=3){
                String code= String.valueOf(encrypted.charAt(i))+String.valueOf(encrypted.charAt(i+1))+String.valueOf(encrypted.charAt(i+2));

                String deceaserCode=decryptCeaser(code,key);

                decryptedCommnad += Character.toString((char) Integer.parseInt(deceaserCode));

            }
        }

        return decryptedCommnad;
    }

    public static String getRandomValue(){
        Random random=new Random();
        return String.valueOf(random.nextInt(90-5+1)+5);
    }

    public static int hexToInteger(String hexSlave){

        int hexInt=0;

        for(int i=0; i<hexSlave.length(); i++){
            String tmp= String.valueOf(hexSlave.charAt(i));
                    //+String.valueOf(hexSlave.charAt(i+1));

            hexInt += Integer.parseInt(tmp,16);
        }

        if(hexInt>234) return 234;

        return hexInt;
    }
}
