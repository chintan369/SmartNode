package com.nivida.smartnode.adapter;

import android.content.Context;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chintak Patel on 18-Jul-16.
 */
public class SwitchListAdapter extends ArrayAdapter<Bean_Switch> {

    Context context;
    public List<Bean_Switch> switchList=new ArrayList<>();


    public SwitchListAdapter(Context context, List<Bean_Switch> switchList){
        super(context,R.layout.custom_switch_list,switchList);
        this.context=context;
        this.switchList=switchList;
    }

    static class ViewHolder{
        protected TextView txt_switch;
        protected CheckBox chk_switch;
    }

    @Override
    public int getCount() {
        return switchList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=null;
        //if (convertView == null) {
            LayoutInflater inflater =(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_switch_list, null);
            viewHolder = new ViewHolder();
            viewHolder.txt_switch = (TextView) convertView.findViewById(R.id.txt_switch);
            viewHolder.chk_switch = (CheckBox) convertView.findViewById(R.id.check_switch);
            viewHolder.chk_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int getPosition = (Integer) buttonView.getTag();  // Here we get the position that we have set for the checkbox using setTag.
                    switchList.get(getPosition).setChecked(buttonView.isChecked()); // Set the value of checkbox to maintain its state.
                }
            });
            convertView.setTag(viewHolder);
            convertView.setTag(R.id.txt_switch, viewHolder.txt_switch);
            convertView.setTag(R.id.check_switch, viewHolder.chk_switch);
        /*} else {
            viewHolder = (ViewHolder) convertView.getTag();
        }*/
        viewHolder.chk_switch.setTag(position); // This line is important.

        if(switchList.get(position).getIsSwitch().equalsIgnoreCase("s")){
            viewHolder.txt_switch.setText(switchList.get(position).getSwitch_name());
        }
        else {
            viewHolder.txt_switch.setText(switchList.get(position).getSwitch_name());
        }


        viewHolder.chk_switch.setChecked(switchList.get(position).isChecked());

        return convertView;
    }

    public List<Bean_Switch> getSelectedSwitches(){
        return switchList;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if(switchList.size()==0){
            Toast.makeText(context, "No Switches Available in the List", Toast.LENGTH_SHORT).show();
        }
    }
}
