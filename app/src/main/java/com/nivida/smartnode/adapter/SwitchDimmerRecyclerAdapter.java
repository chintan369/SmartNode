package com.nivida.smartnode.adapter;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nivida.smartnode.Globals;
import com.nivida.smartnode.R;
import com.nivida.smartnode.SetScheduleActivity;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.SmartNode;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.beans.Bean_SwitchIcons;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.NetworkUtility;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pratik on 12 Jun 2017.
 */

public class SwitchDimmerRecyclerAdapter extends RecyclerView.Adapter<SwitchDimmerRecyclerAdapter.MyViewHolder> {

    Activity activity;
    String fromActivity = "";
    DatabaseHandler databaseHandler;
    NetworkUtility netcheck;
    SwitchDimmerOnOffAdapter.OnSwitchSelection switchSelection;
    MqttClient mqttClient;
    String clientId = "";
    List<Bean_SwitchIcons> switchIconsList = new ArrayList<>();
    int groupid = 0;
    Handler mHandler;
    Runnable mRunnable;
    private List<Bean_Switch> switchList = new ArrayList<>();
    private int callCount = 0;
    private SwitchDimmerOnOffAdapter.DimmerChangeCallBack callback;

    public SwitchDimmerRecyclerAdapter(List<Bean_Switch> switchList, Activity activity, String fromActivity, SwitchDimmerOnOffAdapter.OnSwitchSelection switchSelection) {
        this.switchList = switchList;
        this.activity = activity;
        this.fromActivity = fromActivity;
        databaseHandler = new DatabaseHandler(activity);
        netcheck = new NetworkUtility(activity);
        this.switchSelection = switchSelection;
        clientId = MqttClient.generateClientId();
        switchIconsList = databaseHandler.getAllSwitchIconData();
        if (switchList.size() > 0) {
            this.groupid = switchList.get(0).getSwitchInGroup();
        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.switch_dimmer_onoff, parent, false);

        return new MyViewHolder(itemView);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int pos) {
        final Bean_Switch beanSwitch = switchList.get(pos);

        final int position = pos;

        if (!SmartNode.slavesInLocal.contains(beanSwitch.getSwitchInSlave())) {
            if (!SmartNode.slavesWorking.contains(beanSwitch.getSwitchInSlave())) {
                Glide.with(activity)
                        .load(R.drawable.cloud_nointernet)
                        .into(holder.cloud);
                holder.img_fav.setEnabled(false);
                holder.img_minusDimmer.setEnabled(false);
                holder.img_plusDimmer.setEnabled(false);
                holder.img_schedule.setEnabled(false);
                holder.img_tlock.setEnabled(false);
                holder.img_ulock.setEnabled(false);
                holder.option_menu.setEnabled(false);
            } else {
                holder.img_fav.setEnabled(true);
                holder.img_minusDimmer.setEnabled(true);
                holder.img_plusDimmer.setEnabled(true);
                holder.img_schedule.setEnabled(true);
                holder.img_tlock.setEnabled(true);
                holder.img_ulock.setEnabled(true);
                holder.option_menu.setEnabled(true);
                Glide.with(activity)
                        .load(R.drawable.cloud_internet)
                        .into(holder.cloud);
            }
            holder.cloud.setVisibility(View.VISIBLE);
        } else {
            holder.img_fav.setEnabled(true);
            holder.img_minusDimmer.setEnabled(true);
            holder.img_plusDimmer.setEnabled(true);
            holder.img_schedule.setEnabled(true);
            holder.img_tlock.setEnabled(true);
            holder.img_ulock.setEnabled(true);
            holder.option_menu.setEnabled(true);
            holder.cloud.setVisibility(View.GONE);
        }

        if (beanSwitch.getIsSwitch().equals("s")) {
            holder.img_plusDimmer.setVisibility(View.GONE);
            holder.img_minusDimmer.setVisibility(View.GONE);
        } else {
            holder.img_plusDimmer.setVisibility(View.VISIBLE);
            holder.img_minusDimmer.setVisibility(View.VISIBLE);
        }

        try {
            if (!beanSwitch.isLoading()) {
                holder.progressLoading.setVisibility(View.GONE);
                holder.img_switch.setVisibility(View.VISIBLE);

                if (beanSwitch.getIsSwitch().equalsIgnoreCase("s")) {
                    if (beanSwitch.getIsSwitchOn() == 1) {
                        SmartNode.picassoInstance.load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), true))
                                .into(holder.img_switch);

                    } else {
                        SmartNode.picassoInstance.load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), false))
                                .into(holder.img_switch);
                    }
                } else {
                    int dimmerValID = R.drawable.dimmer_0;
                    if (beanSwitch.getDimmerValue() == 0) {
                        dimmerValID = R.drawable.dimmer_0;
                    } else if (beanSwitch.getDimmerValue() == 1) {
                        dimmerValID = R.drawable.dimmer_1;
                    } else if (beanSwitch.getDimmerValue() == 2) {
                        dimmerValID = R.drawable.dimmer_2;
                    } else if (beanSwitch.getDimmerValue() == 3) {
                        dimmerValID = R.drawable.dimmer_3;
                    } else if (beanSwitch.getDimmerValue() == 4) {
                        dimmerValID = R.drawable.dimmer_4;
                    } else if (beanSwitch.getDimmerValue() == 5) {
                        dimmerValID = R.drawable.dimmer_5;
                    }

                    SmartNode.picassoInstance.load(dimmerValID)
                            .into(holder.img_switch);

                    if (beanSwitch.getIsSwitchOn() == 1) {
                        holder.img_switch.setColorFilter(Color.parseColor("#E6D01F"));
                    } else {
                        holder.img_switch.setColorFilter(Color.parseColor("#666666"));
                    }

                }

            } else {
                holder.progressLoading.setVisibility(View.VISIBLE);
                holder.img_switch.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            //C.connectionError(context);
        }

        if (beanSwitch.getTouchLock().equalsIgnoreCase("Y")) {
            holder.img_tlock.setImageResource(R.drawable.tlock);
        } else {
            holder.img_tlock.setImageResource(R.drawable.tunlock);
        }

        if (beanSwitch.getUserLock().equalsIgnoreCase("Y")) {
            holder.img_ulock.setImageResource(R.drawable.lock);
        } else {
            holder.img_ulock.setImageResource(R.drawable.unlock);
        }

        if (beanSwitch.getHasSchedule() == 0) {
            holder.img_schedule.setImageResource(R.drawable.no_schedule);
        } else {
            holder.img_schedule.setImageResource(R.drawable.has_schedule);
        }

        holder.img_tlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!beanSwitch.isLoading()) {
                    if (beanSwitch.getTouchLock().equalsIgnoreCase("N")) {
                        setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), true, true, position);
                    } else {
                        setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), false, true, position);
                    }
                }

            }
        });

        holder.img_ulock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (beanSwitch.getSlaveUserType().equalsIgnoreCase(Cmd.ULN)) {
                    C.Toast(activity, "Guest don't have privilleges to perform this action");
                } else {
                    if (!beanSwitch.isLoading()) {
                        if (beanSwitch.getUserLock().equalsIgnoreCase("N")) {
                            setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), true, false, position);
                        } else {
                            setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), false, false, position);
                        }
                    }
                }
            }
        });

        holder.img_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, SetScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("group_id", groupid);
                intent.putExtra("switchID", switchList.get(position).getSwitch_id());
                intent.putExtra("switchName", switchList.get(position).getSwitch_name());
                activity.startActivity(intent);
            }
        });


        holder.img_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!beanSwitch.isLoading()) {
                    if (!SmartNode.isConnectedToInternet && !SmartNode.slavesInLocal.contains(beanSwitch.getSwitchInSlave())) {
                        C.Toast(activity, Cmd.INTERNET_UNAVAILABLE);
                    } else if (!SmartNode.slavesWorking.contains(beanSwitch.getSwitchInSlave())) {
                        C.Toast(activity, Cmd.DEVICE_OFFLINE);
                    } else {
                        if (beanSwitch.getIsSwitchOn() == 1) {
                            setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                    false, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                        } else {
                            setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                    true, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                        }
                    }

                }
            }
        });


        /*if (switchList.get(position).getIsSwitch().equalsIgnoreCase("s")) {
            if (position % 2 == 0 && position < getItemCount() - 1 && switchList.get(position + 1).getIsSwitch().equalsIgnoreCase("d")) {
                holder.layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else if (position % 2 > 0 && switchList.get(position - 1).getIsSwitch().equalsIgnoreCase("d")) {
                holder.layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else {
                holder.layout_dimmerBar.setVisibility(View.GONE);
            }

            holder.txt_progress.setVisibility(View.GONE);
        } else if (switchList.get(position).getIsSwitch().equalsIgnoreCase("d")) {
            holder.layout_dimmerBar.setVisibility(View.VISIBLE);
            holder.txt_progress.setVisibility(View.GONE);
        }*/

        if (fromActivity.equalsIgnoreCase(Globals.FAVOURITE)) {
            holder.txt_group.setVisibility(View.VISIBLE);
            holder.txt_group.setText(beanSwitch.getSwitchGroupName());
        } else if (fromActivity.equalsIgnoreCase(Globals.GROUP)) {
            holder.txt_group.setVisibility(View.GONE);
        }

        holder.txt_group_switch.setText(beanSwitch.getSwitch_name());

        holder.txt_progress.setText(String.valueOf(beanSwitch.getDimmerValue()));

        //dimmerProgress.setProgress(beanSwitch.getDimmerValue());
        holder.edt_dimmerValue.setText(String.valueOf(beanSwitch.getDimmerValue()));

        holder.img_plusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = holder.edt_dimmerValue.getText().toString();

                int dimmerVal = 0;

                if (!beanSwitch.isLoading()) {
                    if (dimmerValue.isEmpty()) {
                        dimmerVal = 0;
                        holder.edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal >= 5)) {
                            dimmerVal++;
                            holder.edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
                    }

                    switchList.get(position).setDimmerValue(dimmerVal);

                    if (beanSwitch.getIsSwitchOn() == 1) {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                true, switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    } else {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                false, switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    }
                }
            }
        });

        holder.img_minusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = holder.edt_dimmerValue.getText().toString();

                int dimmerVal = 0;
                if (!beanSwitch.isLoading()) {
                    if (dimmerValue.isEmpty()) {
                        holder.edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal <= 0)) {
                            dimmerVal--;
                            holder.edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
                    }

                    switchList.get(position).setDimmerValue(dimmerVal);

                    if (beanSwitch.getIsSwitchOn() == 1) {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                true, switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    } else {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                false, switchList.get(position).getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    }
                }
            }
        });

        if (beanSwitch.getIsFavourite() == 0) {
            holder.img_fav.setImageResource(R.drawable.new_heart_off);
        } else holder.img_fav.setImageResource(R.drawable.new_heart_on);

        holder.img_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (beanSwitch.getIsFavourite() == 0) {
                    beanSwitch.setIsFavourite(1);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(), true);
                    holder.img_fav.setImageResource(R.drawable.new_heart_on);
                } else {
                    beanSwitch.setIsFavourite(0);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(), false);
                    holder.img_fav.setImageResource(R.drawable.new_heart_off);
                }
            }
        });


        holder.option_menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.showOptionMenu(position, holder.option_menu);
            }
        });
    }

    @Override
    public int getItemCount() {
        return switchList.size();
    }

    public void setViewLoading(int position, boolean loading, String lastCommand) {
        switchList.get(position).setLoading(loading);
        switchList.get(position).setLastCommand(lastCommand);
        switchList.get(position).setTime(new Date());
        notifyItemChanged(position);
    }

    public String setTouchUserLock(String slave_hex_id, String switch_button_num, boolean lock, boolean isTouchLock, int position) {
        String msg = "";

        String lockData = "";

        lockData += switch_button_num;
        if (lock) {
            lockData += "Y";
        } else {
            lockData += "N";
        }


        if (netcheck.isOnline()) {
            switchList.get(position).setTime(new Date());
            switchList.get(position).setLoading(true);
            //notifyDataSetChanged();

            notifyItemChanged(position);

            JSONObject object = new JSONObject();
            try {
                if (isTouchLock) {
                    object.put("cmd", Cmd.TL1);
                } else {
                    object.put("cmd", Cmd.UL1);
                }

                object.put("slave", slave_hex_id);
                object.put("data", lockData);
                object.put("token", switchList.get(position).getSlaveToken());

            } catch (JSONException e) {
                //Log.e("Exception",e.getMessage());
            }

            String mqttCommand = object.toString();
            switchList.get(position).setLastCommand(mqttCommand);

            if (SmartNode.slavesInLocal.contains(switchList.get(position).getSwitchInSlave())) {
                switchSelection.sendUDPCommand(mqttCommand);
            } else {
                //Log.e("Connected INTRNT","--"+SmartNode.isConnectedToInternet);

                new SendMQTTMsg(mqttCommand, switchList.get(position).getSlaveTopic()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }

        } else {
            C.Toast(activity, "No internet connection found,\nplease check your connection first");
        }

        return msg;
    }

    private int getSwitchIDForOnOff(int switchIconID, boolean on) {
        int switchIconDrawable;
        if (on) {
            switchIconDrawable = R.drawable.fluorescent_bulb_on;
        } else {
            switchIconDrawable = R.drawable.fluorescent_bulb_off;
        }

        for (int i = 0; i < switchIconsList.size(); i++) {
            if (switchIconsList.get(i).getIconid() == switchIconID) {
                switchIconDrawable = on ? switchIconsList.get(i).getSwOnId() : switchIconsList.get(i).getSwOffId();
                break;
            }
        }

        return switchIconDrawable;
    }

    public String setSwitchOnOff(String slave_hex_id, int switch_id, String switch_button_num, boolean onOff, int dval, String type, int position) {
        String msg = "";

        try {
            if (netcheck.isOnline()) {
                callCount++;
                switchList.get(position).setTime(new Date());
                switchList.get(position).setLoading(true);
                //notifyDataSetChanged();
                notifyItemChanged(position);

                JSONObject object = new JSONObject();
                if (switchList.get(position).getSlaveUserType().equalsIgnoreCase(Cmd.LIN))
                    object.put("cmd", Cmd.UPD);
                else
                    object.put("cmd", Cmd.UUP);

                object.put("slave", slave_hex_id);
                object.put("token", switchList.get(position).getSlaveToken());

                String mqttCommand = switch_button_num;

                if (onOff) {
                    mqttCommand += "A";
                } else {
                    mqttCommand += "0";
                }

                if (type.equalsIgnoreCase("s")) mqttCommand += "X";
                else mqttCommand += String.valueOf(dval);

                object.put("data", mqttCommand);

                String command = object.toString();

                switchList.get(position).setLastCommand(command);


                if (SmartNode.slavesInLocal.contains(slave_hex_id)) {
                    //Log.e("sent from", "UDP");
                    switchSelection.sendUDPCommand(command);
                } else {
                    if (SmartNode.isConnectedToInternet) {
                        new SendMQTTMsg(command, switchList.get(position).getSlaveTopic()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        C.Toast(activity, "Internet is not Available!");
                    }
                    //Log.e("sent from", "MQTT");

                }

            } else {
                C.Toast(activity, "No internet connection found,\nplease check your connection first");
            }
        } catch (Exception e) {
            //Log.e("Exception",e.getMessage());
        }

        return msg;
    }

    public void setCallBack(SwitchDimmerOnOffAdapter.DimmerChangeCallBack dimmerChangeCallBack) {
        this.callback = dimmerChangeCallBack;
    }

    public void setLockStatusChanged(String slaveID, String switchButtonNum, String sts, boolean isTouchLock) {
        int itemUpdate = -1;

        for (int i = 0; i < switchList.size(); i++) {
            if (switchList.get(i).getSwitchInSlave().equals(slaveID) && switchList.get(i).getSwitch_btn_num().equals(switchButtonNum)) {
                itemUpdate = i;
                if (isTouchLock) {
                    switchList.get(i).setTouchLock(sts);
                } else {
                    switchList.get(i).setUserLock(sts);
                }
                switchList.get(i).setLastCommand("");
                switchList.get(i).setLoading(false);
                break;
            }
        }

        if (itemUpdate != -1) {
            notifyItemChanged(itemUpdate);
        }

        setAllSwitchesOnline(slaveID);

    }

    public String setSwitchItem(String slave_hex_id, String button, String isOn, String dval) {
        String msg = "";

        int itemUpdate = -1;

        for (int i = 0; i < switchList.size(); i++) {
            Bean_Switch beanSwitch = switchList.get(i);

            if (beanSwitch.getSwitchInSlave().equals(slave_hex_id)) {
                if (beanSwitch.getSwitch_btn_num().equals(button)) {
                    itemUpdate = i;
                    switchList.get(i).setLastCommand("");
                    switchList.get(i).setLoading(false);
                    if (isOn.equalsIgnoreCase("A")) {
                        switchList.get(i).setIsSwitchOn(1);
                        //databaseHandler.setSwitchIsOnById(switchList.get(i).getSwitch_id(),true);
                    } else {
                        switchList.get(i).setIsSwitchOn(0);
                        //databaseHandler.setSwitchIsOnById(switchList.get(i).getSwitch_id(),false);
                    }

                    if (beanSwitch.getIsSwitch().equalsIgnoreCase("d")) {
                        ////Log.e("Dimmer sts :", ""+dval);
                        switchList.get(i).setDimmerValue(Integer.parseInt(dval));
                        //databaseHandler.setDimmerValue(switchList.get(i).getSwitch_id(),Integer.parseInt(dval));
                    }

                    msg = "success";
                    ////Log.e("Data :","Braek");
                    break;
                }
            }
        }

        if (itemUpdate != -1) notifyItemChanged(itemUpdate);

        setAllSwitchesOnline(slave_hex_id);

        return msg;
    }

    private void setAllSwitchesOnline(String slaveHexId) {
        for (int i = 0; i < switchList.size(); i++) {
            if (switchList.get(i).getSwitchInSlave().equals(slaveHexId)) {
                notifyItemChanged(i);
            }
        }
    }

    public void startToCheckResendCommands() {
        final int[] MESSAGE_RESEND_DELAY = {500};
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < switchList.size(); i++) {
                    if (switchList.get(i).isLoading() && switchList.get(i).getTime() != null) {
                        long diffInMs = new Date().getTime() - switchList.get(i).getTime().getTime();
                        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);

                        if (!SmartNode.slavesInLocal.contains(switchList.get(i).getSwitchInSlave()) && diffInSec > 7) {
                            if (SmartNode.slavesWorking.contains(switchList.get(i).getSwitchInSlave())) {
                                SmartNode.slavesWorking.remove(switchList.get(i).getSwitchInSlave());
                                setAllSwitchesOffline(switchList.get(i).getSwitchInSlave());
                                continue;
                            }
                        }

                        if (diffInSec > 0.5 && !switchList.get(i).getLastCommand().isEmpty()) {

                            String command = switchList.get(i).getLastCommand();

                            if (SmartNode.slavesInLocal.contains(switchList.get(i).getSwitchInSlave())) {
                                MESSAGE_RESEND_DELAY[0] = 500;
                                switchSelection.sendUDPCommand(command);
                            } /*else {
                                new SendMQTTMsg(command, switchList.get(i).getSlaveTopic()).execute();
                            }*/
                        }
                    }
                }
                mHandler.postDelayed(this, MESSAGE_RESEND_DELAY[0]);
            }
        };
        mHandler.postDelayed(mRunnable, MESSAGE_RESEND_DELAY[0]);
    }

    public void setAllSwitchesOffline(String switchInSlave) {
        for (int i = 0; i < switchList.size(); i++) {
            if (switchInSlave == null) {
                switchList.get(i).setLoading(false);
                notifyItemChanged(i);
                continue;
            }

            if (switchList.get(i).getSwitchInSlave().equals(switchInSlave)) {
                switchList.get(i).setLoading(false);
                notifyItemChanged(i);
            }
        }
    }

    public void stopToCheckResendCommand() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    public int getSwitchIdAtPosition(int position) {
        int switchId = 0;

        switchId = switchList.get(position).getSwitch_id();

        return switchId;
    }

    public String getSwitchName(int position) {
        String switchName = "";

        switchName = switchList.get(position).getSwitch_name();

        return switchName;
    }

    public boolean isSwitch(int position) {
        if (switchList.get(position).getIsSwitch().equalsIgnoreCase("s")) {
            return true;
        }
        return false;
    }

    public String getSwitchNumber(int position) {
        return switchList.get(position).getSwitch_btn_num();
    }

    public String getSlaveIDForSwitch(int position) {
        return switchList.get(position).getSwitchInSlave();
    }

    public int getGroupIdAtPosition(int position) {
        int groupId = 0;

        groupId = switchList.get(position).getSwitchInGroup();

        return groupId;
    }

    public void notifyIconChanged() {
        switchList.clear();
        if (fromActivity.equals(Globals.FAVOURITE)) {
            switchList = databaseHandler.getAllSwitchesInFavourite();
        } else {
            switchList.addAll(databaseHandler.getAllSwitchesByGroupId(groupid));
        }
        notifyDataSetChanged();
    }

    public void notifyIconChanged(String slaveId) {
        if (fromActivity.equals(Globals.FAVOURITE)) {
            List<Bean_Switch> switches = databaseHandler.getAllSwitchesFavourite(groupid, slaveId);
            for (int i = 0; i < switchList.size(); i++) {
                String position = i + "--";
                for (int j = 0; j < switches.size(); j++) {
                    if (switchList.get(i).getSwitchInSlave().equals(switches.get(j).getSwitchInSlave()) && switchList.get(i).getSwitch_btn_num().equals(switches.get(j).getSwitch_btn_num())) {
                        position += "--" + j;
                        switchList.remove(i);
                        Bean_Switch beanSwitch = switches.get(j);
                        beanSwitch.setLoading(false);
                        switchList.add(i, beanSwitch);

                        switches.remove(j);
                        notifyItemChanged(i);
                        break;
                    }


                }
                //Log.e("Position updte","--"+position);
            }
        } else {
            List<Bean_Switch> switches = databaseHandler.getAllSwitches(groupid, slaveId);
            for (int i = 0; i < switchList.size(); i++) {
                String position = i + "--";
                for (int j = 0; j < switches.size(); j++) {
                    if (switchList.get(i).getSwitchInSlave().equals(switches.get(j).getSwitchInSlave()) && switchList.get(i).getSwitch_btn_num().equals(switches.get(j).getSwitch_btn_num())) {
                        position += "--" + j;
                        switchList.remove(i);
                        Bean_Switch beanSwitch = switches.get(j);
                        beanSwitch.setLoading(false);
                        switchList.add(i, beanSwitch);

                        switches.remove(j);
                        notifyItemChanged(i);
                        break;
                    }


                }
                //Log.e("Position updte","--"+position);
            }
        }

    }

    public Object getItem(int position) {
        return switchList.get(position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView txt_group_switch, txt_group, txt_progress;
        ImageView img_fav, option_menu, img_switch, img_tlock, img_ulock, img_schedule, cloud;
        LinearLayout layout_dimmerBar;
        ImageView img_minusDimmer, img_plusDimmer;
        EditText edt_dimmerValue;
        ProgressBar progressLoading;
        FrameLayout frame_bg;

        public MyViewHolder(View itemView) {
            super(itemView);

            txt_group_switch = (TextView) itemView.findViewById(R.id.group_switch_name);
            txt_group = (TextView) itemView.findViewById(R.id.group_name);
            txt_progress = (TextView) itemView.findViewById(R.id.txt_progress);

            img_fav = (ImageView) itemView.findViewById(R.id.fav_off);
            option_menu = (ImageView) itemView.findViewById(R.id.power_off);
            img_switch = (ImageView) itemView.findViewById(R.id.switch_off);
            img_tlock = (ImageView) itemView.findViewById(R.id.img_tlock);
            img_ulock = (ImageView) itemView.findViewById(R.id.img_ulock);
            img_schedule = (ImageView) itemView.findViewById(R.id.img_schedule);
            cloud = (ImageView) itemView.findViewById(R.id.cloud);

            layout_dimmerBar = (LinearLayout) itemView.findViewById(R.id.layout_dimmerBar);
            img_minusDimmer = (ImageView) itemView.findViewById(R.id.img_minusDimmer);
            img_plusDimmer = (ImageView) itemView.findViewById(R.id.img_plusDimmer);
            edt_dimmerValue = (EditText) itemView.findViewById(R.id.edt_dimmerValue);

            progressLoading = (ProgressBar) itemView.findViewById(R.id.progressLoading);
            frame_bg = (FrameLayout) itemView.findViewById(R.id.frame_bg);
        }
    }

    private class SendMQTTMsg extends AsyncTask<Void, Void, Void> {

        String command = "";
        String topic = "";

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

                //Log.e("Command Fired UPD :", command);

                MqttMessage mqttMessage = new MqttMessage(command.getBytes());
                mqttMessage.setQos(0);
                mqttMessage.setRetained(false);
                mqttClient.publish(topic + AppConstant.MQTT_PUBLISH_TOPIC, mqttMessage);
                //Log.e("topic msg", topic + AppConstant.MQTT_PUBLISH_TOPIC + " " + mqttMessage);
                mqttClient.disconnect();
            } catch (MqttException e) {
                //Log.e("Exception : ", e.getMessage());
                e.printStackTrace();
            }

            return null;
        }
    }
}
