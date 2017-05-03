package com.nivida.smartnode;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AboutUsActivity extends AppCompatActivity {
    TextView txt_aboutus,aboutus_info;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        txt_aboutus=(TextView)findViewById(R.id.txt_aboutus);
        aboutus_info=(TextView)findViewById(R.id.aboutus_info);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        txt_aboutus.setTypeface(tf);
        aboutus_info.setTypeface(tf);
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}

