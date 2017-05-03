package com.nivida.smartnode.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chintak Patel on 20-Aug-16.
 */
public class SwitchItemScheduleAdapter extends BaseAdapter {

    Context context;
    List<Bean_Switch> switchList=new ArrayList<>();

    public SwitchItemScheduleAdapter(Context context,List<Bean_Switch> switchList) {
        this.context=context;
        this.switchList=switchList;
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
    public View getView(int position, View convertView, ViewGroup parent) {

        View view=null;

        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view=inflater.inflate(R.layout.custom_schedule_switchitem,parent,false);

        TextView txt_switch_item=(TextView) view.findViewById(R.id.txt_switch_name);
        txt_switch_item.setText(switchList.get(position).getSwitch_name());

        return view;
    }

    public int getSwitchIDAtPosition(int position){
        return switchList.get(position).getSwitch_id();
    }

    public String getSwitchNameAtPosition(int position) {
        return switchList.get(position).getSwitch_name();
    }
}
