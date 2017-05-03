package com.nivida.smartnode.adapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.nivida.smartnode.FavouriteActivity;
import com.nivida.smartnode.Globals;
import com.nivida.smartnode.GroupSwitchOnOffActivity;
import com.nivida.smartnode.MasterGroupActivity;
import com.nivida.smartnode.R;
import com.nivida.smartnode.SetScheduleActivity;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.beans.Bean_SwitchIcons;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.services.AddDeviceService;
import com.nivida.smartnode.services.AddMasterService;
import com.nivida.smartnode.utils.CircularSeekBar;
import com.nivida.smartnode.utils.NetworkUtility;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chintak Patel on 23-Jul-16.
 */
public class SwitchDimmerOnOffAdapter extends BaseAdapter {

    Context context;
    String fromActivity;
    List<Bean_Switch> switchList=new ArrayList<>();
    DatabaseHandler databaseHandler;
    private DimmerChangeCallBack callback;
    int groupid=0;
    AppPreference preference;
    //List<Bean_SwitchIcons> switchIconsList=new ArrayList<>();

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    MqttClient mqttClient;
    String clientId="";
    String subscribedMessage="";
    NetworkUtility netcheck;

    OnSwitchSelection switchSelection;

    public SwitchDimmerOnOffAdapter(Context context,List<Bean_Switch> switchList,String fromActivity, OnSwitchSelection switchSelection){
        this.switchSelection=switchSelection;
        this.context=context;
        this.switchList=switchList;
        this.fromActivity=fromActivity;
        this.netcheck=new NetworkUtility(context);
        clientId=MqttClient.generateClientId();
        databaseHandler=new DatabaseHandler(context);
        preference=new AppPreference(context);
        if(switchList.size()>0){
            this.groupid=switchList.get(0).getSwitchInGroup();
        }
    }

    @Override
    public int getCount() {
        return switchList.size();
    }

    @Override
    public Object getItem(int position) {
        return switchList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view=null;

        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view=inflater.inflate(R.layout.switch_dimmer_onoff,null);

        final Bean_Switch beanSwitch=switchList.get(position);

        final TextView txt_group_switch=(TextView) view.findViewById(R.id.group_switch_name);
        final TextView txt_group=(TextView) view.findViewById(R.id.group_name);
        final TextView txt_progress=(TextView) view.findViewById(R.id.txt_progress);

        final ImageView img_fav=(ImageView) view.findViewById(R.id.fav_off);
        final ImageView option_menu=(ImageView) view.findViewById(R.id.power_off);
        final ImageView img_switch=(ImageView) view.findViewById(R.id.switch_off);
        final ImageView img_tlock=(ImageView) view.findViewById(R.id.img_tlock);
        final ImageView img_ulock=(ImageView) view.findViewById(R.id.img_ulock);
        final ImageView img_schedule=(ImageView) view.findViewById(R.id.img_schedule);

        LinearLayout layout_dimmerBar=(LinearLayout) view.findViewById(R.id.layout_dimmerBar);
        final ImageView img_minusDimmer=(ImageView) view.findViewById(R.id.img_minusDimmer);
        final ImageView img_plusDimmer=(ImageView) view.findViewById(R.id.img_plusDimmer);
        final EditText edt_dimmerValue=(EditText) view.findViewById(R.id.edt_dimmerValue);

        if(beanSwitch.getTouchLock().equalsIgnoreCase("Y")){
            img_tlock.setImageResource(R.drawable.tlock);
        }
        else {
            img_tlock.setImageResource(R.drawable.tunlock);
        }

        if(beanSwitch.getUserLock().equalsIgnoreCase("Y")){
            img_ulock.setImageResource(R.drawable.lock);
        }
        else {
            img_ulock.setImageResource(R.drawable.unlock);
        }

        /*if(databaseHandler.hasScheduleSet(beanSwitch.getSwitchInSlave(),String.valueOf(beanSwitch.getSwitch_id())))
            img_schedule.setImageResource(R.drawable.has_schedule);
        else
            img_schedule.setImageResource(R.drawable.no_schedule);*/

        if(switchList.get(position).getHasSchedule()==0){
            img_schedule.setImageResource(R.drawable.no_schedule);
        }
        else {
            img_schedule.setImageResource(R.drawable.has_schedule);
        }

        img_tlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(switchList.get(position).getTouchLock().equalsIgnoreCase("N")){
                    setTouchUserLock(beanSwitch.getSwitchInSlave(),beanSwitch.getSwitch_btn_num(),true,true);
                }
                else {
                    setTouchUserLock(beanSwitch.getSwitchInSlave(),beanSwitch.getSwitch_btn_num(),false,true);
                }
            }
        });

        img_ulock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(databaseHandler.getSlaveUserType(beanSwitch.getSwitchInSlave()).equalsIgnoreCase(Cmd.ULN)){
                    C.Toast(context,"Guest don't have privilleges to perform this action");
                }
                else {
                    if(switchList.get(position).getUserLock().equalsIgnoreCase("N")){
                        setTouchUserLock(beanSwitch.getSwitchInSlave(),beanSwitch.getSwitch_btn_num(),true,false);
                    }
                    else {
                        setTouchUserLock(beanSwitch.getSwitchInSlave(),beanSwitch.getSwitch_btn_num(),false,false);
                    }
                }
            }
        });

        img_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context, SetScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("group_id",groupid);
                intent.putExtra("switchID",switchList.get(position).getSwitch_id());
                intent.putExtra("switchName",switchList.get(position).getSwitch_name());
                context.startActivity(intent);
            }
        });

        final DiscreteSeekBar dimmerProgress=(DiscreteSeekBar) view.findViewById(R.id.dimmerProgress);

        int groupid=switchList.get(position).getSwitchInGroup();

        //if(beanSwitch.getIsSwitch().equalsIgnoreCase("s")){

        try{
            if(beanSwitch.getIsSwitchOn()==1){
                Glide.with(context)
                        .load(databaseHandler.getSwitchIconData(beanSwitch.getSwitch_icon()).getSwOnId())
                        .into(img_switch);
            }
            else {
                Glide.with(context)
                        .load(databaseHandler.getSwitchIconData(beanSwitch.getSwitch_icon()).getSwOffId())
                        .into(img_switch);
            }
        }catch (Exception e){
            //C.connectionError(context);
        }
       // }
        /*else {
            if(beanSwitch.getIsSwitchOn()==1)
                img_switch.setImageResource(R.drawable.fan_on);
            else img_switch.setImageResource(R.drawable.fan_off);
        }*/

        img_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               if(beanSwitch.getIsSwitchOn()==1){
                   
                   setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                           false, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch());
               }
                else {
                   setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                           true, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch());
               }
            }
        });


        if(switchList.get(position).getIsSwitch().equalsIgnoreCase("s")){
            layout_dimmerBar.setVisibility(View.INVISIBLE);
            dimmerProgress.setVisibility(View.GONE);
            dimmerProgress.setEnabled(false);
            txt_progress.setVisibility(View.GONE);
        }
        else if(switchList.get(position).getIsSwitch().equalsIgnoreCase("d")){
            layout_dimmerBar.setVisibility(View.VISIBLE);
            img_switch.setVisibility(View.VISIBLE);
            dimmerProgress.setVisibility(View.GONE);
            txt_progress.setVisibility(View.GONE);
        }

        if(fromActivity.equalsIgnoreCase(Globals.FAVOURITE)){
            txt_group.setVisibility(View.VISIBLE);
        }
        else if(fromActivity.equalsIgnoreCase(Globals.GROUP)){
            txt_group.setVisibility(View.GONE);
        }

        txt_group_switch.setText(switchList.get(position).getSwitch_name());
        txt_group.setText(databaseHandler.getGroupnameById(groupid));
        txt_progress.setText(String.valueOf(beanSwitch.getDimmerValue()));

        dimmerProgress.setProgress(beanSwitch.getDimmerValue());
        edt_dimmerValue.setText(String.valueOf(beanSwitch.getDimmerValue()));

        img_plusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue=edt_dimmerValue.getText().toString();

                int dimmerVal=0;

                if(dimmerValue.isEmpty()){
                    edt_dimmerValue.setText(String.valueOf("0"));
                }
                else{
                    dimmerVal=Integer.parseInt(dimmerValue);
                    if(!(dimmerVal>=5)){
                        dimmerVal++;
                        edt_dimmerValue.setText(String.valueOf(dimmerVal));
                    }
                }
            }
        });

        img_minusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue=edt_dimmerValue.getText().toString();

                int dimmerVal=0;

                if(dimmerValue.isEmpty()){
                    edt_dimmerValue.setText(String.valueOf("0"));
                }
                else{
                    dimmerVal=Integer.parseInt(dimmerValue);
                    if(!(dimmerVal<=0)){
                        dimmerVal--;
                        edt_dimmerValue.setText(String.valueOf(dimmerVal));
                    }
                }
            }
        });

        edt_dimmerValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String dimmerValue=edt_dimmerValue.getText().toString();

                int dimmerVal=0;

                if(dimmerValue.isEmpty()){
                    dimmerVal=0;
                }
                else{
                    dimmerVal=Integer.parseInt(dimmerValue);
                }

                switchList.get(position).setDimmerValue(dimmerVal);
                databaseHandler.setDimmerValue(beanSwitch.getSwitch_id(),dimmerVal);
                if(beanSwitch.getIsSwitchOn()==1) {
                    setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                            true, switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch());
                }
                else {
                    setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                            false,  switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch());
                }
            }
        });

        if(beanSwitch.getIsFavourite()==0){
            img_fav.setImageResource(R.drawable.new_heart_off);
        }
        else img_fav.setImageResource(R.drawable.new_heart_on);

        img_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(beanSwitch.getIsFavourite()==0){
                    beanSwitch.setIsFavourite(1);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(),true);
                    img_fav.setImageResource(R.drawable.new_heart_on);
                }
                else {
                    beanSwitch.setIsFavourite(0);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(),false);
                    img_fav.setImageResource(R.drawable.new_heart_off);
                }
            }
        });


        option_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.showOptionMenu(position, option_menu);
            }
        });

        dimmerProgress.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int progress, boolean fromUser) {
                if(seekBar.getProgress()==6){
                    seekBar.setProgress(0);
                }
                beanSwitch.setDimmerValue(seekBar.getProgress());
                databaseHandler.setDimmerValue(beanSwitch.getSwitch_id(),seekBar.getProgress());
                if(beanSwitch.getIsSwitchOn()==1) {
                    setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                            true, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch());
                }
                else {
                    setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                            false, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch());
                }
                txt_progress.setText(String.valueOf(beanSwitch.getDimmerValue()));
                Log.e("Fan Value : ", String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
                if(callback!=null){
                    callback.stopScrolling();
                }
                Log.d("Track Touch :","Track touched..");
            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
                if(callback!=null){
                    callback.startScrolling();
                }
                Log.d("Track Touch :","Track left..");
            }
        });

        return view;
    }

    public void setCallBack(DimmerChangeCallBack dimmerChangeCallBack){
        this.callback=dimmerChangeCallBack;
    }

    public interface DimmerChangeCallBack{
        public void stopScrolling();
        public void startScrolling();
        public void showOptionMenu(int position,View view);
    }

    public int getSwitchIdAtPosition(int position){
        int switchId=0;

        switchId=switchList.get(position).getSwitch_id();

        return switchId;
    }

    public String getSwitchName(int position){
        String switchName="";

        switchName=switchList.get(position).getSwitch_name();

        return switchName;
    }

    public boolean isSwitch(int position){
        if(switchList.get(position).getIsSwitch().equalsIgnoreCase("s")){
            return true;
        }
        return false;
    }

    public String getSwitchNumber(int position){
        return switchList.get(position).getSwitch_btn_num();
    }

    public String getSlaveIDForSwitch(int position){
        return switchList.get(position).getSwitchInSlave();
    }

    public int getGroupIdAtPosition(int position){
        int groupId=0;

        groupId=switchList.get(position).getSwitchInGroup();

        return groupId;
    }

    @Override
    public void notifyDataSetChanged() {
        switchList.clear();
        if(fromActivity.equals(Globals.FAVOURITE)){
            switchList=databaseHandler.getAllSwitchesInFavourite();
        }
        else {
            switchList=databaseHandler.getAllSwitchesByGroupId(groupid);
        }
        super.notifyDataSetChanged();
    }

    public String setSwitchItem(String slave_hex_id, String button, String isOn, String dval){
        String msg="";

        //Log.e("Method Invoke","Method started");

        for(int i=0; i<switchList.size();i++){
            Bean_Switch beanSwitch=switchList.get(i);

            //Log.e("Button by srvr",button);
            //Log.e("Button by db", beanSwitch.getSwitch_btn_num());

            if(beanSwitch.getSwitchInSlave().equals(slave_hex_id)){
                if(beanSwitch.getSwitch_btn_num().equals(button)){
                    //Log.e("Button :","Found");
                    if(isOn.equalsIgnoreCase("A")) {
                        //Log.e("Switch sts :", "ON");
                        switchList.get(i).setIsSwitchOn(1);
                        databaseHandler.setSwitchIsOnById(switchList.get(i).getSwitch_id(),true);
                    }
                    else {
                        //Log.e("Switch sts :", "OFF");
                        switchList.get(i).setIsSwitchOn(0);
                        databaseHandler.setSwitchIsOnById(switchList.get(i).getSwitch_id(),false);
                    }

                    if(beanSwitch.getIsSwitch().equalsIgnoreCase("d")){
                        //Log.e("Dimmer sts :", ""+dval);
                        switchList.get(i).setDimmerValue(Integer.parseInt(dval));
                        databaseHandler.setDimmerValue(switchList.get(i).getSwitch_id(),Integer.parseInt(dval));
                    }
                    msg="success";
                    //Log.e("Data :","Braek");
                    break;
                }
            }
        }

        notifyDataSetChanged();

        return msg;
    }

    public String setSwitchOnOff(String slave_hex_id,int switch_id,String switch_button_num,boolean onOff,int dval, String type){
        String msg="";

        try {
            if (netcheck.isOnline()) {

                JSONObject object=new JSONObject();
                if(databaseHandler.getSlaveUserType(slave_hex_id).equalsIgnoreCase(Cmd.LIN))
                    object.put("cmd",Cmd.UPD);
                else
                    object.put("cmd",Cmd.UUP);

                object.put("slave",slave_hex_id);
                object.put("token",databaseHandler.getSlaveToken(slave_hex_id));

                String mqttCommand = switch_button_num;

                if (onOff) {
                    mqttCommand += "A";
                } else {
                    mqttCommand += "0";
                }

                if (type.equalsIgnoreCase("s")) mqttCommand += "X";
                else mqttCommand += String.valueOf(dval);

                object.put("data",mqttCommand);

                String command=object.toString();

                Log.e("Online Sts", "" + preference.isOnline());
                Log.e("Command", command);

                if(preference.getCurrentIPAddr().equals(databaseHandler.getMasterIPBySlaveID(slave_hex_id))){
                    switchSelection.sendUDPCommand(command);
                }
                else {
                   new SendMQTTMsg(command,databaseHandler.getSlaveTopic(slave_hex_id)).execute();
                }
                /*if (preference.isOnline() || (!preference.isOnline() && !preference.getCurrentIPAddr().equalsIgnoreCase(databaseHandler.getMasterIPBySlaveID(slave_hex_id)))) {

                } else {

                }*/


            } else {
                Toast.makeText(context, "No internet connection found,\nplease check your connection first",
                        Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Log.e("Exception",e.getMessage());
        }

        return msg;
    }

    private class SendMQTTMsg extends AsyncTask<Void, Void, Void>{

        String command="";
        String topic="";

        public SendMQTTMsg(String command, String topic) {
            this.command = command;
            this.topic = topic;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                mqttClient = new MqttClient(AppConstant.MQTT_BROKER_URL, clientId, new MemoryPersistence());
                MqttConnectOptions connectOptions = new MqttConnectOptions();
                connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                connectOptions.setPassword(AppConstant.getPassword());
                mqttClient.connect(connectOptions);

                Log.e("Command Fired UPD :", command);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(topic+ AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                Log.e("topic msg", topic + AppConstant.MQTT_PUBLISH_TOPIC + " " + mqttMessage);
                mqttClient.disconnect();
            } catch (MqttException e) {
                Log.e("Exception : ", e.getMessage());
                e.printStackTrace();
            }

            return null;
        }
    }

    public String setTouchUserLock(String slave_hex_id,String switch_button_num,boolean lock,boolean isTouchLock){
        String msg="";

        List<Bean_Switch> switchesBySlave=databaseHandler.getSwitchDataBySlave(slave_hex_id);

        String lockData="";

//        for(int i=0; i<switchesBySlave.size(); i++){
//            if(isTouchLock){
//                if(switchesBySlave.get(i).getSwitch_btn_num().equalsIgnoreCase(switch_button_num)){
//                    if(lock){
//                        lockData += "Y";
//                    }
//                    else {
//                        lockData += "N";
//                    }
//                }
//                else {
//                    lockData +=switchesBySlave.get(i).getTouchLock();
//                }
//            }
//            else {
//                if(switchesBySlave.get(i).getSwitch_btn_num().equalsIgnoreCase(switch_button_num)){
//                    if(lock){
//                        lockData += "Y";
//                    }
//                    else {
//                        lockData += "N";
//                    }
//                }
//                else {
//                    lockData +=switchesBySlave.get(i).getUserLock();
//                }
//            }
//        }
        lockData += switch_button_num;
        if(lock){
            lockData += "Y";
        }
        else {
            lockData += "N";
        }



        if(netcheck.isOnline()){

            JSONObject object=new JSONObject();
            try{
                if(isTouchLock){
                    object.put("cmd", Cmd.TL1);
                }
                else {
                    object.put("cmd",Cmd.UL1);
                }

                object.put("slave",slave_hex_id);
                object.put("data",lockData);
                object.put("token",databaseHandler.getSlaveToken(slave_hex_id));

            }catch (JSONException e){
                Log.e("Exception",e.getMessage());
            }

            String mqttCommand=object.toString();

            Log.e("Online Sts",""+preference.isOnline());
            Log.e("Command",mqttCommand);

            if(preference.getCurrentIPAddr().equals(databaseHandler.getMasterIPBySlaveID(slave_hex_id))){
                switchSelection.sendUDPCommand(mqttCommand);
            }
            else {
                new SendMQTTMsg(mqttCommand,databaseHandler.getSlaveTopic(slave_hex_id)).execute();
            }

            /*if(preference.isOnline() || (!preference.isOnline() && !preference.getCurrentIPAddr().equalsIgnoreCase(databaseHandler.getMasterIPBySlaveID(slave_hex_id)))){
            }
            else {

            }*/


        }
        else {
            Toast.makeText(context, "No internet connection found,\nplease check your connection first",
                    Toast.LENGTH_SHORT).show();
        }

        return msg;
    }

    public interface OnSwitchSelection{
        void sendUDPCommand(String command);
    }


}
