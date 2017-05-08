package com.nivida.smartnode.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_EnergySlave;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nivida new on 02-Dec-16.
 */

public class EnergySlaveListAdapter extends BaseAdapter {

    Context context;
    LayoutInflater inflater;
    List<Bean_EnergySlave> energySlaveList=new ArrayList<>();
    ArrayList<String> daysList=new ArrayList<>();
    OnViewSelection onViewSelection;

    public EnergySlaveListAdapter(Context context, List<Bean_EnergySlave> energySlaveList, OnViewSelection onViewSelection) {
        this.context = context;
        this.energySlaveList = energySlaveList;
        this.onViewSelection=onViewSelection;
        inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        initDays();
    }

    @Override
    public int getCount() {
        return energySlaveList.size();
    }

    @Override
    public Object getItem(int position) {
        return energySlaveList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view=inflater.inflate(R.layout.custom_energyslave,parent,false);

        ArrayAdapter<String> daysAdapter=new ArrayAdapter<String>(context,R.layout.custom_spinner_size,daysList);

        final ImageView img_options=(ImageView) view.findViewById(R.id.img_options);
        TextView txt_slaveName=(TextView) view.findViewById(R.id.txt_slaveName);
        TextView txt_masterName=(TextView) view.findViewById(R.id.txt_masterName);
        final TextView txt_watt=(TextView) view.findViewById(R.id.txt_watt);
        TextView txt_wattUnit=(TextView) view.findViewById(R.id.txt_wattUnit);
        final TextView txt_price=(TextView) view.findViewById(R.id.txt_price);
        TextView txt_priceUnit=(TextView) view.findViewById(R.id.txt_priceUnit);
        Spinner spn_day=(Spinner) view.findViewById(R.id.spn_day);
        final LinearLayout layout_price=(LinearLayout) view.findViewById(R.id.layout_price);
        final LinearLayout layout_watt=(LinearLayout) view.findViewById(R.id.layout_watt);

        img_options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onViewSelection.onOptionSelected(position, img_options);
            }
        });

        layout_watt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_watt.setVisibility(View.GONE);
                layout_price.setVisibility(View.VISIBLE);
            }
        });

        layout_price.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout_watt.setVisibility(View.VISIBLE);
                layout_price.setVisibility(View.GONE);
            }
        });

        txt_slaveName.setText(energySlaveList.get(position).getSlaveName());
        txt_masterName.setText("("+energySlaveList.get(position).getMasterName()+")");
        txt_watt.setText(energySlaveList.get(position).getTotalWatt(Bean_EnergySlave.TODAY));
        txt_price.setText(energySlaveList.get(position).getTotalPrice(Bean_EnergySlave.TODAY));
        spn_day.setAdapter(daysAdapter);

        spn_day.setSelection(energySlaveList.get(position).getDay());

        spn_day.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                energySlaveList.get(position).setDay(pos);
                txt_watt.setText(energySlaveList.get(position).getTotalWatt(pos));
                txt_price.setText(energySlaveList.get(position).getTotalPrice(pos));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        return view;
    }

    private void initDays(){
        daysList.clear();
        daysList.add("Today");
        daysList.add("Last 7 Days");
        daysList.add("Last 30 Days");
        daysList.add("Now");
    }

    public String getSlaveID(int position){
        return energySlaveList.get(position).getSlaveID();
    }

    public String getSlaveName(int position){
        return energySlaveList.get(position).getSlaveName();
    }

    public boolean hasAdded(String slaveID){
        boolean isAdded=false;

        for(int i=0; i<energySlaveList.size(); i++){
            if(energySlaveList.get(i).getSlaveID().equalsIgnoreCase(slaveID)){
                isAdded=true;
                break;
            }
        }

        return isAdded;
    }

    public interface OnViewSelection{
        void onOptionSelected(int position,View view);
    }
}
