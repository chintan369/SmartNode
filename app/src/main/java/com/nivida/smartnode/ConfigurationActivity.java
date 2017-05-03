package com.nivida.smartnode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.adapter.CustomAdapter;
import com.nivida.smartnode.app.AppPreference;

public class ConfigurationActivity extends AppCompatActivity {
    TextView smartnode,txt_1,txt_2,logout_txt;//drawer_txt1;
    DrawerLayout drawerLayout;
    //ListView drawerList;
    GridView drawerList;
    ActionBarDrawerToggle drawerToggle;
    CustomAdapter customAdapter;
    Toolbar toolbar;
    Button btn_signup;
    private boolean isDrawerOpen=false;
    private AppPreference preference;
    ImageView img_setting,logout_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuration);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        preference=new AppPreference(getApplicationContext());
        customAdapter=new CustomAdapter(this);
        fetchid();
        smartnode.setTypeface(tf);
        txt_1.setTypeface(tf);
        txt_2.setTypeface(tf);
        btn_signup.setTypeface(tf);
        logout_txt.setTypeface(tf);
        //drawer_txt1.setTypeface(tf);

    }

    private void fetchid() {
        logout_img=(ImageView)findViewById(R.id.logout_img);
        logout_txt=(TextView)findViewById(R.id.logout_txt);
        smartnode=(TextView)findViewById(R.id.txt_smartnode);
        txt_1=(TextView)findViewById(R.id.txt_1);
        txt_2=(TextView)findViewById(R.id.txt_2);
        //drawer_txt1=(TextView)findViewById(R.id.drawer_txt_1);
        img_setting=(ImageView)findViewById(R.id.img_setting);

        img_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(),Add_IpActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btn_signup=(Button) findViewById(R.id.btn_signup);
        btn_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(getApplicationContext(),Add_IpActivity.class);
                startActivity(intent);
                finish();
            }
        });
        drawerLayout=(DrawerLayout) findViewById(R.id.drawerlayout);
       // drawerList=(ListView) findViewById(R.id.drawerlist);
        drawerList=(GridView) findViewById(R.id.drawerlist);
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        drawerList.setAdapter(customAdapter);
       // drawerList.setDivider(null);      //for listview


        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position){
                    case 0:
                        intent=new Intent(getApplicationContext(),AboutUsActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        intent=new Intent(getApplicationContext(),ContactUsActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        shareApp();
                        break;
                    case 3:
                        intent=new Intent(getApplicationContext(),HelpActivity.class);
                        startActivity(intent);
                        break;

                }
            }
        });

        logout_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });

        drawerToggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                isDrawerOpen=true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                isDrawerOpen=false;
            }


        };
        drawerLayout.post(new Runnable() {
            @Override
            public void run() {
                drawerToggle.syncState();
            }
        });
        drawerLayout.setDrawerListener(drawerToggle);
        drawerToggle.setDrawerIndicatorEnabled(false);
        toolbar.setNavigationIcon(R.drawable.drawer_icon);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isDrawerOpen){
                    drawerLayout.closeDrawer(GravityCompat.START);
                    toolbar.setNavigationIcon(R.drawable.drawer_icon);
                    isDrawerOpen=false;
                }
                else{
                    drawerLayout.openDrawer(GravityCompat.START);
                    toolbar.setNavigationIcon(R.drawable.arrow_back);
                    isDrawerOpen=true;
                }

            }
        });
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(drawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(isDrawerOpen){
            drawerLayout.closeDrawer(GravityCompat.START);
            isDrawerOpen=false;
            toolbar.setNavigationIcon(R.drawable.drawer_icon);
        }
        else{
           showExitDialog();
        }
    }

    public void showExitDialog(){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage("Are you sure to exit ?");
        dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });
        dialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.show();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    public void logout(){
        drawerLayout.closeDrawer(GravityCompat.START);
        isDrawerOpen=false;
        toolbar.setNavigationIcon(R.drawable.drawer_icon);
        AlertDialog.Builder logoutDialog=new AlertDialog.Builder(this);
        logoutDialog.setMessage("Are you sure to logout ?");
        logoutDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                preference.setLoggedIn(false);
                preference.setMaster(false);
                Toast.makeText(getApplicationContext(), "Successfully logged out", Toast.LENGTH_SHORT).show();
                Intent i=new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(i);
                finish();
            }
        });
        logoutDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        logoutDialog.show();
    }

    public void shareApp(){
        try {
            String shareBody = Globals.share;
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            //sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,""+s+"(Open it in Google Play Store to Download the Application)");

            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
        } catch (Exception e) {

        }
    }
}
