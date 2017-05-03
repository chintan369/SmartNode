package com.nivida.smartnode.adapter;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.List;

/**
 * Created by Chintak Patel on 20-Jul-16.
 */
public class SwitchOnOffAdapter extends RecyclerView.Adapter<SwitchOnOffAdapter.ViewHolder> {

    List<Bean_Switch> switchList;
    DatabaseHandler databaseHandler;

    public SwitchOnOffAdapter(Context context, List<Bean_Switch> switchList){
        databaseHandler=new DatabaseHandler(context);
        this.switchList=switchList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
       View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_switch_onoff, parent, false);


        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {

        final Bean_Switch beanSwitch=switchList.get(position);

        holder.switch_name.setText(beanSwitch.getSwitch_name());
        holder.group_name.setText(databaseHandler.getGroupnameById(beanSwitch.getSwitchInGroup()));

        if(beanSwitch.getSwitch_name().contains("Bulb")){
            holder.img_switch_off.setImageResource(R.drawable.bulb);
        }
        else if(beanSwitch.getSwitch_name().contains("Fan")){
            holder.img_switch_off.setImageResource(R.drawable.fan);
        }

        if(beanSwitch.getIsFavourite()==0){
            holder.img_fav_off.setImageResource(R.drawable.hearts_grey);
        }
        else holder.img_fav_off.setImageResource(R.drawable.hearts_green);

        if(beanSwitch.getIsSwitchOn()==0)
            holder.img_power_off.setImageResource(R.drawable.shutdown_thin_grey);
        else holder.img_power_off.setImageResource(R.drawable.shutdown_thin_green);

        holder.img_fav_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(beanSwitch.getIsFavourite()==0){
                    beanSwitch.setIsFavourite(1);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(),true);
                    holder.img_fav_off.setImageResource(R.drawable.hearts_green);
                }
                else {
                    beanSwitch.setIsFavourite(0);
                    databaseHandler.setFavouriteSwitchById(beanSwitch.getSwitch_id(),false);
                    holder.img_fav_off.setImageResource(R.drawable.hearts_grey);
                }
            }
        });

        holder.img_power_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(beanSwitch.getIsSwitchOn()==0){
                    beanSwitch.setIsSwitchOn(1);
                    databaseHandler.setSwitchIsOnById(beanSwitch.getSwitch_id(),true);
                    holder.img_power_off.setImageResource(R.drawable.shutdown_thin_green);
                }
                else {
                    beanSwitch.setIsSwitchOn(0);
                    databaseHandler.setSwitchIsOnById(beanSwitch.getSwitch_id(),false);
                    holder.img_power_off.setImageResource(R.drawable.shutdown_thin_grey);
                }
            }
        });


    }

    @Override
    public int getItemCount() {
        return switchList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView switch_name,group_name;
        public ImageView img_switch_on,img_switch_off,img_power_off,img_fav_off;

        public ViewHolder(View itemView) {
            super(itemView);
            switch_name=(TextView) itemView.findViewById(R.id.group_switch_name);
            group_name=(TextView) itemView.findViewById(R.id.group_name);

            img_switch_on=(ImageView) itemView.findViewById(R.id.switch_on);
            img_switch_off=(ImageView) itemView.findViewById(R.id.switch_off);
            img_power_off=(ImageView) itemView.findViewById(R.id.power_off);
            img_fav_off=(ImageView) itemView.findViewById(R.id.fav_off);
        }
    }
}
