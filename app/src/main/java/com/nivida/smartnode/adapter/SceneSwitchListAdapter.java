package com.nivida.smartnode.adapter;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Scenes;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.List;

/**
 * Created by Chintak Patel on 11-Aug-16.
 */
public class SceneSwitchListAdapter extends ArrayAdapter<Bean_Switch> {

    Context context;
    List<Bean_Switch> switches;
    public DatabaseHandler databaseHandler;

    public SceneSwitchListAdapter(Context context, List<Bean_Switch> switches){
        super(context,R.layout.custom_scene_switch_list,switches);
        this.context=context;
        this.switches=switches;
        databaseHandler=new DatabaseHandler(context);
    }

    static class ViewHolder{
        protected TextView txt_switch_name;
        protected EditText edt_dimmerValue;
        protected Button switch_onOff;
    }

    @Override
    public int getCount() {
        return switches.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder=new ViewHolder();

        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView=inflater.inflate(R.layout.custom_scene_switch_list,parent,false);


        viewHolder.txt_switch_name=(TextView) convertView.findViewById(R.id.txt_switch_name);
       viewHolder.edt_dimmerValue=(EditText) convertView.findViewById(R.id.edt_dimmerValue);
        viewHolder.switch_onOff=(Button) convertView.findViewById(R.id.switch_onOff);

        viewHolder.txt_switch_name.setText(switches.get(position).getSwitch_name());

        if(switches.get(position).getIsSwitchOn()==0){
            viewHolder.switch_onOff.setText("OFF");
            viewHolder.switch_onOff.setBackgroundColor(context.getResources().getColor(R.color.divider_color));
        } else {
            viewHolder.switch_onOff.setText("ON");
            viewHolder.switch_onOff.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
        }

        viewHolder.edt_dimmerValue.setText(String.valueOf(switches.get(position).getDimmerValue()));

        if(switches.get(position).getIsSwitch().equalsIgnoreCase("s")){
            viewHolder.edt_dimmerValue.setVisibility(View.INVISIBLE);
        }

        viewHolder.edt_dimmerValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(viewHolder.edt_dimmerValue.getText().toString().equalsIgnoreCase("")){
                    switches.get(position).setDimmerValue(0);
                } else if(Integer.parseInt(viewHolder.edt_dimmerValue.getText().toString())>5){
                    viewHolder.edt_dimmerValue.setText("5");
                    switches.get(position).setDimmerValue(5);
                } else if(Integer.parseInt(viewHolder.edt_dimmerValue.getText().toString())<0){
                    viewHolder.edt_dimmerValue.setText("0");
                    switches.get(position).setDimmerValue(0);
                } else {
                    switches.get(position).setDimmerValue(Integer.parseInt(viewHolder.edt_dimmerValue.getText().toString()));
                }
            }
        });


        viewHolder.switch_onOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(switches.get(position).getIsSwitchOn()==0){
                    switches.get(position).setIsSwitchOn(1);
                    viewHolder.switch_onOff.setText("ON");
                    viewHolder.switch_onOff.setBackgroundColor(context.getResources().getColor(R.color.colorPrimary));
                }
                else {
                    switches.get(position).setIsSwitchOn(0);
                    viewHolder.switch_onOff.setText("OFF");
                    viewHolder.switch_onOff.setBackgroundColor(context.getResources().getColor(R.color.divider_color));
                }
            }
        });

        convertView.setTag(viewHolder);

        return convertView;
    }

    public List<Bean_Switch> getList() {
        return this.switches;
    }
}
