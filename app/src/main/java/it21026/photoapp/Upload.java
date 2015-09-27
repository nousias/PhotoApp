package it21026.photoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class Upload extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        //Check if device supports camera, if not terminate activity
        if (!getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(getApplicationContext(), getString(R.string.NoCameraSupport), Toast.LENGTH_LONG).show();
            finish();
        } else {
            //Check if the user has is logged in
            SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
            int userid = spref.getInt("Id", -1);

            if (userid == -1) {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Upload.this);
                alertDialogBuilder.setTitle(getString(R.string.app_name));
                alertDialogBuilder.setMessage(getString(R.string.alert_missing_id));
                alertDialogBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        startActivity(new Intent().setClassName("it21026.photoapp", "it21026.photoapp.PhotoApp"));
                        finish();
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            } else {
                Button newUploadButton = (Button) findViewById(R.id.UploadNextButton);
                newUploadButton.setOnClickListener(this);
            }
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.UploadNextButton:
                Intent i = new Intent();
                i.setClassName("it21026.photoapp", "it21026.photoapp.Position");
                i.putExtra("activity", "Upload");
                startActivity(i);
                finish();
                break;
        }
    }
}