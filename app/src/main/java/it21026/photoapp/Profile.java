package it21026.photoapp;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Toast;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import android.app.ProgressDialog;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;
import android.content.SharedPreferences;
import android.content.IntentSender.SendIntentException;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.JsonObjectRequest;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;


public class Profile extends Activity implements OnClickListener, ConnectionCallbacks, OnConnectionFailedListener {

    private static final int RC_SIGN_IN = 0;
    //Tag to identify this activity's requests
    private static final String TAG = Profile.class.getSimpleName();
    // Profile pic image size in pixels | Default is 50x50
    private static final int PROFILE_PIC_SIZE = 400;
    // Google client to interact with Google API
    private GoogleApiClient mGoogleApiClient;
    //A flag indicating that a PendingIntent is in progress and prevents us from starting further intents.
    private boolean mIntentInProgress;

    private boolean mSignInClicked;

    private ConnectionResult mConnectionResult;

    ProgressDialog pDialog;

    private SignInButton SignInButton;
    private Button SignOutButton, RevokeAccessButton;
    private NetworkImageView ProfilePic;
    private TextView ProfileName, ProfileEmail;

    private String personEmail;
    private String personPhotoUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        SignInButton = (SignInButton) findViewById(R.id.SignInButton);
        SignOutButton = (Button) findViewById(R.id.SignOutButton);
        RevokeAccessButton = (Button) findViewById(R.id.RevokeAccessButton);
        ProfilePic = (NetworkImageView) findViewById(R.id.ProfilePic);
        ProfileName = (TextView) findViewById(R.id.ProfileName);
        ProfileEmail = (TextView) findViewById(R.id.ProfileEmail);

        SignInButton.setOnClickListener(this);
        SignOutButton.setOnClickListener(this);
        RevokeAccessButton.setOnClickListener(this);

        pDialog=new ProgressDialog(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addScope(Plus.SCOPE_PLUS_LOGIN).build();
    }

    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        //Cancel all requests upon user exiting the application
        if(VolleySingleton.getInstance().getRequestQueue() != null) {
            VolleySingleton.getInstance().cancelPendingRequests(TAG);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.SignInButton:
                setMessage(1);
                signInWithGoogle();
                break;
            case R.id.SignOutButton:
                setMessage(2);
                signOutFromGplus();
                break;
            case R.id.RevokeAccessButton:
                setMessage(3);
                revokeGplusAccess();
                break;
        }
    }

    //Sign-in into google
    private void signInWithGoogle() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }

    //Method to resolve any sign in errors
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        mSignInClicked = false;
        Toast.makeText(this, getString(R.string.ConnectedUser), Toast.LENGTH_LONG).show();
        // Get user's information
        getProfileInformation();
        pDialog.hide();
        //Update the UI after sign in
        updateUI(true);

    }

    //Updating the UI, showing/hiding buttons and profile layout
    private void updateUI(boolean isSignedIn) {
        if (isSignedIn) {
            SignInButton.setVisibility(View.GONE);
            ProfilePic.setVisibility(View.VISIBLE);
            ProfileName.setVisibility(View.VISIBLE);
            ProfileEmail.setVisibility(View.VISIBLE);
            SignOutButton.setVisibility(View.VISIBLE);
            RevokeAccessButton.setVisibility(View.VISIBLE);
        } else {
            SignInButton.setVisibility(View.VISIBLE);
            ProfilePic.setVisibility(View.GONE);
            ProfileName.setVisibility(View.GONE);
            ProfileEmail.setVisibility(View.GONE);
            SignOutButton.setVisibility(View.GONE);
            RevokeAccessButton.setVisibility(View.GONE);
        }
    }

    //Fetching user's information and getting user Id from application's datbase
    private void getProfileInformation() {
        setMessage(4);
        try {
            if (Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) != null) {
                Person currentPerson = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
                String personName = currentPerson.getDisplayName();
                personPhotoUrl = currentPerson.getImage().getUrl();
                personEmail = Plus.AccountApi.getAccountName(mGoogleApiClient);
                String personGooglePlusProfile = currentPerson.getUrl();

                ProfileName.setText(personName);
                ProfileEmail.setText(personEmail);

                //Load the profile photo
                profilePhoto();

                //Sending email to server.If user doesn't exists new entry in database is created. The method returns the user's id.
                String url="http://83.212.112.140:8080/PhotoApp3/webresources/userentities.users/getId/" + personEmail;
                StringRequest req = new StringRequest(Request.Method.GET,url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                //Casting the server's response to an int
                                saveUser(Integer.valueOf(response));
                            }
                        },new Response.ErrorListener() {
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null) {
                                error.printStackTrace();
                                Toast.makeText(getApplicationContext(),"Error Response code: " +  error.networkResponse.statusCode, Toast.LENGTH_LONG).show();
                            }
                        }
                });
                //Adding request to the queue
                VolleySingleton.getInstance().addToRequestQueue(req,TAG);
            } else {
                Toast.makeText(getApplicationContext(),"Person information is null", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),getString(R.string.UserDataError), Toast.LENGTH_LONG).show();
        }
        pDialog.dismiss();
    }

    private void profilePhoto(){
        //Adjusting the url for custom image size
        personPhotoUrl = personPhotoUrl.substring(0,personPhotoUrl.length() - 2)+ PROFILE_PIC_SIZE;
        ImageLoader imgload = VolleySingleton.getInstance().getImageLoader();
        ProfilePic.setTag(TAG);
        ProfilePic.setImageUrl(personPhotoUrl, imgload);
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
        updateUI(false);
    }

    //Sign-out from google
    private void signOutFromGplus() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            mGoogleApiClient.disconnect();
            mGoogleApiClient.connect();
            //Clearing shared preferences data
            clearUser();
            updateUI(false);
            pDialog.dismiss();
        }
    }

    //Revoking access from google
    private void revokeGplusAccess() {
        if (mGoogleApiClient.isConnected()) {
            Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
            Plus.AccountApi.revokeAccessAndDisconnect(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status arg0) {
                    mGoogleApiClient.connect();
                    setRevoked();
                    clearUser();
                    updateUI(false);
                    pDialog.dismiss();
                }
            });
        }
    }

    //Informing the database that user has revoked access
    private void setRevoked(){
        String url="http://83.212.112.140:8080/PhotoApp3/webresources/userentities.users/setRevoked/"+ personEmail;
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.PUT,url,null,null,null);
        VolleySingleton.getInstance().addToRequestQueue(jsonObjReq);
    }

    //Save user's profile to shared preferencess
    private void saveUser(int id){
        SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor=spref.edit();
        editor.putInt("Id", id);
        editor.putBoolean("SignedIn", true);
        editor.apply();
    }

    //Remove profile info from SharedPreferences
    private void clearUser(){
        SharedPreferences spref = getApplicationContext().getSharedPreferences("PhotoAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor=spref.edit();
        editor.remove("Id");
        editor.putBoolean("SignedIn", false);
        editor.apply();
    }

    //Progress dialog for each process that might be executed
    private void setMessage(int x){
        switch (x){
            //Sign in
            case 1:
                pDialog.setMessage(getString(R.string.SigningIn));
                break;
            //Sign out
            case 2:
                pDialog.setMessage(getString(R.string.SigningOut));
                break;
            //Revoke
            case 3:
                pDialog.setMessage(getString(R.string.Connecting));
                break;
            case 4:
                pDialog.setMessage(getString(R.string.LoadingUserInfo));
                break;
        }
        pDialog.show();
    }
}
