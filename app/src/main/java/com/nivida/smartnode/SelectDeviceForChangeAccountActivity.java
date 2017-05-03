package com.nivida.smartnode;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nivida.smartnode.adapter.MasterDeviceAdapter;
import com.nivida.smartnode.adapter.MasterDeviceToManageAccountAdapter;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_Master;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.ArrayList;
import java.util.List;

public class SelectDeviceForChangeAccountActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView txt_title;

    LinearLayout layout_nodevice;
    GridView masterdevicelist;
    ProgressBar progressbar;

    DatabaseHandler db;
    AppPreference preference;
    List<Bean_Master> masterList=new ArrayList<>();
    MasterDeviceToManageAccountAdapter masterDeviceAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_device_for_change_account);

        preference=new AppPreference(getApplicationContext());
        db=new DatabaseHandler(getApplicationContext());

        setUpToolbar();
        fetchIds();
    }

    private void setUpToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        txt_title = (TextView) findViewById(R.id.txt_title);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        txt_title.setText("Select Device");

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void fetchIds() {
        layout_nodevice=(LinearLayout) findViewById(R.id.layout_nodevice);
        masterdevicelist=(GridView) findViewById(R.id.masterdevicelist);
        progressbar=(ProgressBar) findViewById(R.id.progressbar);
        masterDeviceAdapter=new MasterDeviceToManageAccountAdapter(getApplicationContext(),masterList);
        masterdevicelist.setAdapter(masterDeviceAdapter);

        int totalDevices=db.getMastersCounts();

        if(totalDevices>1){
            layout_nodevice.setVisibility(View.GONE);
            masterdevicelist.setVisibility(View.VISIBLE);
            showMasterDevices();
        }

        masterdevicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String masterID=masterList.get(position).getMasterID();
                String userType=masterList.get(position).getUserType();

                Intent intent=new Intent(getApplicationContext(),MyAccountActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("masterID",masterID);
                intent.putExtra("userType",userType);
                startActivity(intent);
                finish();
            }
        });



    }

    private void showMasterDevices() {
        masterList=db.getAllMasterDeviceDataWOADD();
        Log.e("Size",masterList.size()+"-"+db.getMastersCounts());
        masterDeviceAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
