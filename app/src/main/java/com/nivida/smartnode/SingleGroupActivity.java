package com.nivida.smartnode;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nivida.smartnode.adapter.CustomAdapter;
import com.nivida.smartnode.app.AppPreference;

public class SingleGroupActivity extends AppCompatActivity {

    TextView drawer_txt1;
    DrawerLayout drawerLayout;
    GridView drawerList;
    ActionBarDrawerToggle drawerToggle;
    CustomAdapter customAdapter;
    private boolean isDrawerOpen=false;
    private Toolbar toolbar;
    TextView actionBarTitle,logout_txt;
    ImageView logout_img;
    private Typeface typeface_raleway;

    private AppPreference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_group);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        customAdapter=new CustomAdapter(this);
        logout_txt.setTypeface(tf);
        preference=new AppPreference(getApplicationContext());
        setFonts();
        setUpToolBar();
        setNavigationDrawer();
    }

    private void setNavigationDrawer() {
        logout_img=(ImageView)findViewById(R.id.logout_img);
        logout_txt=(TextView)findViewById(R.id.logout_txt);
        drawerLayout=(DrawerLayout) findViewById(R.id.drawerlayout);
        drawerList=(GridView) findViewById(R.id.drawerlist);
        drawerList.setAdapter(customAdapter);

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
                toolbar.setNavigationIcon(R.drawable.arrow_back);
                isDrawerOpen=true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                toolbar.setNavigationIcon(R.drawable.drawer_icon);
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

    private void setFonts() {
        typeface_raleway= Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
    }

    private void setUpToolBar() {
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        actionBarTitle=(TextView) findViewById(R.id.actionbarTitle);
        actionBarTitle.setTypeface(typeface_raleway);


        //drawer_txt1=(TextView)findViewById(R.id.drawer_txt_1);
        drawer_txt1.setTypeface(typeface_raleway);
    }

    @Override
    public void onBackPressed() {
        if(isDrawerOpen){
            drawerLayout.closeDrawer(GravityCompat.START);
            isDrawerOpen=false;
            toolbar.setNavigationIcon(R.drawable.drawer_icon);
        }
        showExitDialog();
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
