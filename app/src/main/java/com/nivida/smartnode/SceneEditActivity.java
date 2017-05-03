package com.nivida.smartnode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nivida.smartnode.adapter.SceneSwitchListAdapter;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Switch;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;

public class SceneEditActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView actionBarTitle;
    Button btn_save;
    ListView switchListView;
    DatabaseHandler databaseHandler;
    AppPreference appPreference;

    List<Bean_Switch> switches=new ArrayList<>();
    SceneSwitchListAdapter switchListAdapter;

    int groupid,sceneid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scene_edit);
        databaseHandler=new DatabaseHandler(getApplicationContext());
        appPreference=new AppPreference(getApplicationContext());

        Intent intent=getIntent();
        groupid=intent.getIntExtra("group_id",0);
        sceneid=intent.getIntExtra("scene_id",0);

        if(groupid==0 || sceneid==0){
            finish();
        }

        setActionBar();
        switches=databaseHandler.getSceneSwitches(sceneid,groupid);
        Log.e("Size",""+switches.size()+" "+sceneid);
        switchListAdapter=new SceneSwitchListAdapter(getApplicationContext(),switches);

        fetchIDs();
    }

    private void setActionBar() {
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        actionBarTitle=(TextView) findViewById(R.id.actionbarTitle);
        btn_save=(Button) findViewById(R.id.btn_save);

        actionBarTitle.setText(databaseHandler.getSceneName(sceneid));

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //actionBarTitle.setTypeface(typeface_raleway);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Bean_Switch> edited_switches=switchListAdapter.getList();
                databaseHandler.updateSceneSwitches(edited_switches);
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(getApplicationContext(),SceneActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("group_id",groupid);
        startActivity(intent);
        finish();
    }

    private void fetchIDs() {

        switchListView=(ListView) findViewById(R.id.switchListView);
        switchListView.setAdapter(switchListAdapter);
    }
}
