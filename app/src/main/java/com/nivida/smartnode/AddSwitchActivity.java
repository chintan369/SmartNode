package com.nivida.smartnode;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.a.Status;
import com.nivida.smartnode.adapter.DimmerListAdapter;
import com.nivida.smartnode.adapter.SwitchListAdapter;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.UDPService;
import com.nivida.smartnode.utils.ImagePath;
import com.nivida.smartnode.utils.NetworkUtility;
import com.nivida.smartnode.utils.Utility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.BitmapFactory.decodeFile;

public class AddSwitchActivity extends AppCompatActivity {
    public static final int SELECT_PICTURE = 1;
    public static final int SELECT_PICTURE_KITKAT = 2;
    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    private static final int REQUEST_CAMERA = 0;
    private static final int RESULT_IMAGE_LOAD = 1;
    private static final int REQUEST_CROP_ICON = 2;
    ListView switchList, dimmerList;
    Toolbar toolbar;
    TextView txt_smartnode, txt_switchlisting, txt_cancel, txt_addtogroup;
    LinearLayout btn_ok, btn_cancel, layout_existing_group, layout_new_group;
    ArrayAdapter<String> adp_master_group;
    int group_position = 0;
    ImageView img_select_group;
    SwitchListAdapter switchListAdapter;
    DimmerListAdapter dimmerListAdapter;
    Bitmap add_group_icon = null;
    String userChoosenTask = "";
    String selectedImagePath;
    String slave_hex_id = "";
    DatabaseHandler databaseHandler;
    AppPreference preference;
    ArrayList<String> button_list = new ArrayList<>();
    ArrayList<String> buttonType = new ArrayList<>();
    ArrayList<String> buttonStatus = new ArrayList<>();
    ArrayList<String> buttonUserlock = new ArrayList<>();
    ArrayList<String> buttonTouchlock = new ArrayList<>();
    List<Bean_Switch> switchListToAdd = new ArrayList<>();
    MqttClient mqttClient;
    String clientId = "";
    String subscribedMessage = "";
    BroadcastReceiver receiver;
    boolean isUserCredentialTrue = false;
    NetworkUtility netcheck;

    boolean isSwitchesListed=false;

    int key[] = {0x01, 0x01, 0x01, 0xAA, 0xAA, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01};
    char[] keys={1,1,1,'A','A',1};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.activity_add_switch);
        Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/raleway.ttf");
        databaseHandler = new DatabaseHandler(getApplicationContext());
        preference = new AppPreference(getApplicationContext());
        netcheck = new NetworkUtility(getApplicationContext());

        Intent intent = getIntent();
        slave_hex_id = intent.getStringExtra("slave_hex_id");
        Log.e("slave_hex frm switch", slave_hex_id);

        startService(new Intent(this, UDPService.class));
        startAddSwitchService();
        startReceiver();
        switchList = (ListView) findViewById(R.id.switch_list);
        switchListAdapter = new SwitchListAdapter(getApplicationContext(), switchListToAdd);
        switchList.setAdapter(switchListAdapter);


        String STSCommand = AppConstant.START_CMD_STATUS_OF_SLAVE + slave_hex_id + AppConstant.CMD_KEY_TOKEN + databaseHandler.getSlaveToken(slave_hex_id) + AppConstant.END_CMD_STATUS_OF_SLAVE;


        //generateSwitchDataFromJSON();
        fetchid();
        txt_smartnode.setTypeface(tf);
        txt_switchlisting.setTypeface(tf);
        txt_cancel.setTypeface(tf);
        txt_addtogroup.setTypeface(tf);

    }

    private void sendSTSCommand(){
        JSONObject object=new JSONObject();

        try{
            object.put("cmd", Cmd.STS);
            object.put("token",databaseHandler.getSlaveToken(slave_hex_id));
            object.put("slave",slave_hex_id);

            String slaveIP=databaseHandler.getMasterIPBySlaveID(slave_hex_id);

            List<String> ipList=new IPDb(this).ipList();
            if (ipList.contains(slaveIP)) {
                Log.e("IP For Slave",slaveIP);
                new SendUDP(object.toString(), slaveIP).execute();
            } else {
                new PublishMessage(object.toString()).execute();
            }
        }catch (JSONException j){

        }
    }

    private void startAddSwitchService() {
        if (!serviceIsRunning()) {
            final Intent intent = new Intent(this, AddDeviceService.class);
            //startService(intent);
        }
    }

    private boolean serviceIsRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (SERVICE_CLASSNAME.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void generateSwitchDataFromJSON() {
        if (subscribedMessage == null || subscribedMessage.equals("")) {
            Log.e("subscribed", "null");
            //Toast.makeText(getApplicationContext(), "Sorry, Device is not ready yet.", Toast.LENGTH_SHORT).show();
        } else if (subscribedMessage.contains("Hello")) {
            Log.e("Message from device ", "Device said hello as it started just now");
        } else {
            try {
                JSONObject jsonDevice = new JSONObject(subscribedMessage);
                String cmd = jsonDevice.getString("cmd");
                if (cmd.equals("STS") && !isSwitchesListed) {

                    if (jsonDevice.has("status") && jsonDevice.getString("status").contains(Status.ERROR)) {
                        C.Toast(getApplicationContext(), "Please Login Again Due to Some Problem Occured...");
                        return;
                    }

                    String slaveIDIn=jsonDevice.getString("slave");

                    if(!slaveIDIn.equals(slave_hex_id)){
                        return;
                    }

                    isSwitchesListed=true;

                    String buttons = jsonDevice.getString("button");
                    button_list.clear();
                    buttonType.clear();
                    buttonStatus.clear();
                    buttonUserlock.clear();
                    buttonTouchlock.clear();

                    int j = 0;
                    for (int i = 0; i < buttons.length(); i += 2) {
                        button_list.add(String.valueOf(buttons.charAt(i)) + String.valueOf(buttons.charAt(i + 1)));
                        //Log.e("Button Added :", button_list.get(j));
                        j++;
                    }

                    databaseHandler.updateTotalSwitchInSlave(button_list.size(),slave_hex_id);

                    String dimmerValue = jsonDevice.getString("dval");

                    for (int i = 0; i < dimmerValue.length(); i++) {
                        buttonType.add(String.valueOf(dimmerValue.charAt(i)));
                        //Log.e("Dimmer Value :", buttonType.get(i));
                    }

                    String buttonStatusValues = jsonDevice.getString("val");

                    for (int i = 0; i < buttonStatusValues.length(); i++) {
                        buttonStatus.add(String.valueOf(buttonStatusValues.charAt(i)));
                        //Log.e("Switch status :", buttonStatus.get(i));
                    }

                    String buttonUserLocks = jsonDevice.getString("user_locked");

                    for (int i = 0; i < buttonUserLocks.length(); i++) {
                        buttonUserlock.add(String.valueOf(buttonUserLocks.charAt(i)));
                        //Log.e("Switch status :", buttonUserlock.get(i));
                    }

                    String buttonTouchLocks = jsonDevice.getString("touch_lock");

                    for (int i = 0; i < buttonTouchLocks.length(); i++) {
                        buttonTouchlock.add(String.valueOf(buttonUserLocks.charAt(i)));
                        //Log.e("Switch status :", buttonTouchlock.get(i));
                    }

                    switchListToAdd.clear();

                    for (int i = 0; i < button_list.size(); i++) {
                        if (!databaseHandler.isSwitchAdded(button_list.get(i), slave_hex_id)) {
                            Bean_Switch beanSwitch = new Bean_Switch();

                            if (buttonStatus.get(i).equalsIgnoreCase("A"))
                                beanSwitch.setIsSwitchOn(1);
                            else beanSwitch.setIsSwitchOn(0);

                            if (buttonType.get(i).equalsIgnoreCase("X")) {
                                beanSwitch.setIsSwitch("s");
                                beanSwitch.setDimmerValue(0);
                                beanSwitch.setSwitch_name(button_list.get(i) + " " + "Switch");
                            } else {
                                beanSwitch.setIsSwitch("d");
                                beanSwitch.setDimmerValue(Integer.parseInt(buttonType.get(i)));
                                beanSwitch.setSwitch_name(button_list.get(i) + " " + "Dimmer");
                            }

                            beanSwitch.setUserLock(buttonUserlock.get(i));
                            beanSwitch.setTouchLock(buttonTouchlock.get(i));

                            beanSwitch.setSwitch_btn_num(button_list.get(i));

                            beanSwitch.setSwitchInSlave(slave_hex_id);

                            beanSwitch.setIsFavourite(0);
                            beanSwitch.setSwitch_icon(1);


                            switchListToAdd.add(beanSwitch);
                            switchListAdapter.notifyDataSetChanged();
                        }

                    }

                    if (switchListToAdd.size() <= 0) {
                        showAllSwitchAddedDialog();
                    }

                    Log.e("Swtch Adp", "Called actvty");
                }

            } catch (JSONException e) {
                Log.e("JSON Message : ", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void showAllSwitchAddedDialog() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("Message");
        builder.setMessage("All Switches for this slave has already been added in group");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onBackPressed();
            }
        });
        builder.setCancelable(false);
        android.support.v7.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void publishCommandForSwitches(String slave_hex_id) {

    }

    private void startReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                String device_id = "";
                String device_name = "";
                if (bundle != null) {
                    subscribedMessage = bundle.getString(AddDeviceService.MESSAGETOSEND);
                    String UDPMessage = bundle.getString(UDPService.MESSAGEJSON);

                    if (UDPMessage != null) {
                        subscribedMessage = UDPMessage;
                    }
                    generateSwitchDataFromJSON();
                    Log.e("JSOn fr swtch recvr ", "" + subscribedMessage);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, new IntentFilter(AddDeviceService.NOTIFICATION));
        sendSTSCommand();
        if (!isSwitchesListed) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isSwitchesListed && switchListToAdd.size() <= 0) {
                        sendSTSCommand();
                    }
                }
            }, 5000);
        }
        Log.e("Reciever :", "Registered");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        Log.e("Reciever :", "Unregistered");
    }

    private void fetchid() {

        btn_ok = (LinearLayout) findViewById(R.id.btn_ok);
        btn_cancel = (LinearLayout) findViewById(R.id.btn_cancel);
        txt_smartnode = (TextView) findViewById(R.id.txt_smartnode);
        txt_switchlisting = (TextView) findViewById(R.id.txt_switchlisting);
        txt_cancel = (TextView) findViewById(R.id.txt_cancel);
        txt_addtogroup = (TextView) findViewById(R.id.txt_addtogroup);


        //dimmerList=(ListView)findViewById(R.id.dimmer_list);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //Define Adapter to set Data in Switch and Dimmer List

        //dimmerList.setAdapter(dimmerListAdapter);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Bean_Switch> switchList = switchListAdapter.getSelectedSwitches();
                //List<Bean_Dimmer> dimmerList=dimmerListAdapter.getSelectedDimmers();

                List<Bean_Switch> checkedSwitches = new ArrayList<>();
                //List<Bean_Dimmer> checkedDimmers=new ArrayList<>();

                for (int i = 0; i < switchList.size(); i++) {
                    Bean_Switch selectedSwitch = switchList.get(i);
                    boolean isChecked = selectedSwitch.isChecked();
                    if (isChecked) {
                        checkedSwitches.add(selectedSwitch);
                    }
                }

                /*for(int i=0;i<dimmerList.size();i++){
                    Bean_Dimmer selectedDimmer=dimmerList.get(i);
                    boolean isChecked=selectedDimmer.isChecked();
                    if(isChecked){
                        checkedDimmers.add(selectedDimmer);
                    }
                }*/

                if (checkedSwitches.size() == 0) {
                    Toast.makeText(getApplicationContext(), "Please select at least 1 switch", Toast.LENGTH_SHORT).show();
                } else {
                    showAddToGroupDialog(checkedSwitches);
                    //Toast.makeText(AddSwitchActivity.this, ""+checkedSwitches.size(), Toast.LENGTH_SHORT).show();
                }

            }
        });


    }


    //----------------for add to group dialog box--------------------------------

    private void showAddToGroupDialog(final List<Bean_Switch> checkedSwitches) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.add_to_group_dialog, null);
        dialogBuilder.setView(dialogView);

        // final EditText edt=(EditText) dialogView.findViewById(R.id.edt_slavename);
        final RadioGroup radiogroup_type = (RadioGroup) dialogView.findViewById(R.id.radiogroup_type);
        layout_existing_group = (LinearLayout) dialogView.findViewById(R.id.layout_existing_group);
        layout_new_group = (LinearLayout) dialogView.findViewById(R.id.layout_new_group);

        radiogroup_type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_exist) {
                    layout_existing_group.setVisibility(View.VISIBLE);
                    layout_new_group.setVisibility(View.GONE);
                } else if (checkedId == R.id.radio_new) {
                    layout_existing_group.setVisibility(View.GONE);
                    layout_new_group.setVisibility(View.VISIBLE);
                }
            }
        });

        /*final RadioButton radio_exist=(RadioButton)dialogView.findViewById(R.id.radio_exist);
        final RadioButton radio_new=(RadioButton)dialogView.findViewById(R.id.radio_new);*/
        final Spinner spinner_group = (Spinner) dialogView.findViewById(R.id.spinner_group);

        spinner_group.setPrompt("---Select Group---");

        List<Bean_MasterGroup> masterGroupList = databaseHandler.getAllMasterGroupData();


        ArrayList<String> group_names = new ArrayList<>();
        final ArrayList<Integer> group_id = new ArrayList<>();
        for (int j = 0; j < masterGroupList.size() - 1; j++) {
            Bean_MasterGroup masterGroup = masterGroupList.get(j);
            if(masterGroup.getId()!=100){
                group_names.add(masterGroup.getName());
                group_id.add(masterGroup.getId());
            }

        }

        if(group_names.size()==0){
            RadioButton radioNew= (RadioButton)dialogView.findViewById(R.id.radio_new);
            radioNew.setChecked(true);
            ((RadioButton)dialogView.findViewById(R.id.radio_exist)).setEnabled(false);
        }

        adp_master_group = new ArrayAdapter<String>(getApplicationContext(), R.layout.custom_spinner_size, group_names);
        spinner_group.setAdapter(adp_master_group);

        final EditText edt_groupname = (EditText) dialogView.findViewById(R.id.edt_groupname);
        img_select_group = (ImageView) dialogView.findViewById(R.id.img__select_group);

        img_select_group.setClickable(true);
        img_select_group.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });


        dialogBuilder.setTitle("Add To");
        dialogBuilder.setPositiveButton("Add", null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();

        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);

                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        int selected_group_id = radiogroup_type.getCheckedRadioButtonId();
                        final RadioButton rdo_selected = (RadioButton) dialogView.findViewById(selected_group_id);
                        if (selected_group_id==R.id.radio_exist) {
                            String group_name = spinner_group.getSelectedItem().toString();
                            int position = spinner_group.getSelectedItemPosition();

                            databaseHandler.addSwitchtoGroup(group_id.get(position), checkedSwitches);
                            b.dismiss();
                            Toast.makeText(AddSwitchActivity.this, "Switches added to group successfully", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                            /*databaseHandler.addDimmerstoGroup(group_id.get(position),checkedDimmers);*/
                        } else if (selected_group_id==R.id.radio_new) {
                            if (edt_groupname.getText().toString().trim().equals("")) {
                                Toast.makeText(getApplicationContext(), "Please enter group name", Toast.LENGTH_SHORT).show();
                            } else if (add_group_icon == null) {
                                Toast.makeText(getApplicationContext(), "Please select group image", Toast.LENGTH_SHORT).show();
                            } else {

                                int groupCounts=databaseHandler.getGroupDataCounts();

                                Bean_MasterGroup group = new Bean_MasterGroup();
                                group.setId(databaseHandler.getGroupLastId()==99 ? databaseHandler.getGroupLastId()+2 : databaseHandler.getGroupLastId()+ 1);
                                group.setName(edt_groupname.getText().toString());
                                group.setBitmap(add_group_icon);
                                Log.e("Last ID stored","--"+databaseHandler.getGroupLastId());


                                if(groupCounts<2){
                                   group.setId(1);
                                }

                                img_select_group.setDrawingCacheEnabled(true);
                                String groupNameID=group.getName().replace(" ","_")+"_"+group.getId();
                                String imagePath=C.saveGroupImageToLocal(img_select_group.getDrawingCache(),groupNameID);
                                group.setImgLocalPath(imagePath);

                                databaseHandler.addMasterGroupItem(group);

                                Log.e("Last ID","--"+databaseHandler.getGroupLastId());

                                databaseHandler.addSwitchtoGroup(databaseHandler.getGroupLastId(), checkedSwitches);
                                b.dismiss();
                                Toast.makeText(AddSwitchActivity.this, "Switches added to group successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(getApplicationContext(), MasterGroupActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                                finish();
                                /*databaseHandler.addDimmerstoGroup(databaseHandler.getGroupLastId(),checkedDimmers);*/
                            }
                        }



                    }
                });

                btn_cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        b.dismiss();
                    }
                });

            }
        });
        b.show();
    }

    private String saveGroupImageToLocal(Bitmap groupPic,String groupNameID) {
        String imagePath="";

        String rootDirectory= Environment.getExternalStorageDirectory()+"/SmartNode/Groups/";
        File rootDir= new File(rootDirectory);
        if(!rootDir.exists()) rootDir.mkdir();

        String imageName=groupNameID+".jpg";

        File imageFile=new File(rootDir,imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            // Use the compress method on the BitMap object to write image to the OutputStream
            groupPic.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            imagePath=imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return imagePath;
    }

    public Bitmap resizeImage(String path) {
        Bitmap image2 = decodeFile(path);
        File file = new File(path);
        try {
            double xFactor = 0;
            double width = Double.valueOf(image2.getWidth());
            Log.v("WIDTH", String.valueOf(width));
            double height = Double.valueOf(image2.getHeight());
            Log.v("height", String.valueOf(height));
            if (width > height) {
                xFactor = 841 / width;
            } else {
                xFactor = 595 / width;
            }


            Log.v("Nheight", String.valueOf(width * xFactor));
            Log.v("Nweight", String.valueOf(height * xFactor));
            int Nheight = (int) ((xFactor * height));
            int NWidth = (int) (xFactor * width);

            Bitmap bm = Bitmap.createScaledBitmap(image2, NWidth, Nheight, true);
            file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(file);
            bm.compress(Bitmap.CompressFormat.JPEG, 40, ostream);


            ostream.close();
            return bm;
        } catch (Exception e) {

        }
        return null;
    }

    public void selectImage() {
        final CharSequence[] items = {"Choose from Library",
                "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(getApplicationContext());
                /*if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result)
                        cameraIntent();
                } else */
                if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, SELECT_PICTURE_KITKAT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                }
                break;
        }
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        selectedImagePath = null;

        if(resultCode==RESULT_OK && data.getData()!=null){
            Uri originalUri = null;
            if (requestCode == SELECT_PICTURE) {
                originalUri = data.getData();
                String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);

                Bitmap resizedBmp=resizeImage(originalPath);
                if(resizedBmp==null){
                    add_group_icon = decodeFile(originalPath);
                    add_group_icon= Bitmap.createScaledBitmap(add_group_icon,90,90,false);
                }
                else {
                    add_group_icon=resizedBmp;
                }
                img_select_group.setImageBitmap(add_group_icon);
            } else if (requestCode == SELECT_PICTURE_KITKAT) {
                originalUri = data.getData();
                getContentResolver().takePersistableUriPermission(originalUri,(Intent.FLAG_GRANT_READ_URI_PERMISSION| Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);
                Bitmap resizedBmp=resizeImage(originalPath);
                if(resizedBmp==null){
                    add_group_icon = decodeFile(originalPath);
                    add_group_icon= Bitmap.createScaledBitmap(add_group_icon,90,90,false);
                }
                else {
                    add_group_icon=resizedBmp;
                }
                img_select_group.setImageBitmap(add_group_icon);
            }
            else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data);
            }
            //String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);
        }

        /*if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RESULT_IMAGE_LOAD)
                onSelectFromGalleryResult(data);
            else if (requestCode == REQUEST_CAMERA)
                onCaptureImageResult(data);
        }

        if (resultCode == RESULT_OK && requestCode == REQUEST_CROP_ICON) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                add_group_icon = extras.getParcelable("data");
                img_select_group.setImageBitmap(add_group_icon);
            }
        }*/
    }

    private void onCaptureImageResult(Intent data) {
        if (data != null) {
            Uri selectedImage = data.getData();
            String originalPath = ImagePath.getPath(getApplicationContext(),selectedImage);

            Bitmap resizedBmp=resizeImage(originalPath);
            if(resizedBmp==null){
                add_group_icon = decodeFile(originalPath);
                add_group_icon= Bitmap.createScaledBitmap(add_group_icon,90,90,false);
            }
            else {
                add_group_icon=resizedBmp;
            }
            img_select_group.setImageBitmap(add_group_icon);
            //performCropImage(selectedImage);
        }
    }

    private void onSelectFromGalleryResult(Intent data) {

        if (data != null) {
            Uri selectedImage = data.getData();
            performCropImage(selectedImage);
        }

    }

    private void performCropImage(Uri selectedImagePath) {

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //indicate image type and Uri
        cropIntent.setDataAndType(selectedImagePath, "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        //indicate output X and Y
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        startActivityForResult(cropIntent, REQUEST_CROP_ICON);
    }

    @Override
    public void onBackPressed() {
        if (preference.isFromDirectMaster()) {
            Intent intent = new Intent(getApplicationContext(), AddMasterActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }

    public class PublishMessage extends AsyncTask<Void, Void, String> {

        public ProgressDialog statusDialog;
        String command = "";

        public PublishMessage(String command) {
            this.command = command;
        }

        @Override
        protected void onPreExecute() {
            statusDialog = new ProgressDialog(AddSwitchActivity.this);
            statusDialog.setMessage("Please wait...");
            statusDialog.setIndeterminate(false);
            statusDialog.setCancelable(false);
            statusDialog.show();
        }

        @Override
        protected String doInBackground(Void[] params) {
            //publishCommandForSwitches(slave_hex_id);
            if (netcheck.isOnline()) {
                clientId = C.MQTT_ClientID;
                try {
                    mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
                    MqttConnectOptions connectOptions = new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);

                    Log.e("switchCmd", command);

                    MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                    mqttMessage.setQos(0);
                    mqttMessage.setRetained(false);
                    mqttClient.publish(databaseHandler.getSlaveTopic(slave_hex_id) + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                    Log.e("MQTT ", "Message Published to get Switches");
                    mqttClient.disconnect();

                } catch (MqttException e) {
                    Log.e("Exception : ", e.getMessage());
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(getApplicationContext(), "No internet connection found,\nplease check your connection first",
                        Toast.LENGTH_SHORT).show();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String o) {
            statusDialog.dismiss();
        }
    }

    public class SendUDP extends AsyncTask<Void, Void, String> {
        String message;
        String ipAddress = "";

        public SendUDP(String message, String ipAddress) {
            this.message = message;
            this.ipAddress = ipAddress;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            try {

                Log.e("Message",message);
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                Log.e("IP Address", "->" + this.ipAddress);

                Log.e("Brodacast IP",C.getBroadcastAddress(getApplicationContext()).getHostAddress());

                /*if (this.ipAddress.isEmpty() || !C.isValidIP(this.ipAddress)) {*/
                    server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    //server_addr = new InetSocketAddress(ipAddress, 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("Packet", "Sent with IP");
                /*} else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                    Log.e("Packet", "Sent WO IP");
                }*/

                socket.disconnect();
                socket.close();
            } catch (SocketException s) {
                Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }
    }
}
