package it21026.photoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Results extends Activity {

    //Tag to identify requests
    final String TAG = Results.class.getSimpleName();
    ProgressDialog pDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        final TextView emptyresultstext= (TextView)findViewById(R.id.EmptyResultsTextView);
        final ListView photolist = (ListView)findViewById(R.id.PhotoListResults_ListView);
        //Retrieving the coordinates passed from previous activity
        Intent oldIntent=getIntent();
        double longitude=oldIntent.getDoubleExtra("longitude", 0);
        double latitude=oldIntent.getDoubleExtra("latitude", 0);
        double double_direction=oldIntent.getDoubleExtra("direction", 0);
        //If all three are 0 then something went wrong
        if(longitude==0 || latitude==0 || double_direction==0){
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(Results.this);
            alertDialogBuilder.setTitle(getString(R.string.app_name));
            alertDialogBuilder.setMessage(this.getString(R.string.alert_problem_data));
            alertDialogBuilder.setPositiveButton(this.getString(R.string.OK), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    startActivity(new Intent().setClassName("it21026.photoapp","it21026.photoapp.Search"));
                    finish();
                }
            });
            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }else {
            //Casting direction to a float to match the database
            float direction = (float) double_direction;
            //The restful webservice url to search
            String url = "http://83.212.112.140:8080/PhotoApp3/webresources/photoentities.photos/location/" + Double.toString(latitude) + "/" + Double.toString(longitude) + "/" + Float.toString(direction);
            //The list that are going to store the data from the server
            final ArrayList<PhotosLight> Photos = new ArrayList<PhotosLight>();
            //The listview from the layout xml file
            ListView ResultsListView = (ListView) findViewById(R.id.PhotoListResults_ListView);
            //Setting a click listener, images when clicked go fullscreen
            ResultsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent i = new Intent();
                    i.setClassName("it21026.photoapp", "it21026.photoapp.FullscreenPhoto");
                    i.putExtra("url",Photos.get(position).getUrl());
                    startActivity(i);
                }
            });
            final CustomListAdapter adapter = new CustomListAdapter(this, Photos);
            ResultsListView.setAdapter(adapter);

            //A wait dialog to inform user of background work being done
            pDialog=new ProgressDialog(this);
            pDialog.setMessage(getString(R.string.Searching));
            pDialog.show();

            JsonArrayRequest req = new JsonArrayRequest(url,
                    //A response listener for successful communication
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            try {
                                //If the server's response is empty
                                if (response.length() == 0) {
                                    pDialog.dismiss();
                                    //Hide the listView since there were no photos found
                                    photolist.setVisibility(View.GONE);
                                    //Set text view visible so user knows no photos were found
                                    emptyresultstext.setVisibility(View.VISIBLE);
                                //Else save photo details to the arraylist
                                } else {
                                    for (int i = 0; i < response.length(); i++) {
                                        JSONObject photo = (JSONObject) response.get(i);
                                        if(photo.has("description")) {
                                            Photos.add(new PhotosLight(photo.getString("name"), photo.getString("description"), photo.getString("url"), photo.getString("date")));
                                        }else{
                                            Photos.add(new PhotosLight(photo.getString("name"), getString(R.string.NotAvailable), photo.getString("url"), photo.getString("date")));
                                        }
                                    }
                                    pDialog.dismiss();
                                    Toast.makeText(getApplicationContext(), getString(R.string.SuccessfulSearch), Toast.LENGTH_SHORT).show();
                                    //Notifying list adapter about data changes so that it renders the list view with updated data
                                    adapter.notifyDataSetChanged();
                                }
                            //Catching exceptions
                            } catch (JSONException e) {
                                Toast.makeText(getApplicationContext(), "JSON "+getString(R.string.Error) + " " + e.getMessage(), Toast.LENGTH_LONG).show();
                                pDialog.dismiss();
                            }
                        }
                        //The error listener for the errors
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), "VolleyLib "+ getString(R.string.Error) + " " + error.getMessage(), Toast.LENGTH_LONG).show();
                            pDialog.dismiss();
                    }
            });
            VolleySingleton.getInstance().addToRequestQueue(req, TAG);

        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        pDialog.dismiss();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (VolleySingleton.getInstance().getRequestQueue() != null) {
            VolleySingleton.getInstance().cancelPendingRequests(TAG);
        }
    }

}
