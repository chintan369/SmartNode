package com.nivida.smartnode.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.a.Status;
import com.nivida.smartnode.beans.Bean_Master;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Chintak Patel on 7/5/2016.
 */
public class MasterDeviceAdapter extends BaseAdapter {

    private Context context;
    DatabaseHandler databaseHandler;
    List<Bean_Master> masterList=new ArrayList<>();
    Typeface tf;



    public MasterDeviceAdapter(Context context, List<Bean_Master> masterList){
        this.context=context;
        databaseHandler=new DatabaseHandler(context);
        this.masterList=masterList;
        tf=Typeface.createFromAsset(context.getAssets(),"fonts/raleway.ttf");
    }

    @Override
    public int getCount() {
        return masterList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return masterList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view=null;
        //if(convertView==null){
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view= inflater.inflate(R.layout.custom_add_masterdevice_grid,parent,false);
        /*}
        else{
            view=convertView;
        }*/

        Bean_Master master=masterList.get(position);
        final TextView txt_slave_name=(TextView) view.findViewById(R.id.txt_slave_name);
        final ImageView img_icon=(ImageView) view.findViewById(R.id.plus_icon);
        txt_slave_name.setText(master.getName());
        txt_slave_name.setTypeface(tf);

        if(master.getId()==200){
            txt_slave_name.setVisibility(View.GONE);
            img_icon.setVisibility(View.VISIBLE);
        }

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        masterList.clear();
        masterList=databaseHandler.getAllMasterDeviceData();
        super.notifyDataSetChanged();
    }

    public int getMasterId(int position){
        return masterList.get(position).getId();
    }

    public boolean isMasterType(int position){
        return !masterList.get(position).getType().equalsIgnoreCase(Status.STANDALONE);
    }
}
