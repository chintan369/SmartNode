package com.nivida.smartnode;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContactUsActivity extends AppCompatActivity {


    Toolbar toolbar;
    TextView txt_title;

    TextView txt_salesEmail,txt_salesPhone1,txt_salesPhone2,txt_techEmail,txt_techPhone1,txt_techPhone2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);
        setUpToolbar();

        fetchIDs();

    }

    private void fetchIDs() {
        txt_salesEmail = (TextView) findViewById(R.id.txt_salesEmail);
        txt_salesPhone1 = (TextView) findViewById(R.id.txt_salesPhone1);
        txt_salesPhone2 = (TextView) findViewById(R.id.txt_salesPhone2);
        txt_techEmail = (TextView) findViewById(R.id.txt_techEmail);
        txt_techPhone1 = (TextView) findViewById(R.id.txt_techPhone1);
        txt_techPhone2 = (TextView) findViewById(R.id.txt_techPhone2);

        txt_salesEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{txt_salesEmail.getText().toString()});
                email.putExtra(Intent.EXTRA_SUBJECT, "Regarding Sales Inquiries for Smartnode App");
                email.setType("message/rfc822");

                startActivity(Intent.createChooser(email, "Choose an Email Sender"));
            }
        });

        txt_techEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent email = new Intent(Intent.ACTION_SEND);
                email.putExtra(Intent.EXTRA_EMAIL, new String[]{txt_salesEmail.getText().toString()});
                email.putExtra(Intent.EXTRA_SUBJECT, "Regarding Technical Inquiries for Smartnode App");
                email.setType("message/rfc822");

                startActivity(Intent.createChooser(email, "Choose an Email Sender"));
            }
        });

        txt_salesPhone1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+txt_salesPhone1.getText().toString()));
                startActivity(callIntent);
            }
        });

        txt_salesPhone2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+txt_salesPhone2.getText().toString()));
                startActivity(callIntent);
            }
        });

        txt_techPhone1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+txt_techPhone1.getText().toString()));
                startActivity(callIntent);
            }
        });

        txt_techPhone2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:"+txt_techPhone2.getText().toString()));
                startActivity(callIntent);
            }
        });
    }

    private void setUpToolbar(){
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        txt_title=(TextView) findViewById(R.id.txt_title);
        txt_title.setText("Contact us");
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
