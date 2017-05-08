package com.nivida.smartnode.adapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.siyamed.shapeimageview.CircularImageView;
import com.nivida.smartnode.GroupSwitchOnOffActivity;
import com.nivida.smartnode.MasterGroupActivity;
import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.BitmapUtility;

import java.io.File;
import java.util.List;

/**
 * Created by Chintak Patel on 14-Jul-16.
 */
public class MasterGridAdpater extends BaseAdapter {

    Context context;
    Activity activity;
    List<Bean_MasterGroup> masterGroupList;
    private static int RESULT_LOAD_IMAGE = 1;
    public Bitmap imageToGroup=null;
    private DatabaseHandler handler;
    private MasterGridCallBack callback;

    public MasterGridAdpater(Context context, Activity activity, List<Bean_MasterGroup> masterGroupList){
        this.context=context;
        this.activity=activity;
        handler=new DatabaseHandler(context);
        this.masterGroupList=masterGroupList;
    }

    @Override
    public int getCount() {
        return masterGroupList.size();
    }

    @Override
    public Object getItem(int position) {
        return masterGroupList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        System.gc();

        View view=null;

      //  if(convertView==null){
            LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view=inflater.inflate(R.layout.grid_master_group_items,parent,false);
        /*}
        else{
            view=convertView;
        }*/

        final CircularImageView img_item=(CircularImageView) view.findViewById(R.id.img_item);
        final TextView txt_item=(TextView) view.findViewById(R.id.txt_item);

        final Bean_MasterGroup masterGroup=masterGroupList.get(position);

        String groupImage=masterGroup.getImgLocalPath();

        if(masterGroup.getName().toLowerCase().equalsIgnoreCase("Add New Group")){
            Glide.with(context).load(R.drawable.add_new).into(img_item);
        }
        else if(groupImage.isEmpty()){
            Glide.with(context)
                    .load(R.drawable.room_group_default);
        }
        else {
            File file=new File(groupImage);
            if(file.exists()){
                Glide.with(context)
                        .load(file)
                        .into(img_item);
            }
            else {
                Glide.with(context)
                        .load(R.drawable.room_group_default)
                        .into(img_item);
            }


        }


        txt_item.setText(masterGroup.getName());

        view.setClickable(true);

        view.setLongClickable(true);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.e("group_id",""+masterGroup.getId());
                Log.e("count in grp",""+handler.hasSwitchesInGroup(masterGroup.getId()));

                if(masterGroup.getId()==100){
                    if(callback != null){
                        callback.showDialog();
                    }
                }
                else {
                    if(handler.hasSwitchesInGroup(masterGroup.getId())>0){
                        Intent intent=new Intent(context,GroupSwitchOnOffActivity.class);
                        intent.putExtra("group_id",masterGroup.getId());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        activity.finish();
                    }
                    else {
                        Toast.makeText(context,"No switch / dimmer in this group",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        return view;
    }

    public void setCallBack(MasterGridCallBack masterGridCallBack){
        this.callback=masterGridCallBack;
    }

    public interface MasterGridCallBack{
        public void showDialog();
    }

    @Override
    public void notifyDataSetChanged() {
        masterGroupList.clear();
        masterGroupList=handler.getAllMasterGroupData();
        super.notifyDataSetChanged();
    }
}
