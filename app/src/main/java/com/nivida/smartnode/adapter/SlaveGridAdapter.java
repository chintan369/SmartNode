package com.nivida.smartnode.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_SlaveGroup;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by Chintak Patel on 7/5/2016.
 */
public class SlaveGridAdapter extends BaseAdapter {

    private Context context;
    DatabaseHandler databaseHandler;
    AppPreference preference;
    List<Bean_SlaveGroup> bean_slaveGroupList=new ArrayList<>();
    Typeface tf;



    public SlaveGridAdapter(Context context, List<Bean_SlaveGroup> slaveGroupList){
        this.context=context;
        databaseHandler=new DatabaseHandler(context);
        preference=new AppPreference(context);
        this.bean_slaveGroupList=slaveGroupList;
        tf=Typeface.createFromAsset(context.getAssets(),"fonts/raleway.ttf");
    }

    @Override
    public int getCount() {
        return bean_slaveGroupList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return bean_slaveGroupList.get(position);
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
            view= inflater.inflate(R.layout.custom_add_slave_grid,parent,false);
        /*}
        else{
            view=convertView;
        }*/
        Bean_SlaveGroup slaveGroup=bean_slaveGroupList.get(position);
        final TextView txt_slave_name=(TextView) view.findViewById(R.id.txt_slave_name);
        final ImageView img_icon=(ImageView) view.findViewById(R.id.plus_icon);
        txt_slave_name.setText(slaveGroup.getName());
        txt_slave_name.setTypeface(tf);

        if(slaveGroup.getId()==200){
            txt_slave_name.setVisibility(View.GONE);
            img_icon.setVisibility(View.VISIBLE);
        }

        Log.e("slave_id",bean_slaveGroupList.get(position).getId()+"");

        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        bean_slaveGroupList.clear();
        bean_slaveGroupList=databaseHandler.getAllSlaveGroupData(preference.getMasterIDForDevice());
        super.notifyDataSetChanged();
    }

    public int getSlave_id(int position){
        return bean_slaveGroupList.get(position).getId();
    }

    public String getSlaveHexID(int position){
        return bean_slaveGroupList.get(position).getHex_id();
    }
}
