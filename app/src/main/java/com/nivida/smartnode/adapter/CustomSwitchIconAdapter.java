package com.nivida.smartnode.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_SwitchIcons;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.List;

import static com.nivida.smartnode.R.drawable.edittext_layout;

/**
 * Created by Chintak Patel on 06-Aug-16.
 */
public class CustomSwitchIconAdapter extends BaseAdapter {
    DatabaseHandler dbhandler;
    Context context;
    List<Bean_SwitchIcons> switchIconsList;

    public CustomSwitchIconAdapter(Context context, List<Bean_SwitchIcons> switchIconsList){
        this.context=context;
        dbhandler=new DatabaseHandler(context);
        this.switchIconsList=switchIconsList;
        //Toast.makeText(context,""+switchIconsList.size(),Toast.LENGTH_SHORT).show();
    }
    @Override
    public int getCount() {
        return switchIconsList.size();
    }

    @Override
    public Object getItem(int position) {
        return switchIconsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view=null;

        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view=inflater.inflate(R.layout.custom_change_switch_icon,parent,false);

        ImageView imageIcon=(ImageView) view.findViewById(R.id.imageIconForSwitch);

        if(switchIconsList.get(position).isChecked()){
            view.setBackgroundResource(R.drawable.icon_selector_bg);
        }

        imageIcon.setImageResource(switchIconsList.get(position).getSwOffId());

        return view;
    }

    public int getSwitchIconId(int position){

        return switchIconsList.get(position).getIconid();
    }

    public void setIconChecked(int position){
        for(int i=0; i<switchIconsList.size() ; i++){
            switchIconsList.get(i).setChecked(false);
        }

        switchIconsList.get(position).setChecked(true);
        notifyDataSetChanged();

    }

}
