package it21026.photoapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

public class FullscreenPhoto extends Activity {

    //Tag to identify this activity's requests
    final String TAG = FullscreenPhoto.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Making the activity fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_fullscreen_photo);
        //Getting photo URL and NetworkImageView item and make request
        Intent oldIntent = getIntent();
        String url = oldIntent.getStringExtra("url");
        ImageLoader imgload = VolleySingleton.getInstance().getImageLoader();
        NetworkImageView fullphoto = (NetworkImageView) findViewById(R.id.FullscreenPhoto);
        fullphoto.setTag(TAG);
        fullphoto.setImageUrl(url, imgload);

    }

    @Override
    protected void onStop() {
        super.onStop();
        //Canceling request in case it takes to long and user decides to exit the activity
        if (VolleySingleton.getInstance().getRequestQueue() != null) {
            VolleySingleton.getInstance().cancelPendingRequests(TAG);
        }
    }

}
