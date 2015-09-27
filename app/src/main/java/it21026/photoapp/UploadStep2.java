package it21026.photoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class UploadStep2 extends Activity implements View.OnClickListener{

    double longitude,latitude,direction;

    //This activity is middle step providing information to the user
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_step2);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //Retrieving location information to pass them again to the next activity
        Intent oldIntent = getIntent();
        longitude = oldIntent.getDoubleExtra("longitude", 0);
        latitude = oldIntent.getDoubleExtra("latitude", 0);
        direction = oldIntent.getDoubleExtra("direction", 0);
        //If all three are 0 then something went wrong
        if (longitude == 0 || latitude == 0 || direction == 0) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UploadStep2.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder.setMessage(getString(R.string.alert_problem_data));
            alertDialogBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent().setClassName("it21026.photoapp", "it21026.photoapp.Upload"));
                    finish();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }else{
            Button newUploadButton = (Button) findViewById(R.id.UploadStep2NextButton);
            newUploadButton.setOnClickListener(this);
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.UploadStep2NextButton:
                Intent i = new Intent();
                i.setClassName("it21026.photoapp", "it21026.photoapp.TakePhoto");
                i.putExtra("longitude", longitude);
                i.putExtra("latitude",latitude);
                i.putExtra("direction",direction);
                startActivity(i);
                finish();
                break;
        }
    }
}
