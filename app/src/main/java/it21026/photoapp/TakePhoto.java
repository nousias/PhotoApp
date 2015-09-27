package it21026.photoapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TakePhoto extends Activity implements View.OnClickListener {

    //Tag to identify requests
    final String TAG = Results.class.getSimpleName();

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;

    //The file where the captured image is going to be saved
    File photoFile = null;

    ProgressDialog pDialog;

    private double longitude;
    private double latitude;
    private double double_direction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        pDialog = new ProgressDialog(this);

        //Retrieve location information
        Intent oldIntent = getIntent();
        longitude = oldIntent.getDoubleExtra("longitude", 0);
        latitude = oldIntent.getDoubleExtra("latitude", 0);
        double_direction = oldIntent.getDoubleExtra("direction", 0);
        Button OK = (Button) findViewById(R.id.OKTakePhoto);
        OK.setOnClickListener(this);

        //Start the intent to take photograph
        dispatchTakePictureIntent();
    }

    //Button on click listener
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.OKTakePhoto:
                uploadData();
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //If there are any pending volley requests from this activity cancel them
        if (VolleySingleton.getInstance().getRequestQueue() != null) {
            VolleySingleton.getInstance().cancelPendingRequests(TAG);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getApplicationContext(), getString(R.string.ImageFileError), Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PhotoApp_" + timeStamp + "_JPEG.jpg";
        //The file is stored in the public photos directory
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return new File(storageDir, imageFileName);
    }

    //This runs after photograph is taken
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            //Successfully captured the image
            if (resultCode == RESULT_OK) {
                if(photoFile.length()>2.5*1024*1024) retakePhoto();
                // bitmap factory options
                BitmapFactory.Options options = new BitmapFactory.Options();
                // down sizing image as it throws OutOfMemory Exception for larger images
                options.inSampleSize = 8;
                Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
                //Display the image
                ImageView newPhotoThumbnail = (ImageView) findViewById(R.id.UploadThumbnail);
                newPhotoThumbnail.setImageBitmap(myBitmap);
            } else if (resultCode == RESULT_CANCELED) {
                //User cancelled Image capture
                Toast.makeText(getApplicationContext(), getString(R.string.ImageCaptureCanceled), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                //Failed to capture image
                Toast.makeText(getApplicationContext(), getString(R.string.ImageCaptureError), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void uploadData() {
        pDialog.setMessage(getString(R.string.UploadData));
        pDialog.show();

        //Casting direction to a float to match the database
        float direction = (float) double_direction;

        //Retrieving the user id
        SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
        int userid = spref.getInt("Id", -1);

        //Getting the new name and description user might has provided
        EditText nameText = (EditText) findViewById(R.id.NameEditTakePhoto);
        String nametxt = nameText.getText().toString();

        EditText descritpionText = (EditText) findViewById(R.id.DescriptionEditTakePhoto);
        String descriptiontxt = descritpionText.getText().toString();

        String photoName = photoFile.getName();
        if (nametxt.trim().length() != 0) {
            photoName = nametxt+".jpg";
        }

        //The json object containing all of the photo's information
        JSONObject newPhotoData = new JSONObject();
        try {
            newPhotoData.put("latitude", latitude);
            newPhotoData.put("longitude", longitude);
            newPhotoData.put("direction", direction);
            newPhotoData.put("userId", userid);
            newPhotoData.put("date", dateTime());
            newPhotoData.put("name", photoName);
            newPhotoData.put("url", "notyetavailable");
            if (descriptiontxt.trim().length() != 0) {
                newPhotoData.put("description", descriptiontxt);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //The restful webservice to create new photo entry in database
        String postDataUrl = "http://83.212.112.140:8080/PhotoApp3/webresources/photoentities.photos/newPhotoData";
        JsonObjectRequest photoData = new JsonObjectRequest(Request.Method.POST, postDataUrl, newPhotoData,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        pDialog.dismiss();
                        //The request was successfuly sent but server's response was empty
                        if (response.length() == 0) {
                            Toast.makeText(getApplicationContext(), getString(R.string.ErrorUploadData), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            int id = -1;
                            try {
                                //Getting the id of the newly created entry in the database
                                id = response.getInt("photoId");
                            } catch (Exception e) {
                                //Informing of the error that occurred due to problematic json response
                                Toast.makeText(getApplicationContext(), getString(R.string.ServerError), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            //If id was changed-sent
                            if (id != -1) {
                                //Start the photo upload
                                new uploadPhoto().execute(id);
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.ServerError), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    }
                }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
                pDialog.dismiss();
                Toast.makeText(getApplicationContext(), getString(R.string.ServerError), Toast.LENGTH_SHORT).show();
                if (error.networkResponse != null) {
                    Toast.makeText(getApplicationContext(), "Error Response code: " + error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                }
            }
        }
        );
        //Adding request to the queue
        VolleySingleton.getInstance().addToRequestQueue(photoData, TAG);
    }

    //Method for getting the current time and date in MySQL compatible format
    private String dateTime() {
        Calendar cal = Calendar.getInstance();
        String DATE_FORMAT_NOW = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        String date = sdf.format(cal.getTime());
        //ZZZZZ is supported from api 18 and up. In case api is lower add semicolon to the string
        int api=android.os.Build.VERSION.SDK_INT;
        if(api<18) {
            //Semicolon is being added to the string
            date = new StringBuffer(date).insert(date.length() - 2, ":").toString();
        }
        return date;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(pDialog.isShowing()) {
            pDialog.dismiss();
        }
    }

    public class uploadPhoto extends AsyncTask<Integer, Void, Boolean> {

        private ProgressDialog pd = new ProgressDialog(TakePhoto.this);
        protected void onPreExecute() {
            super.onPreExecute();
            pd.setMessage(getString(R.string.UploadPhoto));
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected Boolean doInBackground(Integer... id) {
            String result;
            String photoUploadUrl="http://83.212.112.140:8080/PhotoApp3Upload/files/upload/photo";
            try
            {
                //Temporarily renaming File to id for uploading
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File newName = new File(storageDir, String.valueOf(id[0])+".jpg");
                photoFile.renameTo(newName);
                HttpClient client = new DefaultHttpClient();
                HttpPost post = new HttpPost(photoUploadUrl);

                MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
                entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                //Add the file to the request
                if(newName != null)
                {
                    entityBuilder.addBinaryBody("file", newName);
                }
                //Adding the photo id as well for naming the file on the server
                HttpEntity entity = entityBuilder.build();
                post.setEntity(entity);
                HttpResponse response = client.execute(post);
                HttpEntity httpEntity = response.getEntity();
                newName.renameTo(photoFile);
                if(response.getStatusLine().getStatusCode()==200){
                    return true;
                }else{
                    return false;
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
                return false;
            }

        }

        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            pd.dismiss();
            if(result){
                Toast.makeText(getApplicationContext(), getString(R.string.SuccessfulUploadPhoto), Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.ErrorUploadPhoto), Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }

    //If photo was exceeds size photo is retaken
    private void retakePhoto(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(TakePhoto.this);
        alertDialogBuilder.setTitle(getString(R.string.app_name));
        alertDialogBuilder.setMessage(getString(R.string.FileTooBig));
        alertDialogBuilder.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dispatchTakePictureIntent();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
}
