package com.nivida.smartnode;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.nivida.smartnode.adapter.SwitchListAdapter;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;

import java.util.ArrayList;
import java.util.List;

public class AddSwitch2Activity extends AppCompatActivity {

    ListView switchList;
    Toolbar toolbar;
    TextView txt_smartnode,txt_switchlisting,txt_dimmerlisting,txt_cancel,txt_addtogroup;
    LinearLayout btn_ok,btn_cancel,layout_existing_group,layout_new_group;
    ArrayAdapter<String> adp_master_group;

    int group_position=0;
    ImageView img_select_group;

    SwitchListAdapter switchListAdapter;

    Bitmap add_group_icon=null;

    private static final int REQUEST_CAMERA=0;
    private static final int RESULT_IMAGE_LOAD=1;
    private static final int REQUEST_CROP_ICON=2;
    String userChoosenTask="";
    String selectedImagePath;

    String slave_hex_id="";

    DatabaseHandler databaseHandler;

    ArrayList<String> button_list=new ArrayList<>();
    ArrayList<String> buttonType=new ArrayList<>();
    ArrayList<String> buttonStatus=new ArrayList<>();

    List<Bean_Switch> switchListToAdd=new ArrayList<>();

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue=false;
    NetworkUtility netcheck;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_switch2);

        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        databaseHandler=new DatabaseHandler(getApplicationContext());
        netcheck=new NetworkUtility(getApplicationContext());

        Intent intent=getIntent();
        slave_hex_id=intent.getStringExtra("slave_hex_id");
        Log.e("slave_hex frm switch",slave_hex_id);
    }
}
