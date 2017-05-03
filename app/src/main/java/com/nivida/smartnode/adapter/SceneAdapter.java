package com.nivida.smartnode.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.nivida.smartnode.R;
import com.nivida.smartnode.beans.Bean_Scenes;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.List;

/**
 * Created by Chintak Patel on 10-Aug-16.
 */
public class SceneAdapter extends BaseAdapter {

    Context context;
    List<Bean_Scenes> scenesList;
    public DatabaseHandler databaseHandler;

    public SceneAdapter(Context context, List<Bean_Scenes> scenesList){
        this.context=context;
        this.scenesList=scenesList;
        databaseHandler=new DatabaseHandler(context);
    }

    @Override
    public int getCount() {
        return scenesList.size();
    }

    @Override
    public Object getItem(int position) {
        return scenesList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view=null;

        LayoutInflater inflater=(LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view=inflater.inflate(R.layout.custom_scene_items,null);

        TextView txt_scene_name=(TextView) view.findViewById(R.id.txt_scene_name);

        txt_scene_name.setText(scenesList.get(position).getSceneName());

        return view;
    }

    public int getSceneID(int position){
        return scenesList.get(position).getSceneId();
    }

    public String getSceneName(int position){
        return scenesList.get(position).getSceneName();
    }

    public int getSceneForGroup(int position){
        return scenesList.get(position).getSceneGroup();
    }

    public void newDataAdded(int groupid) {
        scenesList.clear();
        scenesList=databaseHandler.getScenesList(groupid);
        notifyDataSetChanged();
    }
}
