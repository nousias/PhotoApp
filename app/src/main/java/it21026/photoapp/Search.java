package it21026.photoapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Search extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Button newNextButton = (Button) findViewById(R.id.SearchNextButton);
        newNextButton.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.SearchNextButton:
                Intent i = new Intent();
                i.setClassName("it21026.photoapp", "it21026.photoapp.Position");
                //Putting as extra that the call was made from the Search activity (Position is called from upload also)
                i.putExtra("activity", "Search");
                startActivity(i);
                finish();
                break;
        }
    }
}
