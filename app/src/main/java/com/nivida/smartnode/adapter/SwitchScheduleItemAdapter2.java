package com.nivida.smartnode.adapter;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.kyleduo.switchbutton.SwitchButton;
import com.nivida.smartnode.R;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_ScheduleItem;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.NetworkUtility;

import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * Created by Chintak Patel on 20-Aug-16.
 */
public class SwitchScheduleItemAdapter2 extends BaseAdapter {
    Context context;
    List<Bean_ScheduleItem> scheduleItemList;
    DatabaseHandler databaseHandler;
    Activity activity;
    NetworkUtility networkUtility;

    MqttClient mqttClient;
    String clientID=MqttClient.generateClientId();

    int enableForPosition=0;
    boolean schduleEnabled=false;

    AppPreference preference;
    int switchID=0;

    OnScheduleViewSelection onScheduleViewSelection;

    public SwitchScheduleItemAdapter2(Activity activity,List<Bean_ScheduleItem> scheduleItemList, OnScheduleViewSelection onScheduleViewSelection,int switchID)  {
        this.context=activity.getApplicationContext();
        this.scheduleItemList=scheduleItemList;
        databaseHandler=new DatabaseHandler(context);
        networkUtility=new NetworkUtility(context);
        this.activity=activity;
        this.onScheduleViewSelection=onScheduleViewSelection;
        preference=new AppPreference(context);
        this.switchID=switchID;
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
        else {
            view=convertView;
        }

        final Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);

        //Log.e("Sch time my",scheduleItemList.get(position).getTime());

        /*Log.d("sch item",scheduleItem.toString());
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
        Log.d("sch item switch ON",scheduleItem.isSwitchOn()+"");*/


        final TextView txt_time=(TextView) view.findViewById(R.id.txt_time);
        final SwitchButton btn_OnOff=(SwitchButton) view.findViewById(R.id.btn_OnOff);

        txt_time.setTypeface(C.raleway(context));

        final CheckBox chk_enable=(CheckBox) view.findViewById(R.id.chk_enable);

        final TextView sunday=(TextView) view.findViewById(R.id.sunday);
        final TextView monday=(TextView) view.findViewById(R.id.monday);
        final TextView tuesday=(TextView) view.findViewById(R.id.tuesday);
        final TextView wednesday=(TextView) view.findViewById(R.id.wednesday);
        final TextView thursday=(TextView) view.findViewById(R.id.thursday);
        final TextView friday=(TextView) view.findViewById(R.id.friday);
        final TextView saturday=(TextView) view.findViewById(R.id.saturday);

        LinearLayout layout_days=(LinearLayout) view.findViewById(R.id.layout_days);
        LinearLayout layout_repeatTime=(LinearLayout) view.findViewById(R.id.layout_repeatTime);

        RadioGroup rdg_schType=(RadioGroup) view.findViewById(R.id.rdg_schType);
        RadioButton rdo_schOnce=(RadioButton) view.findViewById(R.id.rdo_schOnce);
        RadioButton rdo_schDaily=(RadioButton) view.findViewById(R.id.rdo_schDaily);
        RadioButton rdo_schRepeat=(RadioButton) view.findViewById(R.id.rdo_schRepeat);

        final DiscreteSeekBar dimmer_value=(DiscreteSeekBar) view.findViewById(R.id.dimmerProgress);

        /*ImageView btn_copy=(ImageView) view.findViewById(R.id.copy);
        ImageView btn_save=(ImageView) view.findViewById(R.id.save);
        ImageView btn_delete=(ImageView) view.findViewById(R.id.delete);*/

        Button btn_save=(Button) view.findViewById(R.id.btn_save);
        Button btn_delete=(Button) view.findViewById(R.id.btn_delete);

        if(scheduleItemList.get(position).isOnce() && scheduleItemList.get(position).areNoneDaysSelected()){
            scheduleItemList.get(position).setSingleDay(getCurrentDay()-1,"Y");

        }
        else if(scheduleItemList.get(position).isDaily() && !scheduleItemList.get(position).areAllDaysSelected()){
            scheduleItemList.get(position).setAllDaySelected();
        }
        else if(scheduleItemList.get(position).areNoneDaysSelected()){
            scheduleItemList.get(position).setSingleDay(getCurrentDay()-1,"Y");
        }

        Log.e("Slot Num",scheduleItemList.get(position).getSlot_num());



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

            txt_time.setEnabled(false);
            layout_days.setEnabled(false);
            layout_repeatTime.setEnabled(false);

            monday.setEnabled(false);
            tuesday.setEnabled(false);
            wednesday.setEnabled(false);
            thursday.setEnabled(false);
            friday.setEnabled(false);
            saturday.setEnabled(false);
            sunday.setEnabled(false);

            rdo_schDaily.setEnabled(false);
            rdo_schOnce.setEnabled(false);
            rdo_schRepeat.setEnabled(false);
            dimmer_value.setEnabled(false);
            btn_OnOff.setEnabled(false);
        }
        else {
            chk_enable.setChecked(false);

            txt_time.setEnabled(true);
            layout_days.setEnabled(true);
            layout_repeatTime.setEnabled(true);

            monday.setEnabled(true);
            tuesday.setEnabled(true);
            wednesday.setEnabled(true);
            thursday.setEnabled(true);
            friday.setEnabled(true);
            saturday.setEnabled(true);
            sunday.setEnabled(true);

            rdo_schDaily.setEnabled(true);
            rdo_schOnce.setEnabled(true);
            rdo_schRepeat.setEnabled(true);
            dimmer_value.setEnabled(true);
            btn_OnOff.setEnabled(true);
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
                        C.Toast(context,"Please Select Time");
                        chk_enable.setChecked(false);
                    }
                    else if(!checkDaysSelected(position)){
                        C.Toast(context,"Please Select at least 1 Day");
                        chk_enable.setChecked(false);
                        databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    }
                    else if(networkUtility.isOnline()){
                        scheduleItemList.get(position).setSchEnabled(true);
                        //Send Command to Server for add schedule
                        enableForPosition=position;
                        schduleEnabled=false;
                        onScheduleViewSelection.getAllSlotsInfo(scheduleItemList.get(position).getSlave_id());
                        //getAvailableSlotInSlave(scheduleItemList.get(position).getSlave_id());
                        databaseHandler.updateScheduleItem(scheduleItemList.get(position));
                    }
                }
                else {
                    if(networkUtility.isOnline()){
                        onScheduleViewSelection.deleteSchedule(position,scheduleItemList.get(position));
                        scheduleItemList.get(position).setSchEnabled(false);
                        scheduleItemList.get(position).setSlot_num("26");
                        chk_enable.setChecked(false);
                    }
                    else {
                        C.Toast(context,context.getString(R.string.nointernet));
                        chk_enable.setChecked(true);
                    }
                    databaseHandler.updateScheduleItem(scheduleItemList.get(position));
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
                            if(selectedHour<10){
                                txt_time.setText( "0"+selectedHour + "-" + selectedMinute);
                                scheduleItemList.get(position).setTime("0"+selectedHour+"-"+selectedMinute);
                            }
                            else {
                                txt_time.setText( selectedHour + "-" + selectedMinute);
                                scheduleItemList.get(position).setTime(selectedHour+"-"+selectedMinute);
                            }
                        }
                        else {
                            if(selectedHour<10){
                                txt_time.setText( "0"+ selectedHour + "-" +"0"+ selectedMinute);
                                scheduleItemList.get(position).setTime("0"+selectedHour+"-0"+selectedMinute);
                            }
                            else {
                                txt_time.setText( selectedHour + "-" +"0"+ selectedMinute);
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
                if(scheduleItemList.get(position).getDays()[0].equalsIgnoreCase("N")){
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                if(scheduleItemList.get(position).getDays()[1].equalsIgnoreCase("N")){
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    if(scheduleItemList.get(position).isOnce())
                        scheduleItemList.get(position).setNoneDaySelected();
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
                    scheduleItemList.get(position).setAllDaySelected();
                    notifyDataSetChanged();
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
                    if(!scheduleItemList.get(position).isSingleDaySelected()){
                        scheduleItemList.get(position).setDefaultDaySelected();
                    }
                    notifyDataSetChanged();
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

        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteItem(position);
            }
        });



        return view;
    }

    public void updateScheduleDeleted(String slotNumber, String slave) {
        databaseHandler.updateScheduleSlot(slotNumber, slave);
        for(int i=0; i<scheduleItemList.size(); i++){
            if(scheduleItemList.get(i).getSlot_num().equals(slotNumber)){
                scheduleItemList.get(i).setSlot_num("26");
                scheduleItemList.get(i).setSchEnabled(false);
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void setHasNoSchedule() {
        for(int i=0; i<scheduleItemList.size(); i++){
            scheduleItemList.get(i).setSlot_num("26");
            scheduleItemList.get(i).setSchEnabled(false);
        }
        notifyDataSetChanged();
    }

    public interface OnScheduleViewSelection{
        void deleteSchedule(int position,Bean_ScheduleItem scheduleItem);
        void getAllSlotsInfo(String slaveID);
        void createSchedule(int position,String createCommand, String slaveID);
    }

    private void updateDatabaseSchedule() {

    }

    private void getAvailableSlotInSlave(String slave_id) {
        String command = AppConstant.START_CMD_SCH_GETALL+slave_id+AppConstant.END_CMD_SCH_GETALL;
        Log.e("command b",command);
        new PublishMessage(command,slave_id).execute();

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
            new PublishMessage(command,scheduleItem.getSlave_id()).execute();
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
        if(!scheduleItemList.get(position).getSlot_num().isEmpty()){
            onScheduleViewSelection.deleteSchedule(position,scheduleItemList.get(position));
            databaseHandler.removeScheduleItem(scheduleItemList.get(position));
            notifyDataSetChanged();
        }
    }

    private void publishCommandForSetSchedule(int position, String slot_number, String slave) throws MqttException {
        Bean_ScheduleItem scheduleItem=scheduleItemList.get(position);
        scheduleItem.setSlot_num(String.valueOf(slot_number));

        if(scheduleItem.getSlave_id().equalsIgnoreCase(slave)) {

            try{
                JSONObject object=new JSONObject();

                object.put("cmd", Cmd.SCH);
                object.put("token",databaseHandler.getSlaveToken(scheduleItem.getSlave_id()));
                object.put("slave",scheduleItem.getSlave_id());

                String data="C-";
                data += scheduleItem.getSlot_num()+"-";
                data +=scheduleItem.getTime()+"-";

                for (int i = 0; i < scheduleItem.getDays().length ; i++) {
                    if(i==scheduleItem.getDays().length-1)
                        data += scheduleItem.getDays()[i]+"-";
                    else
                        data += scheduleItem.getDays()[i];
                }

                data += scheduleItem.getSwitch_btn_num()+"-";

                if(scheduleItem.isSwitchOn())
                    data += "ON-";
                else
                    data += "OF-";

                data += scheduleItem.getDimmerValue()+"-";

                if(scheduleItem.isOnce()){
                    data += "01";
                }
                else {
                    data += "00";
                }

                /*if(scheduleItem.getRepeat()<10){
                    data += "0"+scheduleItem.getRepeat();
                }
                else {
                    data += scheduleItem.getRepeat();
                }*/

                object.put("data",data);

                String command=object.toString();

                Log.e("command",command);

                onScheduleViewSelection.createSchedule(position,command,scheduleItem.getSlave_id());

            }catch (Exception e){
                Log.e("Exception",e.getMessage());
            }
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

    public void setAvailableSlotForSlave(String slave, String slot_number) {
        int slotNum=Integer.parseInt(slot_number);
        Log.d("Slave & Slot",slave+" "+slot_number);
        if(slotNum>25){
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
        String slaveID="";

        public PublishMessage(String command,String slaveID){
            this.command=command;
            this.slaveID=slaveID;
        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.e("command",command);
            if(networkUtility.isOnline()){
                try {
                    mqttClient=new MqttClient(AppConstant.MQTT_BROKER_URL,C.MQTT_ClientID,new MemoryPersistence());
                    MqttConnectOptions connectOptions=new MqttConnectOptions();
                    connectOptions.setUserName(AppConstant.MQTT_USERNAME);
                    connectOptions.setPassword(AppConstant.getPassword());
                    mqttClient.connect(connectOptions);
                    MqttMessage mqttMessage=new MqttMessage(command.getBytes());
                    mqttMessage.setRetained(true);
                    mqttMessage.setQos(1);
                    mqttClient.publish(databaseHandler.getSlaveTopic(slaveID)+AppConstant.MQTT_PUBLISH_TOPIC,mqttMessage);
                    mqttClient.disconnect();
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }
    }

    public void updateAllItemstoDatabase(){
        for(int i=0; i<scheduleItemList.size(); i++){
            databaseHandler.updateScheduleItem(scheduleItemList.get(i));
        }
    }

    @Override
    public void notifyDataSetChanged() {
        updateAllItemstoDatabase();
        scheduleItemList=databaseHandler.getAllSchedulesForSwitch(switchID);
        super.notifyDataSetChanged();
    }

    private int getCurrentDay(){
        Calendar calendar=Calendar.getInstance();
        return calendar.get(Calendar.DAY_OF_WEEK);
    }
}
