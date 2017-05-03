package com.nivida.smartnode.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Dimmer;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.CircularSeekBar;

import java.util.List;

/**
 * Created by Chintak Patel on 21-Jul-16.
 */
public class DimmerOnOffAdapter extends ArrayAdapter<Bean_Dimmer> {

    Context context;
    List<Bean_Dimmer> dimmerList;
    DatabaseHandler databaseHandler;

    public DimmerOnOffAdapter(Context context, List<Bean_Dimmer> dimmerList) {
        super(context, R.layout.custom_dimmer_onoff,dimmerList);
        this.context=context;
        this.dimmerList=dimmerList;
        databaseHandler=new DatabaseHandler(context);
    }

    static class ViewHolder{
        protected TextView txt_group_dimmer_name,txt_group_name,txt_progress;
        protected CircularSeekBar dimmerProgress;
        protected ImageView img_fav;
    }

    @Override
    public int getCount() {
        return dimmerList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=null;
        if (convertView == null) {
            LayoutInflater inflater =(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_dimmer_onoff, null);
            viewHolder = new ViewHolder();
            viewHolder.txt_group_dimmer_name = (TextView) convertView.findViewById(R.id.group_dimmer_name);
            viewHolder.txt_group_name = (TextView) convertView.findViewById(R.id.group_name);
            viewHolder.txt_progress = (TextView) convertView.findViewById(R.id.txt_progress);
            viewHolder.dimmerProgress = (CircularSeekBar) convertView.findViewById(R.id.dimmerProgress);
            viewHolder.img_fav=(ImageView) convertView.findViewById(R.id.fav_off);

            viewHolder.dimmerProgress.setProgress(dimmerList.get(position).getDimmerValue());
            viewHolder.txt_progress.setText(String.valueOf(dimmerList.get(position).getDimmerValue()));

            viewHolder.txt_group_dimmer_name.setText(dimmerList.get(position).getDimmer_name());
            viewHolder.txt_group_name.setText(databaseHandler.getGroupnameById(dimmerList.get(position).getDimmerInGroup()));

            if(dimmerList.get(position).getIsFavourite()==0){
                viewHolder.img_fav.setImageResource(R.drawable.hearts_grey);
            }
            else viewHolder.img_fav.setImageResource(R.drawable.hearts_green);

            final ViewHolder finalViewHolder = viewHolder;
            viewHolder.img_fav.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int isFavourite=dimmerList.get(position).getIsFavourite();
                    int dimmerId=dimmerList.get(position).getDimmer_id();

                    if(isFavourite==0){
                       dimmerList.get(position).setIsFavourite(1);
                        finalViewHolder.img_fav.setImageResource(R.drawable.hearts_green);
                        databaseHandler.setFavouriteDimmerById(dimmerId,true);
                    }
                    else {
                        dimmerList.get(position).setIsFavourite(0);
                        finalViewHolder.img_fav.setImageResource(R.drawable.hearts_grey);
                        databaseHandler.setFavouriteDimmerById(dimmerId,false);
                    }
                }
            });

            final ViewHolder finalViewHolder1 = viewHolder;
            viewHolder.dimmerProgress.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
                @Override
                public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
                    if(circularSeekBar.getProgress()==6){
                        circularSeekBar.setProgress(0);
                    }
                    dimmerList.get(position).setDimmerValue(circularSeekBar.getProgress());
                    databaseHandler.setDimmerValue(dimmerList.get(position).getDimmer_id(),circularSeekBar.getProgress());
                    finalViewHolder1.txt_progress.setText(String.valueOf(dimmerList.get(position).getDimmerValue()));
                }

                @Override
                public void onStopTrackingTouch(CircularSeekBar seekBar) {

                }

                @Override
                public void onStartTrackingTouch(CircularSeekBar seekBar) {

                }
            });


            convertView.setTag(viewHolder);
            //convertView.setTag(R.id.txt_switch, viewHolder.txt_switch);
            //convertView.setTag(R.id.check_switch, viewHolder.chk_switch);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        //viewHolder.chk_switch.setTag(position); // This line is important.

        //viewHolder.txt_switch.setText(switchList.get(position).getSwitch_name());
        //viewHolder.chk_switch.setChecked(switchList.get(position).isChecked());

        return convertView;
    }
}
