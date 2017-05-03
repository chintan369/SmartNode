package com.nivida.smartnode;

import android.content.Intent;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.model.DatabaseHandler;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener,View.OnFocusChangeListener {

    AppPreference preference;
    Typeface typeface_roboto;
    DatabaseHandler databaseHandler;

    private int[] images={R.drawable.kitchen,R.drawable.bedroom,
            R.drawable.master_bedroom,R.drawable.storeroom,
            R.drawable.mainroom,R.drawable.washroom,
            R.drawable.drawingroom,R.drawable.studyroom,R.drawable.add_new};
    private String[] names={"Kitchen","Bed Room","Master Bedroom","Store Room","Main Room","Wash Room","Drawing Room","Study Room",
            "Add New Group"};
    //LinearLayout background;

    EditText edt_username,edt_password;
    Button btn_login;
    RadioGroup rdo_device_type;
    RadioButton rdo_selected_device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        preference=new AppPreference(getApplicationContext());
        databaseHandler=new DatabaseHandler(getApplicationContext());
        typeface_roboto=Typeface.createFromAsset(getAssets(),"fonts/roboto.ttf");
        fetchID();
    }

    private void checkForKeypadOpen() {
        final View activityRootView = findViewById(R.id.rootView);
        activityRootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                //r will be populated with the coordinates of your view that area still visible.
                activityRootView.getWindowVisibleDisplayFrame(r);

                int heightDiff = activityRootView.getRootView().getHeight() - (r.bottom - r.top);
                if (heightDiff > 100) { // if more than 100 pixels, its probably a keyboard...
                   // background.setVisibility(View.GONE);
                }
                else {
                   // background.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void fetchID() {
        //background=(LinearLayout) findViewById(R.id.background);

        edt_username=(EditText) findViewById(R.id.edt_username);
        edt_password=(EditText) findViewById(R.id.edt_password);

        btn_login=(Button) findViewById(R.id.btn_login);
        rdo_device_type=(RadioGroup) findViewById(R.id.rdo_device_type);

        setEvents();
        setFonts();
    }

    private void setFonts() {
        edt_username.setTypeface(typeface_roboto);
        edt_password.setTypeface(typeface_roboto);
    }

    private void setEvents() {
        edt_username.setOnFocusChangeListener(this);
        edt_password.setOnFocusChangeListener(this);
        btn_login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.btn_login:
                checkLoginCredentials();
                break;
        }
    }

    private void checkLoginCredentials() {
        String username=edt_username.getText().toString().trim();
        String password=edt_password.getText().toString().trim();

        int selected_device=rdo_device_type.getCheckedRadioButtonId();
        rdo_selected_device=(RadioButton) findViewById(selected_device);
        if(username.equals("")){
            Toast.makeText(getApplicationContext(),"Please enter User Name",Toast.LENGTH_SHORT).show();
        }
        else if(password.equals("")){
            Toast.makeText(getApplicationContext(),"Please enter Password",Toast.LENGTH_SHORT).show();
        }
        else if(password.length()<6){
            Toast.makeText(getApplicationContext(),"Password must at least 6 character long",Toast.LENGTH_SHORT).show();
        }
        else if(username.equals("nimesh") && password.equals("nopassword")){
            if(selected_device==-1){
                Toast.makeText(getApplicationContext(),"Please select Type",Toast.LENGTH_SHORT).show();
            }
            else if(rdo_selected_device.getText().toString().equalsIgnoreCase("stand-alone")){
                preference.setLoggedIn(true);
                preference.setMaster(false);
                Toast.makeText(getApplicationContext(),"Successfully Logged in",Toast.LENGTH_SHORT).show();

                Intent intent=new Intent(getApplicationContext(),SingleGroupActivity.class);
                startActivity(intent);
                finish();
                if(databaseHandler.getGroupDataCounts()==0){
                    databaseHandler.addDefaultRows(images,names);
                }
            }
            else if(rdo_selected_device.getText().toString().equalsIgnoreCase("master")){
                preference.setLoggedIn(true);
                preference.setMaster(true);
                Toast.makeText(getApplicationContext(),"Logged in successfully",Toast.LENGTH_SHORT).show();

                Log.e("Masters count : ",String.valueOf(databaseHandler.getMastersCounts()));
                if(databaseHandler.getMastersCounts()>1){
                    Intent intent=new Intent(getApplicationContext(),MasterGroupActivity.class);
                    startActivity(intent);
                }
                else {
                    Intent intent=new Intent(getApplicationContext(),AddMasterActivity.class);
                    startActivity(intent);
                }
                if(databaseHandler.getGroupDataCounts()==0){
                    databaseHandler.addDefaultRows(images,names);
                }
                finish();
            }
        }
        else {
            Toast.makeText(getApplicationContext(),"Please enter correct username and password ",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if(hasFocus){
            checkForKeypadOpen();
        }
        else{
            checkForKeypadOpen();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(edt_username.hasFocus()){
            checkForKeypadOpen();
        }
        if(edt_password.hasFocus()){
            checkForKeypadOpen();
        }
    }
}
