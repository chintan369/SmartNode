package com.nivida.smartnode.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nivida.smartnode.Globals;
import com.nivida.smartnode.R;
import com.nivida.smartnode.SetScheduleActivity;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.a.Cmd;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.beans.Bean_SwitchIcons;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.model.IPDb;
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
 * Created by Chintak Patel on 23-Jul-16.
 */
public class SwitchDimmerOnOffAdapter extends BaseAdapter {

    //Define MQTT variables here
    public static final String SERVICE_CLASSNAME = "com.nivida.smartnode.services.AddDeviceService";
    Context context;
    String fromActivity;
    List<Bean_Switch> switchList = new ArrayList<>();
    DatabaseHandler databaseHandler;
    int groupid = 0;
    AppPreference preference;
    //List<Bean_SwitchIcons> switchIconsList=new ArrayList<>();
    MqttClient mqttClient;
    String clientId = "";
    String subscribedMessage = "";
    NetworkUtility netcheck;
    OnSwitchSelection switchSelection;
    List<Bean_SwitchIcons> switchIconsList = new ArrayList<>();
    List<String> ipList = new ArrayList<>();
    boolean allowToNotifyChange = true;
    Handler mHandler;
    Runnable mRunnable;
    private DimmerChangeCallBack callback;
    private int callCount = 0;

    public SwitchDimmerOnOffAdapter(Context context, List<Bean_Switch> switchList, String fromActivity, OnSwitchSelection switchSelection) {
        this.switchSelection = switchSelection;
        this.context = context;
        this.switchList = switchList;
        this.fromActivity = fromActivity;
        this.netcheck = new NetworkUtility(context);
        clientId = MqttClient.generateClientId();
        databaseHandler = new DatabaseHandler(context);
        switchIconsList = databaseHandler.getAllSwitchIconData();
        ipList = new IPDb(context).ipList();
        preference = new AppPreference(context);
        if (switchList.size() > 0) {
            this.groupid = switchList.get(0).getSwitchInGroup();
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

    //@Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.switch_dimmer_onoff, null);
        return new MyViewHolder(itemView);
    }

    //@Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {

        final Bean_Switch beanSwitch = switchList.get(position);

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

        /*if(databaseHandler.hasScheduleSet(beanSwitch.getSwitchInSlave(),String.valueOf(beanSwitch.getSwitch_id())))
            img_schedule.setImageResource(R.drawable.has_schedule);
        else
            img_schedule.setImageResource(R.drawable.no_schedule);*/

        if (switchList.get(position).getHasSchedule() == 0) {
            holder.img_schedule.setImageResource(R.drawable.no_schedule);
        } else {
            holder.img_schedule.setImageResource(R.drawable.has_schedule);
        }

        holder.img_tlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoading(position)) {
                    if (switchList.get(position).getTouchLock().equalsIgnoreCase("N")) {
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
                if (switchList.get(position).getSlaveUserType().equalsIgnoreCase(Cmd.ULN)) {
                    C.Toast(context, "Guest don't have privilleges to perform this action");
                } else {
                    if (!isLoading(position)) {
                        if (switchList.get(position).getUserLock().equalsIgnoreCase("N")) {
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
                Intent intent = new Intent(context, SetScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("group_id", groupid);
                intent.putExtra("switchID", switchList.get(position).getSwitch_id());
                intent.putExtra("switchName", switchList.get(position).getSwitch_name());
                context.startActivity(intent);
            }
        });

        //final DiscreteSeekBar dimmerProgress = (DiscreteSeekBar) view.findViewById(R.id.dimmerProgress);

        //int groupid = switchList.get(position).getSwitchInGroup();

        //if(beanSwitch.getIsSwitch().equalsIgnoreCase("s")){

        try {
            if (!isLoading(position)) {
                if (beanSwitch.getIsSwitchOn() == 1) {
                    Glide.with(context)
                            .load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), true))
                            .into(holder.img_switch);
                } else {
                    Glide.with(context)
                            .load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), false))
                            .into(holder.img_switch);
                }
            } else {
                Glide.with(context)
                        .load(R.drawable.loading)
                        .asGif()
                        .into(holder.img_switch);
            }

        } catch (Exception e) {
            //C.connectionError(context);
        }
        // }
        /*else {
            if(beanSwitch.getIsSwitchOn()==1)
                img_switch.setImageResource(R.drawable.fan_on);
            else img_switch.setImageResource(R.drawable.fan_off);
        }*/

        holder.img_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("Switch Click", "clicked" + position + " - loading -" + isLoading(position));
                if (!isLoading(position)) {
                    if (beanSwitch.getIsSwitchOn() == 1) {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                false, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    } else {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                true, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    }
                }
            }
        });


        if (switchList.get(position).getIsSwitch().equalsIgnoreCase("s")) {
            if (position % 2 == 0 && position < getItemCount() - 1 && switchList.get(position + 1).getIsSwitch().equalsIgnoreCase("d")) {
                holder.layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else if (position % 2 > 0 && switchList.get(position - 1).getIsSwitch().equalsIgnoreCase("d")) {
                holder.layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else {
                holder.layout_dimmerBar.setVisibility(View.GONE);
            }

            //dimmerProgress.setVisibility(View.GONE);
            //dimmerProgress.setEnabled(false);
            holder.txt_progress.setVisibility(View.GONE);
        } else if (switchList.get(position).getIsSwitch().equalsIgnoreCase("d")) {
            holder.layout_dimmerBar.setVisibility(View.VISIBLE);
            holder.img_switch.setVisibility(View.VISIBLE);
            //dimmerProgress.setVisibility(View.GONE);
            holder.txt_progress.setVisibility(View.GONE);
        }

        if (fromActivity.equalsIgnoreCase(Globals.FAVOURITE)) {
            holder.txt_group.setVisibility(View.VISIBLE);
            holder.txt_group.setText(switchList.get(position).getSwitchGroupName());
        } else if (fromActivity.equalsIgnoreCase(Globals.GROUP)) {
            holder.txt_group.setVisibility(View.GONE);
        }

        holder.txt_group_switch.setText(switchList.get(position).getSwitch_name());

        holder.txt_progress.setText(String.valueOf(beanSwitch.getDimmerValue()));

        //dimmerProgress.setProgress(beanSwitch.getDimmerValue());
        holder.edt_dimmerValue.setText(String.valueOf(beanSwitch.getDimmerValue()));

        holder.img_plusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = holder.edt_dimmerValue.getText().toString();

                int dimmerVal = 0;

                if (!isLoading(position)) {
                    if (dimmerValue.isEmpty()) {
                        holder.edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal >= 5)) {
                            dimmerVal++;
                            holder.edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
                    }
                }
            }
        });

        holder.img_minusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = holder.edt_dimmerValue.getText().toString();

                int dimmerVal = 0;
                if (!isLoading(position)) {
                    if (dimmerValue.isEmpty()) {
                        holder.edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal <= 0)) {
                            dimmerVal--;
                            holder.edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
                    }
                }
            }
        });

        holder.edt_dimmerValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String dimmerValue = holder.edt_dimmerValue.getText().toString();

                int dimmerVal = 0;

                if (dimmerValue.isEmpty()) {
                    dimmerVal = 0;
                } else {
                    dimmerVal = Integer.parseInt(dimmerValue);
                }

                switchList.get(position).setDimmerValue(dimmerVal);
                //databaseHandler.setDimmerValue(beanSwitch.getSwitch_id(), dimmerVal);
                if (!isLoading(position)) {
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

    private boolean isLoading(int position) {
        return switchList.get(position).isLoading();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    //@Override
    public int getItemCount() {
        return switchList.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        View view = null;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.switch_dimmer_onoff, null);

        final Bean_Switch beanSwitch = switchList.get(position);

        final TextView txt_group_switch = (TextView) view.findViewById(R.id.group_switch_name);
        final TextView txt_group = (TextView) view.findViewById(R.id.group_name);
        final TextView txt_progress = (TextView) view.findViewById(R.id.txt_progress);

        final ImageView img_fav = (ImageView) view.findViewById(R.id.fav_off);
        final ImageView option_menu = (ImageView) view.findViewById(R.id.power_off);
        final ImageView img_switch = (ImageView) view.findViewById(R.id.switch_off);
        final ImageView img_tlock = (ImageView) view.findViewById(R.id.img_tlock);
        final ImageView img_ulock = (ImageView) view.findViewById(R.id.img_ulock);
        final ImageView img_schedule = (ImageView) view.findViewById(R.id.img_schedule);

        LinearLayout layout_dimmerBar = (LinearLayout) view.findViewById(R.id.layout_dimmerBar);
        final ImageView img_minusDimmer = (ImageView) view.findViewById(R.id.img_minusDimmer);
        final ImageView img_plusDimmer = (ImageView) view.findViewById(R.id.img_plusDimmer);
        final EditText edt_dimmerValue = (EditText) view.findViewById(R.id.edt_dimmerValue);

        if (beanSwitch.getTouchLock().equalsIgnoreCase("Y")) {
            img_tlock.setImageResource(R.drawable.tlock);
        } else {
            img_tlock.setImageResource(R.drawable.tunlock);
        }

        if (beanSwitch.getUserLock().equalsIgnoreCase("Y")) {
            img_ulock.setImageResource(R.drawable.lock);
        } else {
            img_ulock.setImageResource(R.drawable.unlock);
        }

        if (switchList.get(position).getHasSchedule() == 0) {
            img_schedule.setImageResource(R.drawable.no_schedule);
        } else {
            img_schedule.setImageResource(R.drawable.has_schedule);
        }

        img_tlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!switchList.get(position).isLoading()) {
                    if (switchList.get(position).getTouchLock().equalsIgnoreCase("N")) {
                        setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), true, true, position);
                    } else {
                        setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), false, true, position);
                    }
                }

            }
        });

        img_ulock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchList.get(position).getSlaveUserType().equalsIgnoreCase(Cmd.ULN)) {
                    C.Toast(context, "Guest don't have privilleges to perform this action");
                } else {
                    if (!switchList.get(position).isLoading()) {
                        if (switchList.get(position).getUserLock().equalsIgnoreCase("N")) {
                            setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), true, false, position);
                        } else {
                            setTouchUserLock(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_btn_num(), false, false, position);
                        }
                    }
                }
            }
        });

        img_schedule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SetScheduleActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("group_id", groupid);
                intent.putExtra("switchID", switchList.get(position).getSwitch_id());
                intent.putExtra("switchName", switchList.get(position).getSwitch_name());
                context.startActivity(intent);
            }
        });

        //final DiscreteSeekBar dimmerProgress = (DiscreteSeekBar) view.findViewById(R.id.dimmerProgress);

        //int groupid = switchList.get(position).getSwitchInGroup();

        //if(beanSwitch.getIsSwitch().equalsIgnoreCase("s")){

        try {
            if (!switchList.get(position).isLoading()) {
                if (beanSwitch.getIsSwitchOn() == 1) {
                    Glide.with(context)
                            .load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), true))
                            .into(img_switch);
                } else {
                    Glide.with(context)
                            .load(getSwitchIDForOnOff(beanSwitch.getSwitch_icon(), false))
                            .into(img_switch);
                }
            } else {
                Glide.with(context)
                        .load(R.drawable.loading)
                        .asGif()
                        .into(img_switch);
            }

        } catch (Exception e) {
            //C.connectionError(context);
        }
        // }
       /* else {
            if(beanSwitch.getIsSwitchOn()==1)
                img_switch.setImageResource(R.drawable.fan_on);
            else img_switch.setImageResource(R.drawable.fan_off);
        }*/

        img_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!switchList.get(position).isLoading()) {
                    if (beanSwitch.getIsSwitchOn() == 1) {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                false, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    } else {
                        setSwitchOnOff(beanSwitch.getSwitchInSlave(), beanSwitch.getSwitch_id(), beanSwitch.getSwitch_btn_num(),
                                true, beanSwitch.getDimmerValue(), beanSwitch.getIsSwitch(), position);
                    }
                }
            }
        });


        if (switchList.get(position).getIsSwitch().equalsIgnoreCase("s")) {
            if (position % 2 == 0 && position < getCount() - 1 && switchList.get(position + 1).getIsSwitch().equalsIgnoreCase("d")) {
                layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else if (position % 2 > 0 && switchList.get(position - 1).getIsSwitch().equalsIgnoreCase("d")) {
                layout_dimmerBar.setVisibility(View.INVISIBLE);
            } else {
                layout_dimmerBar.setVisibility(View.GONE);
            }

            //dimmerProgress.setVisibility(View.GONE);
            //dimmerProgress.setEnabled(false);
            txt_progress.setVisibility(View.GONE);
        } else if (switchList.get(position).getIsSwitch().equalsIgnoreCase("d")) {
            layout_dimmerBar.setVisibility(View.VISIBLE);
            img_switch.setVisibility(View.VISIBLE);
            //dimmerProgress.setVisibility(View.GONE);
            txt_progress.setVisibility(View.GONE);
        }

        if (fromActivity.equalsIgnoreCase(Globals.FAVOURITE)) {
            txt_group.setVisibility(View.VISIBLE);
            txt_group.setText(switchList.get(position).getSwitchGroupName());
        } else if (fromActivity.equalsIgnoreCase(Globals.GROUP)) {
            txt_group.setVisibility(View.GONE);
        }

        txt_group_switch.setText(switchList.get(position).getSwitch_name());

        txt_progress.setText(String.valueOf(beanSwitch.getDimmerValue()));

        //dimmerProgress.setProgress(beanSwitch.getDimmerValue());
        edt_dimmerValue.setText(String.valueOf(beanSwitch.getDimmerValue()));

        img_plusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = edt_dimmerValue.getText().toString();

                int dimmerVal = 0;

                if (!switchList.get(position).isLoading()) {
                    if (dimmerValue.isEmpty()) {
                        edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal >= 5)) {
                            dimmerVal++;
                            edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
                    }
                }
            }
        });

        img_minusDimmer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String dimmerValue = edt_dimmerValue.getText().toString();

                int dimmerVal = 0;
                if (!switchList.get(position).isLoading()) {
                    if (dimmerValue.isEmpty()) {
                        edt_dimmerValue.setText(String.valueOf("0"));
                    } else {
                        dimmerVal = Integer.parseInt(dimmerValue);
                        if (!(dimmerVal <= 0)) {
                            dimmerVal--;
                            edt_dimmerValue.setText(String.valueOf(dimmerVal));
                        }
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
                String dimmerValue = edt_dimmerValue.getText().toString();

                int dimmerVal = 0;

                if (dimmerValue.isEmpty()) {
                    dimmerVal = 0;
                } else {
                    dimmerVal = Integer.parseInt(dimmerValue);
                }

                switchList.get(position).setDimmerValue(dimmerVal);
                //databaseHandler.setDimmerValue(beanSwitch.getSwitch_id(), dimmerVal);
                if (!switchList.get(position).isLoading()) {
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
            img_fav.setImageResource(R.drawable.new_heart_off);
        } else img_fav.setImageResource(R.drawable.new_heart_on);

        img_fav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (beanSwitch.getIsFavourite() == 0) {
                    beanSwitch.setIsFavourite(1);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(), true);
                    img_fav.setImageResource(R.drawable.new_heart_on);
                } else {
                    beanSwitch.setIsFavourite(0);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(), false);
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

        return view;
    }

    public void setCallBack(DimmerChangeCallBack dimmerChangeCallBack) {
        this.callback = dimmerChangeCallBack;
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

    @Override
    public void notifyDataSetChanged() {
        /*if(allowToNotifyChange){
            allowToNotifyChange=false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    allowToNotifyChange=true;
                    notifyDataSetChanged();
                }
            },1000);*/
        super.notifyDataSetChanged();
        //}
    }

    public String setSwitchItem(String slave_hex_id, String button, String isOn, String dval) {
        String msg = "";

        for (int i = 0; i < switchList.size(); i++) {
            Bean_Switch beanSwitch = switchList.get(i);

            if (beanSwitch.getSwitchInSlave().equals(slave_hex_id)) {
                if (beanSwitch.getSwitch_btn_num().equals(button)) {
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


                    //notifyAll();

                    Log.e("update", button + "--" + switchList.get(i).isLoading());

                    notifyDataSetChanged();

                    msg = "success";
                    ////Log.e("Data :","Braek");
                    break;
                }
            }
        }

        return msg;
    }

    public String setSwitchOnOff(String slave_hex_id, int switch_id, String switch_button_num, boolean onOff, int dval, String type, int position) {
        String msg = "";

        try {
            if (netcheck.isOnline()) {
                callCount++;
                switchList.get(position).setTime(new Date());
                switchList.get(position).setLoading(true);
                notifyDataSetChanged();

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

                //Log.e("Online Sts", "" + preference.isOnline());
                //Log.e("Command", command);

                if (callCount % 50 == 0) {
                    ipList = new IPDb(context).ipList();
                }


                if (ipList.contains(switchList.get(position).getSlaveIP())) {
                    switchSelection.sendUDPCommand(command);
                } else {
                    new SendMQTTMsg(command, switchList.get(position).getSlaveTopic()).execute();
                }

            } else {
                C.Toast(context, "No internet connection found,\nplease check your connection first");
            }
        } catch (Exception e) {
            //Log.e("Exception",e.getMessage());
        }

        return msg;
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
            notifyDataSetChanged();

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
            if (callCount % 50 == 0) {
                ipList = new IPDb(context).ipList();
            }

            if (ipList.contains(switchList.get(position).getSlaveIP())) {
                switchSelection.sendUDPCommand(mqttCommand);
            } else {
                new SendMQTTMsg(mqttCommand, switchList.get(position).getSlaveTopic()).execute();
            }

        } else {
            C.Toast(context, "No internet connection found,\nplease check your connection first");
        }

        return msg;
    }

    public void notifyIconChanged() {
        switchList.clear();
        if (fromActivity.equals(Globals.FAVOURITE)) {
            switchList = databaseHandler.getAllSwitchesInFavourite();
        } else {
            switchList = databaseHandler.getAllSwitchesByGroupId(groupid);
        }
        notifyDataSetChanged();
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

    public void setLockStatusChanged(String slaveID, String switchButtonNum, String sts, boolean isTouchLock) {

        for (int i = 0; i < switchList.size(); i++) {
            if (switchList.get(i).getSwitchInSlave().equals(slaveID) && switchList.get(i).getSwitch_btn_num().equals(switchButtonNum)) {
                if (isTouchLock) {
                    switchList.get(i).setTouchLock(sts);
                } else {
                    switchList.get(i).setUserLock(sts);
                }
                switchList.get(i).setLastCommand("");
                switchList.get(i).setLoading(false);

                notifyDataSetChanged();
                break;
            }
        }

    }

    public void startToCheckResendCommands() {
        final int MESSAGE_RESEND_DELAY = 3500;
        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < switchList.size(); i++) {
                    if (switchList.get(i).isLoading()) {
                        long diffInMs = new Date().getTime() - switchList.get(i).getTime().getTime();
                        long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);

                        if (diffInSec > 3 && !switchList.get(i).getLastCommand().isEmpty()) {
                            if (callCount % 50 == 0) {
                                ipList = new IPDb(context).ipList();
                            }

                            String command = switchList.get(i).getLastCommand();

                            if (ipList.contains(switchList.get(i).getSlaveIP())) {
                                switchSelection.sendUDPCommand(command);
                            } else {
                                new SendMQTTMsg(command, switchList.get(i).getSlaveTopic()).execute();
                            }
                        }
                    }
                }
                mHandler.postDelayed(this, MESSAGE_RESEND_DELAY);
            }
        };
        mHandler.postDelayed(mRunnable, MESSAGE_RESEND_DELAY);
    }

    public void stopToCheckResendCommand() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    public interface DimmerChangeCallBack {
        public void stopScrolling();

        public void startScrolling();

        public void showOptionMenu(int position, View view);
    }

    public interface OnSwitchSelection {
        void sendUDPCommand(String command);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView txt_group_switch, txt_group, txt_progress;
        public ImageView img_fav, option_menu, img_switch, img_tlock, img_ulock, img_schedule, img_minusDimmer, img_plusDimmer;
        public LinearLayout layout_dimmerBar;
        public EditText edt_dimmerValue;
        public boolean isLoading = false;

        public MyViewHolder(View view) {
            super(view);
            txt_group_switch = (TextView) view.findViewById(R.id.group_switch_name);
            txt_group = (TextView) view.findViewById(R.id.group_name);
            txt_progress = (TextView) view.findViewById(R.id.txt_progress);

            img_fav = (ImageView) view.findViewById(R.id.fav_off);
            option_menu = (ImageView) view.findViewById(R.id.power_off);
            img_switch = (ImageView) view.findViewById(R.id.switch_off);
            img_tlock = (ImageView) view.findViewById(R.id.img_tlock);
            img_ulock = (ImageView) view.findViewById(R.id.img_ulock);
            img_schedule = (ImageView) view.findViewById(R.id.img_schedule);

            layout_dimmerBar = (LinearLayout) view.findViewById(R.id.layout_dimmerBar);
            img_minusDimmer = (ImageView) view.findViewById(R.id.img_minusDimmer);
            img_plusDimmer = (ImageView) view.findViewById(R.id.img_plusDimmer);
            edt_dimmerValue = (EditText) view.findViewById(R.id.edt_dimmerValue);
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
