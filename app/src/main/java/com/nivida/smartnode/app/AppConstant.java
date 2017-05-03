package com.nivida.smartnode.app;

/**
* Created by Chintak Patel on 14-Jul-16.
 */
public class AppConstant {

    public static final String MQTT_SERVER_IP_ADDRESS="203.109.67.132";
    public static final int MQTT_PORT_NUMBER=1883;
    //public static final String MQTT_BROKER_URL="tcp://connect.smartnode.in:1883";
    public static final String MQTT_BROKER_URL="tcp://52.43.171.204:1883";

    public static final String MQTT_USERNAME="nO1cANsTOPuS";
    public static final String MQTT_PASSWORD="d$2N@8p&1V#1";

    //public static final String MQTT_SUBSCRIBE_TOPIC="master/M_tx";

    //public static final String MQTT_PUBLISH_TOPIC="master/M_rx";
    public static final String MQTT_SUBSCRIBE_TOPIC="/M_tx";
    public static final String MQTT_PUBLISH_TOPIC="/M_rx";

    public static final String CMD_KEY_TOKEN="\",\"token\":\"";

    public static final String CMD_GET_MASTER="{\"cmd\":\"MST\"}";
    public static final String CMD_GET_MASTER_TOKEN="{\"cmd\":\"MST\",\"token\":\"aaaaaaaaaa\"}";

    public static final String CMD_ADMIN_LOGIN_1="{\"cmd\":\"LIN\",\"user\":\"";
    public static final String CMD_ADMIN_LOGIN_2="\",\"pin\":\"";
    public static final String CMD_ADMIN_LOGIN_3="\"}";

    public static final String CMD_GUEST_LOGIN_1="{\"cmd\":\"ULN\",\"user\":\"";
    public static final String CMD_GUEST_LOGIN_2="\",\"pin\":\"";
    public static final String CMD_GUEST_LOGIN_3="\"}";


    public static final String START_CMD_RENAME_MASTER="{\"cmd\":\"MRN\",\"data\":\"";
    public static final String END_CMD_RENAME_MASTER="\"}";

    public static final String CMD_ADD_DEVICE="{\"cmd\":\"ADD\",\"t\":\"60\"}";

    public static final String CMD_LIST_OF_SLAVES="{\"cmd\":\"LOS\"}";

    public static final String START_CMD_STATUS_OF_SLAVE="{\"cmd\":\"STS\",\"slave\":\"";
    public static final String END_CMD_STATUS_OF_SLAVE="\"}";

    public static final String START_CMD_UPD_SWITCH="{\"cmd\":\"UPD\",\"slave\":\"";
    public static final String CENTER_CMD_UPD_SWITCH="\",\"data\":\"";
    public static final String END_CMD_UPD_SWITCH="\"}";

    public static final String START_CMD_SCHEDULE="{\"cmd\":\"SCH\",\"slave\":\"";
    public static final String CENETER_CMD_SCHEDULE="\",\"data\":\"";
    public static final String END_CMD_SCHEDULE="\"}";

    public static final String START_CMD_SCH_GETALL="{\"cmd\":\"SCH\",\"slave\":\"";
    public static final String END_CMD_SCH_GETALL="\",\"data\":\"ALL\"}";

    public static final String START_CMD_SCH_GET="{\"cmd\":\"SCH\",\"slave\":\"";
    public static final String CENETER_CMD_SCH_GET="\",\"data\":\"";
    public static final String END_CMD_SCH_GET="\"}";



    public static final int MASTER_PORT_TO_SEND=13001;
    public static final int MASTER_PORT_TO_RECV=13000;

    public static char[] getPassword(){
        char[] password=new char[AppConstant.MQTT_PASSWORD.length()];
        for(int i=0;i<AppConstant.MQTT_PASSWORD.length(); i++){
            password[i]=AppConstant.MQTT_PASSWORD.charAt(i);
        }

        return password;
    }

}
