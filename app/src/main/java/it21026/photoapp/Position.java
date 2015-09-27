package it21026.photoapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Position extends Activity implements View.OnClickListener,SensorEventListener {

    LocationManager locManager;
    private static Location currentlocation;

    private static SensorManager mSensorManager;
    private static Sensor accelerometer;
    private static Sensor magnetometer;

    private static float[] mAccelerometer = null;
    private static float[] mGeomagnetic = null;
    double azimuth;

    private static double direction;

    private static double longitude;
    private static double latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //All the sensors needed to get the location sa well as orientation
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        locManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        currentlocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        locationUpdate(true);

        Button newOKButton = (Button) findViewById(R.id.PositionOKButton);
        newOKButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.PositionOKButton:
                //Pausing the sensors
                onPause();

                //Retrieving from the intent that started this activity if it was Search or Upload
                Intent oldIntent=getIntent();
                String previousActivity = oldIntent.getStringExtra("activity");

                //Creating the intent
                Intent i = new Intent();
                i.putExtra("longitude", longitude);
                i.putExtra("latitude", latitude);
                //Using the last recorded value for the direction
                i.putExtra("direction", direction);

                if(previousActivity.equals("Search")) {
                    //Next activity in case Search started Position is Results
                    i.setClassName("it21026.photoapp", "it21026.photoapp.Results");
                }else if(previousActivity.equals("Upload")){
                    //Next activity in case Upload started Position is UploadStep2
                    i.setClassName("it21026.photoapp", "it21026.photoapp.UploadStep2");
                }

                startActivity(i);
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
        locationUpdate(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //To stop the listeners and save battery
        mSensorManager.unregisterListener(this, accelerometer);
        mSensorManager.unregisterListener(this, magnetometer);
        locationUpdate(false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // onSensorChanged gets called for each sensor so each time according values are stored
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mAccelerometer = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }
        //Checking if we have values from both sensors
        if (mAccelerometer != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mAccelerometer, mGeomagnetic);
            //If the rotation matrix is available, the azimuth value can be calcuated
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                //At this point, orientation contains the azimuth(direction), pitch and roll values. Azimuth is the only one needed now
                azimuth = 180 * orientation[0] / Math.PI;
                longitude = currentlocation.getLongitude();
                latitude = currentlocation.getLatitude();
                //Using the geomagnetic field to get the declination
                GeomagneticField geoField = new GeomagneticField((float) latitude, (float) longitude, (float) currentlocation.getAltitude(), System.currentTimeMillis());
                //Using declination to get true instead of magnetic direction
                direction = azimuth - (geoField.getDeclination());
                //Converting direction values from 0 to 360
                if(direction<0) direction += 360;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Not in use in this case
    }

    //A method for registering and uregistering a location listener
    private void locationUpdate(boolean on){
        LocationListener loclistener =new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentlocation = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        if(on) {
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, loclistener);
        }else{
            locManager.removeUpdates(loclistener);
        }
    }
}
