package com.nivida.smartnode;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.siyamed.shapeimageview.CircularImageView;
import com.nivida.smartnode.a.C;
import com.nivida.smartnode.adapter.CustomAdapter;
import com.nivida.smartnode.adapter.MasterGridAdpater;
import com.nivida.smartnode.app.AppConstant;
import com.nivida.smartnode.app.AppPreference;
import com.nivida.smartnode.beans.Bean_MasterGroup;
import com.nivida.smartnode.model.DatabaseHandler;
import com.nivida.smartnode.utils.ImagePath;
import com.nivida.smartnode.utils.Utility;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

public class MasterGroupActivity extends AppCompatActivity implements MasterGridAdpater.MasterGridCallBack {

    public static final int SELECT_PICTURE = 1;
    public static final int SELECT_PICTURE_KITKAT = 2;
    private static final int REQUEST_CAMERA = 0;
    private static final int RESULT_IMAGE_LOAD = 1;
    private static final int REQUEST_CROP_ICON = 2;
    TextView drawer_txt1;
    DrawerLayout drawerLayout;
    GridView drawerList;
    ActionBarDrawerToggle drawerToggle;
    CustomAdapter customAdapter;
    String userChoosenTask = "";
    Bitmap bitmap;
    Bitmap thePic = null;
    String selectedImagePath;
    LayoutInflater inflater;
    View dialogView;
    EditText edt_groupname;
    CircularImageView img_selectgroup;
    String currentVersion = "1.0";
    private boolean isDrawerOpen=false;
    private Toolbar toolbar;
    private TextView actionBarTitle;
    private LinearLayout btn_favourites,btn_energyMonitoring;
    private TextView txt_favourite;
    private GridView groupGrid;
    private Typeface typeface_raleway;
    private MasterGridAdpater masterGridAdpater;
    private List<Bean_MasterGroup> masterGroupList;
    private DatabaseHandler dbhandler;
    private AppPreference preference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_master_group);
        Typeface tf=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
        dbhandler=new DatabaseHandler(getApplicationContext());
        preference=new AppPreference(getApplicationContext());

        try {
            currentVersion = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            currentVersion = "1.0";
            e.printStackTrace();
        }

        Log.e("Current Version", "-->" + currentVersion);

        //new GetVersionCode().execute();

        try{
            masterGroupList=dbhandler.getAllMasterGroupData();
        }catch (Exception e){
            //C.connectionError(getApplicationContext());
        }


        masterGridAdpater=new MasterGridAdpater(getApplicationContext(),this,masterGroupList);
        masterGridAdpater.setCallBack(this);
        customAdapter=new CustomAdapter(this);

        setDialogView();
        setFonts();
        setUpToolBar();
        fetchIDs();
        setNavigationDrawer();
    }

    private void setNavigationDrawer() {
        drawerLayout=(DrawerLayout) findViewById(R.id.drawerlayout);
        drawerList=(GridView) findViewById(R.id.drawerlist);
        drawerList.setAdapter(customAdapter);

        drawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position){
                    case 0:
                        intent=new Intent(getApplicationContext(),ContactUsActivity.class);
                        startActivity(intent);
                        break;
                    case 1:
                        shareApp();
                        break;

                }
            }
        });


        toolbar.setNavigationIcon(R.drawable.drawer_icon);
        drawerToggle=new ActionBarDrawerToggle(this,drawerLayout,toolbar,R.string.open,R.string.close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
                drawerToggle.setDrawerIndicatorEnabled(true);
                //toolbar.setNavigationIcon(R.drawable.arrow_back);
                isDrawerOpen=true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu();
                drawerToggle.setDrawerIndicatorEnabled(false);
                //toolbar.setNavigationIcon(R.drawable.drawer_icon);
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


        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isDrawerOpen){
                    drawerLayout.closeDrawer(GravityCompat.START);
                    //toolbar.setNavigationIcon(R.drawable.drawer_icon);
                    drawerToggle.setDrawerIndicatorEnabled(false);
                    isDrawerOpen=false;
                }
                else{
                    drawerLayout.openDrawer(GravityCompat.START);
                    toolbar.setNavigationIcon(R.drawable.arrow_back);
                    drawerToggle.setDrawerIndicatorEnabled(true);
                    isDrawerOpen=true;
                }

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()){

            case R.id.add_master:
                intent=new Intent(getApplicationContext(),AddMasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
            case R.id.myAccount:
                intent=new Intent(getApplicationContext(),SelectDeviceForChangeAccountActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
        }

        if(drawerToggle.onOptionsItemSelected(item)){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setDialogView() {
        inflater = this.getLayoutInflater();
        dialogView = inflater.inflate(R.layout.custom_dialog_addnewgroup, null);
    }



    private void fetchIDs() {


        btn_favourites=(LinearLayout) findViewById(R.id.btn_favourites);
        btn_energyMonitoring=(LinearLayout) findViewById(R.id.btn_energyMonitoring);
        txt_favourite=(TextView) findViewById(R.id.txt_favourite);
        txt_favourite.setTypeface(typeface_raleway);

        btn_favourites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),FavouriteActivity.class);
                startActivity(i);
                finish();
            }
        });

        btn_energyMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(getApplicationContext(),EnergyMonitoringActivity.class);
                startActivity(i);
            }
        });

        groupGrid=(GridView) findViewById(R.id.groupGrid);
        int displayWidth=this.getResources().getDisplayMetrics().widthPixels;
        groupGrid.setColumnWidth((displayWidth/3)-10);
        groupGrid.setAdapter(masterGridAdpater);

        groupGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int pos=position;

                int group_id=dbhandler.getMasterGroupIdAtCurrentPosition(pos);

                Log.e("group_id",""+group_id);
                Log.e("count in grp",""+dbhandler.hasSwitchesInGroup(group_id));

                if(group_id!=100){

                    if (dbhandler.hasSwitchesInGroup(group_id)>0){
                        Intent intent=new Intent(MasterGroupActivity.this,GroupSwitchOnOffActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtra("group_id",group_id);
                        startActivity(intent);
                        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                        //finish();

                    }
                    else {
                        C.Toast(getApplicationContext(),"No switch / dimmer in this group");
                    }
                } else if (group_id == 100) {
                    showDialog(false, 0);
                }
            }
        });

        groupGrid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int pos=position;
                final int g_id=dbhandler.getMasterGroupIdAtCurrentPosition(pos);

                String[] menuItems = {"Change Title / Icon", "Remove Group"};

                AlertDialog.Builder builder = new AlertDialog.Builder(MasterGroupActivity.this);
                builder.setItems(menuItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showDialog(true, g_id);
                                break;
                            case 1:
                                showRemoveGroupDialog(g_id);
                                break;
                        }

                    }
                });

                AlertDialog dialogB = builder.create();
                dialogB.show();

                return true;
            }
        });

    }

    private void showRemoveGroupDialog(final int groupID) {
        if (groupID != 100) {
            //C.createShortCut(MasterGroupActivity.this, dbhandler.getGroupnameById(g_id), g_id);
            AlertDialog.Builder confirmDelete = new AlertDialog.Builder(MasterGroupActivity.this);
            confirmDelete.setTitle("Confirm to delete");
            confirmDelete.setMessage("Are you sure to delete this group ?");
            confirmDelete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    dbhandler.deleteMasterGroupByGroupId(groupID);
                    dbhandler.deleteAllSwitchesFromGroup(groupID);
                    Toast.makeText(MasterGroupActivity.this, "Group deleted successfully", Toast.LENGTH_SHORT).show();
                    masterGridAdpater.notifyDataSetChanged();
                    dialog.dismiss();
                }
            });
            confirmDelete.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            AlertDialog dialog = confirmDelete.create();
            dialog.show();
        }
    }

    private void setFonts() {
        typeface_raleway=Typeface.createFromAsset(getAssets(),"fonts/raleway.ttf");
    }

    private void setUpToolBar() {
        toolbar=(Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        actionBarTitle=(TextView) findViewById(R.id.actionbarTitle);
        actionBarTitle.setTypeface(typeface_raleway);


        //drawer_txt1=(TextView)findViewById(R.id.drawer_txt_1);
       // drawer_txt1.setTypeface(typeface_raleway);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        bitmap = null;
        selectedImagePath = null;

        if(resultCode==RESULT_OK && data.getData()!=null){
            Uri originalUri = null;
            if (requestCode == SELECT_PICTURE) {
                originalUri = data.getData();
                String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);
                thePic = BitmapFactory.decodeFile(originalPath);
                Bitmap.createScaledBitmap(thePic,90,90,false);
                img_selectgroup.setImageBitmap(thePic);
            } else if (requestCode == SELECT_PICTURE_KITKAT) {
                if (data.getData() != null)
                originalUri = data.getData();
                //getContentResolver().takePersistableUriPermission(originalUri,(Intent.FLAG_GRANT_READ_URI_PERMISSION| Intent.FLAG_GRANT_WRITE_URI_PERMISSION));
                String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);
                thePic = BitmapFactory.decodeFile(originalPath);
                Bitmap.createScaledBitmap(thePic,90,90,false);
                img_selectgroup.setImageBitmap(thePic);
            }
            else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data);
            }
            //String originalPath = ImagePath.getPath(getApplicationContext(),originalUri);
        }

        /*if (resultCode == Activity.RESULT_OK) {
            if (requestCode == RESULT_IMAGE_LOAD) {
                onSelectFromGalleryResult(data);
            }
            else if (requestCode == REQUEST_CAMERA) {
                onCaptureImageResult(data);
            }
        }*/

        /*if(resultCode==RESULT_OK && requestCode == REQUEST_CROP_ICON){
            Bundle extras = data.getExtras();
            if(extras != null ) {
                thePic = extras.getParcelable("data");
                Bitmap.createScaledBitmap(thePic,90,90,false);
                img_selectgroup.setImageBitmap(thePic);
            }
        }*/
    }

    private void onCaptureImageResult(Intent data) {
        if (data != null) {
            Uri selectedImage = data.getData();
            Log.e("URI",selectedImage.toString());
            String originalPath = ImagePath.getPath(getApplicationContext(),selectedImage);
            thePic = BitmapFactory.decodeFile(originalPath);
            Bitmap.createScaledBitmap(thePic,90,90,false);
            img_selectgroup.setImageBitmap(thePic);
            //performCropImage(selectedImage);
        }
    }

    private void onSelectFromGalleryResult(Intent data) {

        if (data != null) {
            Uri selectedImage = data.getData();
            Log.e("URI",selectedImage.toString());
            performCropImage(selectedImage);
        }

    }

    private void performCropImage(Uri selectedImagePath) {

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        //cropIntent.setClassName("com.google.android.gallery3d", "com.android.gallery3d.app.CropImage");
        //indicate image type and Uri
        cropIntent.setDataAndType(selectedImagePath, "image/*");
        //set crop properties
        cropIntent.putExtra("crop", "true");
        //indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1);
        cropIntent.putExtra("aspectY", 1);
        //indicate output X and Y
        cropIntent.putExtra("outputX", 256);
        cropIntent.putExtra("outputY", 256);
        //retrieve data on return
        cropIntent.putExtra("return-data", true);
        //start the activity - we handle returning in onActivityResult
        startActivityForResult(cropIntent, REQUEST_CROP_ICON);
    }

    public void showDialog(final boolean isEditMode, final int groupID) {

        thePic=null;
        String groupName = "";
        String imagePath = "";
        if (isEditMode) {
            groupName = dbhandler.getGroupnameById(groupID);
            imagePath = dbhandler.getGroupImageById(groupID);
        }

        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.custom_dialog_addnewgroup, null);
        dialogBuilder.setView(dialogView);

        edt_groupname=(EditText) dialogView.findViewById(R.id.edt_groupname);
        img_selectgroup=(CircularImageView) dialogView.findViewById(R.id.img__select_group);

        if (isEditMode) {
            edt_groupname.setText(groupName);
            if (imagePath != null && !imagePath.isEmpty()) {
                try {
                    Picasso.with(this)
                            .load(new File(imagePath))
                            .skipMemoryCache()
                            .into(img_selectgroup);
                } catch (Exception e) {
                    Log.e("Exception", e.getMessage());
                }
            }
        }

        img_selectgroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        String buttonTitle = isEditMode ? "Save" : "Add Group";
        String title = isEditMode ? "Update Group" : "Add New Group";
        dialogBuilder.setTitle(title);
        dialogBuilder.setPositiveButton(buttonTitle, null);
        dialogBuilder.setNegativeButton("Cancel", null);
        final AlertDialog b = dialogBuilder.create();

        b.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button btn_postive = b.getButton(AlertDialog.BUTTON_POSITIVE);
                /*btn_postive.setBackgroundResource(R.drawable.button_background_orange2);
                btn_postive.setTextColor(getResources().getColor(android.R.color.white));*/
                Button btn_cancel = b.getButton(AlertDialog.BUTTON_NEGATIVE);
                //btn_cancel.setBackgroundResource(R.drawable.button_background_orange);
                btn_postive.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if(edt_groupname.getText().toString().trim().equals("")){
                            Toast.makeText(MasterGroupActivity.this, "Please enter group name", Toast.LENGTH_SHORT).show();
                        } else if (!isEditMode && thePic == null) {
                            Toast.makeText(MasterGroupActivity.this, "Please select picture", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            if (isEditMode) {
                                String groupName = edt_groupname.getText().toString().trim();

                                if (thePic == null) {
                                    dbhandler.updateMasterGroupItem(groupID, groupName, null);
                                } else {
                                    img_selectgroup.setDrawingCacheEnabled(true);
                                    String groupNameID = groupName.replace(" ", "_") + "_" + groupID;
                                    String path = Environment.getExternalStorageDirectory() + "/SmartNode/Groups/" + groupNameID + ".png";
                                    File file = new File(path);
                                    if (file.exists()) file.delete();
                                    String newImagePath = C.saveGroupImageToLocal(img_selectgroup.getDrawingCache(), groupNameID);

                                    dbhandler.updateMasterGroupItem(groupID, groupName, newImagePath);
                                }
                            } else {
                                int groupID = dbhandler.getGroupLastId() == 99 ? dbhandler.getGroupLastId() + 2 : dbhandler.getGroupLastId() + 1;

                                int groupCount = dbhandler.getGroupDataCounts();
                                Bean_MasterGroup beanMasterGroup = new Bean_MasterGroup();
                                beanMasterGroup.setId(groupID);
                                beanMasterGroup.setName(edt_groupname.getText().toString());
                                beanMasterGroup.setBitmap(thePic);
                                beanMasterGroup.setHasSwitches("0");

                                if (groupCount < 2) {
                                    beanMasterGroup.setId(1);
                                }
                                img_selectgroup.setDrawingCacheEnabled(true);

                                String groupNameID = beanMasterGroup.getName().replace(" ", "_") + "_" + beanMasterGroup.getId();
                                String imagePath = C.saveGroupImageToLocal(img_selectgroup.getDrawingCache(), groupNameID);
                                beanMasterGroup.setImgLocalPath(imagePath);

                                //Toast.makeText(MasterGroupActivity.this, ""+dbhandler.getGroupLastId(), Toast.LENGTH_SHORT).show();
                                dbhandler.addMasterGroupItem(beanMasterGroup);
                            }
                            masterGridAdpater.notifyDataSetChanged();

                            b.dismiss();
                        }
                    }
                });
            }
        });
        b.show();
    }

    private String saveGroupImageToLocal(Bitmap groupPic,String groupNameID) {
        String imagePath="";

        String rootDirectory=Environment.getExternalStorageDirectory()+"/SmartNode/Groups/";
        File rootDir= new File(rootDirectory);
        if(!rootDir.exists()) rootDir.mkdir();

        String imageName=groupNameID+".jpg";

        File imageFile=new File(rootDir,imageName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            // Use the compress method on the BitMap object to write image to the OutputStream
            groupPic.compress(Bitmap.CompressFormat.JPEG, 50, fos);
            imagePath=imageFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(fos!=null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return imagePath;
    }

    public void selectImage(){
        final CharSequence[] items = {"Choose from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result= Utility.checkPermission(getApplicationContext());
                /*if (items[item].equals("Take Photo")) {
                    userChoosenTask="Take Photo";
                    if(result)
                        cameraIntent();
                } else */
                if (items[item].equals("Choose from Library")) {
                    userChoosenTask="Choose from Library";
                    if(result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    private void galleryIntent() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Choose Picture from"), SELECT_PICTURE_KITKAT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if(userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                }
                break;
        }
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onBackPressed(){
        if(isDrawerOpen){
            drawerLayout.closeDrawer(GravityCompat.START);
            isDrawerOpen=false;
            //toolbar.setNavigationIcon(R.drawable.drawer_icon);
            drawerToggle.setDrawerIndicatorEnabled(false);
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
                System.exit(0);
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
        drawerToggle.setDrawerIndicatorEnabled(false);
        //toolbar.setNavigationIcon(R.drawable.drawer_icon);
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

    private void showAlertDialogForUpdate(String currentVersion, String onlineVersion) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Version Update!");
        builder.setMessage("Hello, SmartNode's new Version " + onlineVersion + " is available on Play Store with new improvements.\n" + "Please update it from Play Store");
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String appPackageName = getPackageName();
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                }
                dialog.dismiss();
                finish();
            }
        });

        builder.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        android.support.v7.app.AlertDialog dialog = builder.create();

        if (!this.isFinishing())
            dialog.show();
    }

    private class ReceiveUDP extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void[] params) {

            String message = AppConstant.CMD_GET_MASTER_TOKEN;
            try {
                DatagramSocket socket = new DatagramSocket(13001);
                byte[] senddata = new byte[message.length()];
                senddata = message.getBytes();

                InetSocketAddress server_addr;
                DatagramPacket packet;

                Log.e("IP Address Saved", "->" + preference.getIpaddress());

                if (preference.getIpaddress().isEmpty() || !C.isValidIP(preference.getIpaddress())) {
                    server_addr = new InetSocketAddress(C.getBroadcastAddress(getApplicationContext()).getHostAddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    socket.setBroadcast(true);
                    socket.send(packet);
                } else {
                    server_addr = new InetSocketAddress(preference.getIpaddress(), 13001);
                    packet = new DatagramPacket(senddata, senddata.length, server_addr);
                    socket.setReuseAddress(true);
                    //socket.setBroadcast(true);
                    socket.send(packet);
                }

                DatagramSocket client_socket;
                client_socket = new DatagramSocket(13000);
                client_socket.setSoTimeout(2500);
                client_socket.setReuseAddress(true);

                String text = "";
                byte[] recieve_data = new byte[2048];
                DatagramPacket recvpacket = new DatagramPacket(recieve_data, recieve_data.length);
                Log.e("Packet", "Object created");
                //client_socket.setSoTimeout(60000);
                client_socket.receive(recvpacket);
                Log.e("Packet :", "Recieved");

                Log.e("Recived IP :", recvpacket.getAddress().getHostAddress());
                preference.setIpaddress(recvpacket.getAddress().getHostAddress());

                text = new String(recieve_data, 0, recvpacket.getLength());
                Log.e("Received Data :", text);

                socket.disconnect();
                socket.close();
                client_socket.disconnect();
                client_socket.close();
                return text;
                /*int port = recvpacket.getPort();

                Log.e("Received port :", String.valueOf(port));
                Log.e("Received Pac Data", recvpacket.getData().toString());*/
            } catch (SocketException s) {
                //C.Toast(getApplicationContext(), s.getLocalizedMessage());
                Log.e("Exception", "->" + s.getLocalizedMessage());
            } catch (IOException e) {
                //C.Toast(getApplicationContext(), e.getLocalizedMessage());
                Log.e("Exception", "->" + e.getLocalizedMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if (s == null || s.isEmpty()) {
                preference.setOnline(true);
            } else {
                preference.setOnline(false);
            }
        }
    }

    private class GetVersionCode extends AsyncTask<Void, String, String> {
        @Override
        protected String doInBackground(Void... voids) {

            String newVersion = null;
            try {
                newVersion = Jsoup.connect("https://play.google.com/store/apps/details?id=" + MasterGroupActivity.this.getPackageName() + "&hl=en")
                        .timeout(30000)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .referrer("http://www.google.com")
                        .get()
                        .select("div[itemprop=softwareVersion]")
                        .first()
                        .ownText();
                return newVersion;
            } catch (Exception e) {
                return newVersion;
            }
        }

        @Override
        protected void onPostExecute(String onlineVersion) {
            super.onPostExecute(onlineVersion);
            if (onlineVersion != null && !onlineVersion.isEmpty()) {
                if (Float.valueOf(currentVersion) < Float.valueOf(onlineVersion)) {
                    showAlertDialogForUpdate(currentVersion, onlineVersion);
                }
            }
            Log.e("update", "Current version " + currentVersion + "playstore version " + onlineVersion);
        }
    }
}
