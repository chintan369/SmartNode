package com.nivida.smartnode;

import android.content.Intent;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpActivity extends AppCompatActivity {
    TextView txt_help,help_info;
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.arrow_back);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        txt_help=(TextView)findViewById(R.id.txt_help);
        help_info=(TextView)findViewById(R.id.help_info);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        txt_help.setTypeface(tf);
        help_info.setTypeface(tf);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
