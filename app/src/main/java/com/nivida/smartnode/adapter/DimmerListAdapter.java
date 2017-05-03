package com.nivida.smartnode.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Dimmer;
import com.nivida.smartnode.beans.Bean_Switch;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Chintak Patel on 18-Jul-16.
 */
public class DimmerListAdapter extends ArrayAdapter<Bean_Dimmer> {

    Context context;
    public List<Bean_Dimmer> dimmerList=new ArrayList<>();


    public DimmerListAdapter(Context context, List<Bean_Dimmer> dimmerList){
        super(context,R.layout.custom_dimmer_list,dimmerList);
        this.context=context;
        this.dimmerList=dimmerList;
    }

    static class ViewHolder{
        protected TextView txt_dimmer;
        protected CheckBox chk_dimmer;
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
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder=null;
        //if (convertView == null) {
            LayoutInflater inflater =(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.custom_dimmer_list, null);
            viewHolder = new ViewHolder();
            viewHolder.txt_dimmer = (TextView) convertView.findViewById(R.id.txt_dimmer);
            viewHolder.chk_dimmer = (CheckBox) convertView.findViewById(R.id.check_dimmer);
            viewHolder.chk_dimmer.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int getPosition = (Integer) buttonView.getTag();  // Here we get the position that we have set for the checkbox using setTag.
                    dimmerList.get(getPosition).setChecked(buttonView.isChecked()); // Set the value of checkbox to maintain its state.
                }
            });
            convertView.setTag(viewHolder);
            convertView.setTag(R.id.txt_dimmer, viewHolder.txt_dimmer);
            convertView.setTag(R.id.check_dimmer, viewHolder.chk_dimmer);
        /*} else {
            viewHolder = (ViewHolder) convertView.getTag();
        }*/
        viewHolder.chk_dimmer.setTag(position); // This line is important.

        viewHolder.txt_dimmer.setText(dimmerList.get(position).getDimmer_name());
        viewHolder.chk_dimmer.setChecked(dimmerList.get(position).isChecked());

        return convertView;
    }

    public List<Bean_Dimmer> getSelectedDimmers(){
        return dimmerList;
    }
}
