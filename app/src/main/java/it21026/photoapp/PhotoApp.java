package it21026.photoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class PhotoApp extends Activity implements View.OnClickListener {

    //Variable if user is connected or not
    private boolean SignedIn;

    AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_app);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        PackageManager pm= this.getPackageManager();
        boolean hasgps = pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

        //Inspecting the device for the sensors needed for the application to run
        SensorManager mSensorManager;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //If both sensors used to get direction are missing end the application
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) == null || mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)==null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PhotoApp.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder.setMessage(getString(R.string.alert_no_sensors));
            alertDialogBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent().setClassName("it21026.photoapp", "it21026.photoapp.Upload"));
                    finish();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }else if(!hasgps){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PhotoApp.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder.setMessage(getString(R.string.alert_no_gps));
            alertDialogBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent().setClassName("it21026.photoapp", "it21026.photoapp.Upload"));
                    finish();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
        }else {
            //Check if there is internet connection and gps available
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
            LocationManager manager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean GPSon = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(PhotoApp.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder.setCancelable(false);
            alertDialogBuilder.setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    finish();
                }
            });
            //If there is no internet but there is GPS
            if (!isConnected && GPSon) {
                alertDialogBuilder.setMessage(this.getString(R.string.alert_internet));
                alertDialogBuilder.setNegativeButton("Wireless Settings", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        alertDialog.dismiss();
                        finish();
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            //if there is GPS but no internet
            } else if (!GPSon && isConnected) {
                alertDialogBuilder.setMessage(this.getString(R.string.alert_gps));
                alertDialogBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        alertDialog.dismiss();
                        finish();
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            //if neither of the services are available
            } else if (!GPSon && !isConnected) {
                alertDialogBuilder.setMessage(this.getString(R.string.alert_internet_gps));
                alertDialogBuilder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent(Settings.ACTION_SETTINGS));
                        alertDialog.dismiss();
                        finish();
                    }
                });
                alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                //Check if the user has is logged in
                SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
                SignedIn = spref.getBoolean("SignedIn", false);

                Button newSearchButton = (Button) findViewById(R.id.SearchButton);
                newSearchButton.setOnClickListener(this);

                Button newUploadButton = (Button) findViewById(R.id.UploadButton);
                newUploadButton.setOnClickListener(this);
            }
        }

    }

    @Override
    protected void onResume(){
        super.onResume();
        //Check if the user has is logged in, change menu accordingly
        SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
        SignedIn = spref.getBoolean("SignedIn", false);
        invalidateOptionsMenu();
    }

    @Override
    public void onClick(View v) {
        Intent i = new Intent();
        switch (v.getId()) {
            case R.id.SearchButton:
                i.setClassName("it21026.photoapp", "it21026.photoapp.Search");
                startActivity(i);
                break;
            case R.id.UploadButton:
                //Check if user has logged in if not inform to login first
                if (!SignedIn) {
                    Toast.makeText(getApplicationContext(), getString(R.string.ToastLogin), Toast.LENGTH_LONG).show();
                } else {
                    i.setClassName("it21026.photoapp", "it21026.photoapp.Upload");
                    startActivity(i);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_photo_app, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Displaying the signin or the profile icon
        MenuItem login_item = menu.findItem(R.id.action_profile);
        MenuItem logout_item = menu.findItem(R.id.action_login);
        if (SignedIn) {
            login_item.setVisible(true);
            logout_item.setVisible(false);
        }else{
            login_item.setVisible(false);
            logout_item.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent i = new Intent();

        // Handle presses on the action bar items
        switch (id) {
            case R.id.action_login:
                //Logging in using google sign in process
                i.setClassName("it21026.photoapp", "it21026.photoapp.Profile");
                startActivity(i);
                return true;
            case R.id.action_profile:
                i.setClassName("it21026.photoapp", "it21026.photoapp.Profile");
                startActivity(i);
                return true;
            case R.id.about:
                i.setClassName("it21026.photoapp", "it21026.photoapp.About");
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
