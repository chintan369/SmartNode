package com.nivida.smartnode;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.model.DatabaseHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Add_IpActivity extends AppCompatActivity {
    EditText edt_ipaddress,edt_port,edt_hashkey;
    LinearLayout btn_cancel,btn_ok;
    TextView txt_ipaddress,txt_port,txt_hashkey,txt_add_ip,txt_cancel,txt_ok;
    AppPreference appPreference;
    DatabaseHandler databaseHandler;

    private int[] images={R.drawable.kitchen,R.drawable.bedroom,
            R.drawable.master_bedroom,R.drawable.storeroom,
            R.drawable.mainroom,R.drawable.washroom,
            R.drawable.drawingroom,R.drawable.studyroom,R.drawable.add_new};
    private String[] names={"Kitchen","Bed Room","Master Bedroom","Store Room","Main Room","Wash Room","Drawing Room","Study Room",
            "Add New Group"};

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ip);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        appPreference=new AppPreference(getApplicationContext());
        databaseHandler=new DatabaseHandler(getApplicationContext());
        fetchid();
        edt_ipaddress.setTypeface(tf);
        edt_port.setTypeface(tf);
        edt_hashkey.setTypeface(tf);
        txt_add_ip.setTypeface(tf);
        txt_ipaddress.setTypeface(tf);
        txt_port.setTypeface(tf);
        txt_hashkey.setTypeface(tf);
        txt_cancel.setTypeface(tf);
        txt_ok.setTypeface(tf);


    }

    private void fetchid() {
        edt_ipaddress=(EditText)findViewById(R.id.edt_ipaddress);
        edt_port=(EditText)findViewById(R.id.edt_port);
        edt_hashkey=(EditText)findViewById(R.id.edt_hashkey);
        txt_ipaddress=(TextView)findViewById(R.id.txt_ipaddress);
        txt_port=(TextView)findViewById(R.id.txt_port);
        txt_hashkey=(TextView)findViewById(R.id.txt_hashkey);
        txt_add_ip=(TextView)findViewById(R.id.txt_add_ip);
        btn_cancel=(LinearLayout) findViewById(R.id.btn_cancel);
        btn_ok=(LinearLayout) findViewById(R.id.btn_ok);

        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
        edt_ipaddress.setFilters(filters);

        toolbar=(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(appPreference.isConfigured()){
                    Intent i=new Intent(getApplicationContext(),MasterGroupActivity.class);
                    startActivity(i);
                    finish();
                }
                else {
                    Intent i=new Intent(getApplicationContext(),ConfigurationActivity.class);
                    startActivity(i);
                    finish();
                }
            }
        });

        txt_cancel=(TextView)findViewById(R.id.txt_cancel);
        txt_ok=(TextView)findViewById(R.id.txt_ok);

        if(appPreference.isConfigured()){
            edt_ipaddress.setText(appPreference.getIpaddress());
            edt_port.setText(appPreference.getPortnumber());
            edt_hashkey.setText(appPreference.getHashkey());
        }

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edt_ipaddress.setText("");
                edt_port.setText("");
                edt_hashkey.setText("");
                onBackPressed();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String ipaddress,portnumber,hashkey;
                ipaddress=edt_ipaddress.getText().toString();
                portnumber=edt_port.getText().toString();
                hashkey=edt_hashkey.getText().toString();

                if(ipaddress.equals("")){
                    Toast.makeText(getApplicationContext(),"Please enter IP Address",Toast.LENGTH_SHORT).show();

                }
                else if(!isIpAddressValid(ipaddress)){
                    Toast.makeText(getApplicationContext(),"Please enter correct IP address",Toast.LENGTH_SHORT).show();

                }
                else if(portnumber.equals("")){
                    Toast.makeText(getApplicationContext(),"Please enter Port Number",Toast.LENGTH_SHORT).show();

                }
                else if(Integer.parseInt(portnumber.toString())==0){
                    Toast.makeText(getApplicationContext(),"Please enter correct Port Number",Toast.LENGTH_SHORT).show();
                }
                else if(portnumber.length()>4 || portnumber.length()<2){
                    Toast.makeText(getApplicationContext(),"Please enter correct Port number",Toast.LENGTH_SHORT).show();

                }
                else if(hashkey.equals("")){
                    Toast.makeText(getApplicationContext(),"Please enter Hash Key",Toast.LENGTH_SHORT).show();

                }
                else
                {
                    appPreference.setConfigured(true);
                    appPreference.setIpaddress(ipaddress);
                    appPreference.setPortnumber(portnumber);
                    appPreference.setHashkey(hashkey);

                    if(databaseHandler.getGroupDataCounts()==0){
                        databaseHandler.addDefaultRows(images,names);
                    }

                    if(appPreference.isMaster()){
                        Intent i=new Intent(getApplicationContext(),MasterGroupActivity.class);
                        startActivity(i);
                    }
                    else{
                        Intent i=new Intent(getApplicationContext(),SingleGroupActivity.class);
                        startActivity(i);
                    }

                    finish();
                }

            }

        });



    }
    public static boolean isIpAddressValid(String ipaddress) {

        final String PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

        Pattern pattern = Pattern.compile(PATTERN);
        Matcher matcher = pattern.matcher(ipaddress);
        return matcher.matches();
    }

    @Override
    public void onBackPressed() {
        if(appPreference.isConfigured()){
            Intent i=new Intent(getApplicationContext(),MasterGroupActivity.class);
            startActivity(i);
            finish();
        }
        else {
            Intent i=new Intent(getApplicationContext(),ConfigurationActivity.class);
            startActivity(i);
            finish();
        }

    }
}

