package com.nivida.smartnode.adapter;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.nivida.smartnode.R;
import com.nivida.smartnode.SceneActivity;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.beans.Bean_ScheduleItem;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.NetworkUtility;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Chintak Patel on 20-Aug-16.
 */
public class SwitchScheduleItemAdapter extends BaseAdapter {
    Context context;
    List<Bean_ScheduleItem> scheduleItemList;
    DatabaseHandler databaseHandler;
    Activity activity;
    NetworkUtility networkUtility;

    MqttClient mqttClient;
    String clientID=MqttClient.generateClientId();

    int enableForPosition=0;
    boolean schduleEnabled=false;

    public SwitchScheduleItemAdapter(Context context, List<Bean_ScheduleItem> scheduleItemList, Activity activity)  {
        this.context=context;
        this.scheduleItemList=scheduleItemList;
        databaseHandler=new DatabaseHandler(context);
        networkUtility=new NetworkUtility(context);
        this.activity=activity;
    }

    @Override
    public int getCount() {
        return scheduleItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return scheduleItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view=convertView;

        if(view == null){
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view=inflater.inflate(R.layout.custom_switch_schedule_row,parent,false);
        }

        final Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);

        Log.e("Sch time my",scheduleItemList.get(position).getTime());

        Log.d("sch item",scheduleItem.toString());
        Log.d("sch item days",scheduleItem.getDays().toString());
        Log.d("sch item dimmer",scheduleItem.getDimmerValue());
        Log.d("sch item slave",scheduleItem.getSlave_id());
        Log.d("sch item slot_num",scheduleItem.getSlot_num());
        Log.d("sch item switch num",scheduleItem.getSwitch_btn_num());
        Log.d("sch item time",scheduleItem.getTime());
        Log.d("sch item repeat num",scheduleItem.getRepeat()+"");
        Log.d("sch item repeated",scheduleItem.isRepeated()+"");
        Log.d("sch item daily",scheduleItem.isDaily()+"");
        Log.d("sch item once",scheduleItem.isOnce()+"");
        Log.d("sch item enabled",scheduleItem.isSchEnabled()+"");
        Log.d("sch item switch ON",scheduleItem.isSwitchOn()+"");


        final TextView txt_time=(TextView) view.findViewById(R.id.txt_time);
        final Switch btn_OnOff=(Switch) view.findViewById(R.id.btn_OnOff);

        final CheckBox chk_enable=(CheckBox) view.findViewById(R.id.chk_enable);

        final TextView sunday=(TextView) view.findViewById(R.id.sunday);
        final TextView monday=(TextView) view.findViewById(R.id.monday);
        final TextView tuesday=(TextView) view.findViewById(R.id.tuesday);
        final TextView wednesday=(TextView) view.findViewById(R.id.wednesday);
        final TextView thursday=(TextView) view.findViewById(R.id.thursday);
        final TextView friday=(TextView) view.findViewById(R.id.friday);
        final TextView saturday=(TextView) view.findViewById(R.id.saturday);

        RadioGroup rdg_schType=(RadioGroup) view.findViewById(R.id.rdg_schType);
        RadioButton rdo_schOnce=(RadioButton) view.findViewById(R.id.rdo_schOnce);
        RadioButton rdo_schDaily=(RadioButton) view.findViewById(R.id.rdo_schDaily);
        RadioButton rdo_schRepeat=(RadioButton) view.findViewById(R.id.rdo_schRepeat);

        final DiscreteSeekBar dimmer_value=(DiscreteSeekBar) view.findViewById(R.id.dimmerProgress);

        ImageView btn_copy=(ImageView) view.findViewById(R.id.copy);
        ImageView btn_save=(ImageView) view.findViewById(R.id.save);
        ImageView btn_delete=(ImageView) view.findViewById(R.id.delete);



        if(databaseHandler.isSwitch(scheduleItem.getSwitch_btn_num(), scheduleItem.getSlave_id())){
           dimmer_value.setVisibility(View.GONE);
        }
        else {
            dimmer_value.setVisibility(View.VISIBLE);
            dimmer_value.setProgress(Integer.parseInt(scheduleItem.getDimmerValue()));
        }

        dimmer_value.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
            @Override
            public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
                if(seekBar.getProgress()==6){
                    seekBar.setProgress(0);
                }

                scheduleItemList.get(position).setDimmerValue(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(DiscreteSeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(DiscreteSeekBar seekBar) {

            }
        });


        if(scheduleItemList.get(position).getTime()!=null && !scheduleItemList.get(position).getTime().equalsIgnoreCase("")){
            txt_time.setText(scheduleItemList.get(position).getTime());
        }
        else
        {
            Calendar mcurrentTime = Calendar.getInstance();
            int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
            int minute = mcurrentTime.get(Calendar.MINUTE);
            String hourS=String.valueOf(hour);
            String minuteS=String.valueOf(minute);
            if(hour<10)
                hourS = "0"+hourS;
            if(minute<10)
                minuteS="0"+minuteS;
            txt_time.setText(hourS+"-"+minuteS);
            scheduleItemList.get(position).setTime(hourS+"-"+minuteS);
            Log.e("time set",hour+"-"+minute);
        }

        if(scheduleItemList.get(position).isSchEnabled()){
            chk_enable.setChecked(true);
        }
        else {
            chk_enable.setChecked(false);
        }

        if(scheduleItemList.get(position).isRepeated()){
            rdo_schRepeat.setChecked(true);
        }
        else {
            rdo_schRepeat.setChecked(false);
        }

        if(scheduleItemList.get(position).isDaily()){
            rdo_schDaily.setChecked(true);
        }

        if(scheduleItemList.get(position).isOnce()){
            rdo_schOnce.setChecked(true);
        }

        rdg_schType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rdo_schOnce:
                        scheduleItemList.get(position).setRepeat(1);
                        updateDatabaseSchedule();
                        break;
                    case R.id.rdo_schDaily:
                        scheduleItemList.get(position).setRepeat(0);
                        break;
                    case R.id.rdo_schRepeat:
                        scheduleItemList.get(position).setRepeat(0);
                        break;
                }
            }
        });

        chk_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(txt_time.getText().toString().trim().isEmpty() || txt_time.getText().toString().contains("SELECT")){
                        Toast.makeText(context,"Please Select Time",Toast.LENGTH_SHORT).show();
                        chk_enable.setChecked(false);
                    }
                    else if(!checkDaysSelected(position)){
                        Toast.makeText(context,"Please Select at least 1 day",Toast.LENGTH_SHORT).show();
                        chk_enable.setChecked(false);
                        databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    }
                    else if(networkUtility.isOnline()){
                        scheduleItemList.get(position).setSchEnabled(true);
                        //Send Command to Server for add schedule
                        enableForPosition=position;
                        schduleEnabled=false;
                        getAvailableSlotInSlave(scheduleItemList.get(position).getSlave_id());
                        databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    }
                }
                else {
                    databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    if(networkUtility.isOnline()){
                        scheduleItemList.get(position).setSchEnabled(false);
                        //Send Command to Server for Delete Schedule
                        try {
                            publishCommandForDeleteSchedule(position);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        chk_enable.setChecked(false);
                    }
                    else {
                        chk_enable.setChecked(true);
                    }


                }
            }
        });

        if(scheduleItemList.get(position).isSwitchOn()){
            btn_OnOff.setChecked(true);
        }
        else {
            btn_OnOff.setChecked(false);
        }

        btn_OnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                scheduleItemList.get(position).setSwitchOn(isChecked);
            }
        });

        txt_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(activity, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {

                        if(selectedMinute>=10){
                            txt_time.setText( selectedHour + ":" + selectedMinute);
                            if(selectedHour<10){
                                scheduleItemList.get(position).setTime("0"+selectedHour+"-"+selectedMinute);
                            }
                            else {
                                scheduleItemList.get(position).setTime(selectedHour+"-"+selectedMinute);
                            }
                        }
                        else {
                            txt_time.setText( selectedHour + ":" +"0"+ selectedMinute);

                            if(selectedHour<10){
                                scheduleItemList.get(position).setTime("0"+selectedHour+"-0"+selectedMinute);
                            }
                            else {
                                scheduleItemList.get(position).setTime(selectedHour+"-0"+selectedMinute);
                            }
                        }

                        Log.e("Selected Time",scheduleItemList.get(position).getTime());


                    }
                }, hour, minute, true);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();
            }
        });

        for(int i=0; i<scheduleItemList.get(position).getDays().length; i++){
            switch (i){
                case 0:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        sunday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        sunday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 1:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        monday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        monday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 2:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        tuesday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        tuesday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 3:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        wednesday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        wednesday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 4:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        thursday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        thursday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 5:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        friday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        friday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
                case 6:
                    if(scheduleItemList.get(position).getDays()[i].equalsIgnoreCase("N"))
                        saturday.setBackgroundResource(R.drawable.bg_days_off);
                    else
                        saturday.setBackgroundResource(R.drawable.bg_days_on);
                    break;
            }
        }

        sunday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("S day b",scheduleItemList.get(position).getDays()[0]);

                if(scheduleItemList.get(position).getDays()[0].equalsIgnoreCase("N")){
                    sunday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(0,"Y");
                }
                else{
                    sunday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(0,"N");
                }

                notifyDataSetChanged();
                Log.e("S day a",scheduleItemList.get(position).getDays()[0]);
            }
        });

        monday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("M day b",scheduleItemList.get(position).getDays()[1]);
                if(scheduleItemList.get(position).getDays()[1].equalsIgnoreCase("N")){
                    monday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(1,"Y");
                }
                else{
                    monday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(1,"N");
                }
                notifyDataSetChanged();
                Log.e("M day a",scheduleItemList.get(position).getDays()[1]);
            }
        });

        tuesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("T day b",scheduleItemList.get(position).getDays()[2]);
                if(scheduleItemList.get(position).getDays()[2].equalsIgnoreCase("N")){
                    tuesday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(2,"Y");
                }
                else{
                    tuesday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(2,"N");
                }
                notifyDataSetChanged();
                Log.e("T day a",scheduleItemList.get(position).getDays()[2]);
            }
        });

        wednesday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(scheduleItemList.get(position).getDays()[3].equalsIgnoreCase("N")){
                    wednesday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(3,"Y");
                }
                else{
                    wednesday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(3,"N");
                }
                notifyDataSetChanged();
            }
        });

        thursday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("T day b",scheduleItemList.get(position).getDays()[4]);
                if(scheduleItemList.get(position).getDays()[4].equalsIgnoreCase("N")){
                    thursday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(4,"Y");
                }
                else{
                    thursday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(4,"N");
                }
                Log.e("T day a",scheduleItemList.get(position).getDays()[4]);
                notifyDataSetChanged();
            }
        });

        friday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(scheduleItemList.get(position).getDays()[5].equalsIgnoreCase("N")){
                    friday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(5,"Y");
                }
                else{
                    friday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(5,"N");
                }
                notifyDataSetChanged();
            }
        });

        saturday.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(scheduleItemList.get(position).getDays()[6].equalsIgnoreCase("N")){
                    saturday.setBackgroundResource(R.drawable.bg_days_on);
                    scheduleItemList.get(position).setPerticularDay(6,"Y");
                }
                else{
                    saturday.setBackgroundResource(R.drawable.bg_days_off);
                    scheduleItemList.get(position).setPerticularDay(6,"N");
                }
                notifyDataSetChanged();
            }
        });

        rdo_schRepeat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    scheduleItemList.get(position).setDaily(false);
                    scheduleItemList.get(position).setRepeat(0);
                    scheduleItemList.get(position).setOnce(false);
                    scheduleItemList.get(position).setRepeated(true);
                }
                else {
                    scheduleItemList.get(position).setRepeated(false);
                }
            }
        });

        rdo_schDaily.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    scheduleItemList.get(position).setDaily(true);
                    scheduleItemList.get(position).setRepeat(0);
                    scheduleItemList.get(position).setOnce(false);
                    scheduleItemList.get(position).setRepeated(false);
                }
                else {
                    scheduleItemList.get(position).setDaily(false);
                }
            }
        });

        rdo_schOnce.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    scheduleItemList.get(position).setOnce(true);
                    scheduleItemList.get(position).setRepeat(1);
                    scheduleItemList.get(position).setDaily(false);
                    scheduleItemList.get(position).setRepeated(false);
                }
                else {
                    scheduleItemList.get(position).setOnce(false);
                }
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(txt_time.getText().toString().equalsIgnoreCase("") || txt_time.getText().toString().contains("SELECT")){
                    Toast.makeText(context,"Please Select Time",Toast.LENGTH_SHORT).show();
                }
                else {
                    databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    Toast.makeText(context,"Schedule Saved Successfully",Toast.LENGTH_SHORT).show();
                    notifyDataSetChanged();
                }

            }
        });

        btn_copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bean_ScheduleItem beanScheduleItem=scheduleItemList.get(position);
                beanScheduleItem.setScheduleID(databaseHandler.getNewIDForSCH());
                scheduleItemList.add(beanScheduleItem);
                databaseHandler.addScheduleItem(beanScheduleItem);
                notifyDataSetChanged();
            }
        });

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteItem(scheduleItemList.get(position).getSlot_num());
            }
        });



        return view;
    }

    private void updateDatabaseSchedule() {

    }

    private void getAvailableSlotInSlave(String slave_id) {
        String command = AppConstant.START_CMD_SCH_GETALL+slave_id+AppConstant.END_CMD_SCH_GETALL;
        Log.e("command b",command);
        new PublishMessage(command).execute();

    }

    public boolean checkDaysSelected(int position){
        Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);
        boolean isChecked=false;

        String[] days=scheduleItem.getDays();
        for (String day : days) {
            if (day.equalsIgnoreCase("Y"))
                isChecked = true;
        }
        return isChecked;
    }

    private void publishCommandForDeleteSchedule(int position) throws MqttException {
        Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);

        String command = AppConstant.START_CMD_SCHEDULE+scheduleItem.getSlave_id()+AppConstant.CENETER_CMD_SCHEDULE;

        command += "D-" + scheduleItem.getSlot_num();

        command += AppConstant.END_CMD_SCHEDULE;

        if(networkUtility.isOnline()){
            new PublishMessage(command).execute();
        }
        else {
            Toast.makeText(context,"Internet connection unavailable\nplease connect to internet",Toast.LENGTH_SHORT).show();
        }

    }

    public void setSlotNumber(String slotNumber){
        scheduleItemList.get(enableForPosition).setSlot_num(slotNumber);
        databaseHandler.updateScheduleItem(scheduleItemList.get(enableForPosition));
    }

    public void deleteItem(String slotNumber){
        for(int i=0; i<scheduleItemList.size() ; i++){
            if(scheduleItemList.get(i).getSlot_num().equalsIgnoreCase(slotNumber)){
                databaseHandler.removeScheduleItem(scheduleItemList.get(i));
                scheduleItemList.remove(i);
                notifyDataSetChanged();
            }
        }

    }

    public void deleteItem(int position){
        if(!scheduleItemList.get(position).getSlot_num().equalsIgnoreCase("")){
            try {
                publishCommandForDeleteSchedule(position);
                Toast.makeText(context, "Schedule Deleted Successfully", Toast.LENGTH_SHORT).show();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    private void publishCommandForSetSchedule(int position, int slot_number, String slave) throws MqttException {
        Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);
        scheduleItem.setSlot_num(String.valueOf(slot_number));

        if(scheduleItem.getSlave_id().equalsIgnoreCase(slave)) {

            String command = AppConstant.START_CMD_SCHEDULE + scheduleItem.getSlave_id() + AppConstant.CENETER_CMD_SCHEDULE;
            //String cmd="C-NN-HH-MM-YYNYYNY-NO-ON-5-00";
            command += "C-";

            if(slot_number>=0 && slot_number<10){
                command += "0"+ scheduleItem.getSlot_num() + "-";
            }
            else {
                command += scheduleItem.getSlot_num() + "-";
            }

            command += scheduleItem.getTime() + "-";

            for (int i = 0; i < scheduleItem.getDays().length; i++) {
                command += scheduleItem.getDays()[i];
            }

            command += "-";

            command += scheduleItem.getSwitch_btn_num() + "-";

            if (scheduleItem.isSwitchOn()) {
                command += "ON";
            } else {
                command += "OF";
            }

            command += "-" + scheduleItem.getDimmerValue() + "-";
            if (scheduleItem.getRepeat() >= 10) {
                command += scheduleItem.getRepeat();
            } else {
                command += "0" + scheduleItem.getRepeat();
            }

            command += AppConstant.END_CMD_SCHEDULE;

           new PublishMessage(command).execute();


        }


    }

    private void setAllDaysSelected(int position,boolean selected) {
        String[] days=scheduleItemList.get(position).getDays();

        if(selected){
            for(int i=0;i<days.length;i++){
                scheduleItemList.get(position).setPerticularDay(i,"Y");
            }
        }
    }

    public void setAvailableSlotForSlave(String slave, int slot_number) {

        Log.d("Slave & Slot",slave+" "+slot_number);
        if(slot_number==26){
            scheduleItemList.get(enableForPosition).setSchEnabled(false);
            notifyDataSetChanged();
        } else {
            if(!schduleEnabled){
                try {
                    publishCommandForSetSchedule(enableForPosition, slot_number, slave);
                    schduleEnabled=true;
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }


    }

    private class PublishMessage extends AsyncTask<Void, Void, Void>{

        String command;

        public PublishMessage(String command){
            this.command=command;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.e("command",command);
            if(networkUtility.isOnline()){
                try {
                    mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,clientID,new MemoryPersistence());
                    MqttConnectOptions connectOptions=new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                    MqttMessage mqttMessage=new MqttMessage(command.getBytes());
                    mqttMessage.setRetained(true);
                    mqttMessage.setQos(1);
                    mqttClient.publish(AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }
}
