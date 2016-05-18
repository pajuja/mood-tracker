/*
Copyright 2014 Google Inc. All rights reserved.
        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/
package fi.vtt.moodtracker;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.AuthData;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import fi.vtt.climblib.Path;

//TODO : Clean up and documentate
/*
 * An activity representing a list of ClimbSessions. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link ClimbSessionDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link ClimbSessionListFragment} and the item details
 * (if present) is a {@link ClimbSessionDetailFragment}.
 * <p/>
 * This activity also implements the required
 * {@link ClimbSessionListFragment.Callbacks} interface
 * to listen for item selections.
 */
//public class ClimbTrackerActivity extends AppCompatActivity
//        implements ClimbSessionListFragment.Callbacks, GoogleApiClient.ConnectionCallbacks,
//        GoogleApiClient.OnConnectionFailedListener,
//        SharedPreferences.OnSharedPreferenceChangeListener,
//        GradePickerFragment.GradeDialogListener {
public class ClimbTrackerActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = ClimbTrackerActivity.class.getSimpleName();

    private static final int PERMISSION_UPFRONT_REQUEST = 123;

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
//    private boolean mTwoPane;

    /* *************************************
     *              GENERAL                *
     ***************************************/
    /* TextView that is used to display information about the logged in user */
    private TextView mLoggedInStatusTextView;

    /* A dialog that is presented until the Firebase authentication finished. */
    private ProgressDialog mAuthProgressDialog;

    /* A reference to the Firebase */
    private Firebase mFirebaseRef;

    /* Data from the authenticated user */
    private AuthData mAuthData;

    /* Listener for Firebase session changes */
    private Firebase.AuthStateListener mAuthStateListener;

    /* *************************************
     *              GOOGLE                 *
     ***************************************/
    /* Request code used to invoke sign in user interactions for Google+ */
    public static final int RC_GOOGLE_LOGIN = 1;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* A flag indicating that a PendingIntent is in progress and prevents us from starting further intents. */
    private boolean mGoogleIntentInProgress;

    /* Track whether the sign-in button has been clicked so that we know to resolve all issues preventing sign-in
     * without waiting. */
    private boolean mGoogleLoginClicked;

    /* Store the connection result from onConnectionFailed callbacks so that we can resolve them when the user clicks
     * sign-in. */
    private ConnectionResult mGoogleConnectionResult;

    /* The login button for Google */
    private SignInButton mGoogleLoginButton;

    /** Analytics tracker COMMENTED OUT in ClimbTrackerApplication, ClimbSessionDetailFragment too*/
//    private Tracker mTracker;

    private GoogleApiClient mGoogleWearApiClient;
    private String configJSON;
    private String configJSONBuffer;
    private boolean needPermission;
    private TextView jsonScreen;
    private Switch uiswitch;
    private boolean uisState = false;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_climb_tracker);
        setContentView(R.layout.activity_mood);
        needPermission = checkAndRequestPermissions();

        // Analytics.
/*        ClimbTrackerApplication application = (ClimbTrackerApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // track pageview
        mTracker.setScreenName(LOG_TAG);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());*/

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        uiswitch = (Switch) findViewById(R.id.uiswitch);
        uiswitch.setChecked(sharedPref.getBoolean(Path.UISWITCH, uisState));
        uiswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    uisState = true;
                    Toast.makeText(ClimbTrackerActivity.this, "Wearable UI uses buttons",
                            Toast.LENGTH_SHORT).show();
                } else {
                    uisState = false;
                    Toast.makeText(ClimbTrackerActivity.this, "Wearable UI uses listview",
                            Toast.LENGTH_SHORT).show();
                }
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean(Path.UISWITCH, uisState);
                editor.commit();
                if (configJSONBuffer.length() > 0)
                    sendJSONToWear();
            }
        });

        /* *************************************
         *               GOOGLE                *
         ***************************************/
        /* Load the Google login button */
        mGoogleLoginButton = (SignInButton) findViewById(R.id.login_with_google);
        mGoogleLoginButton.setSize(1);
        mGoogleLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleLoginClicked = true;
                if (!mGoogleApiClient.isConnecting()) {
                    if (mGoogleConnectionResult != null) {
                        resolveSignInError();
                    } else if (mGoogleApiClient.isConnected()) {
                        getGoogleOAuthTokenAndLogin();
                    } else {
                    /* connect API now */
                        Log.d(TAG, "Trying to connect to Google API");
                        mGoogleApiClient.connect();
                    }
                }
            }
        });
        /* Setup the Google API object to allow Google+ logins */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();

        mGoogleWearApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();
        mGoogleWearApiClient.connect();

        /* *************************************
         *               GENERAL               *
         ***************************************/
        mLoggedInStatusTextView = (TextView) findViewById(R.id.login_status);
        jsonScreen = (TextView) findViewById(R.id.json_screen);

        /* Create the Firebase ref that is used for all authentication with Firebase */
        mFirebaseRef = new Firebase(getResources().getString(R.string.firebase_url)  + getResources().getString(R.string.fb_current));

        /* Setup the progress dialog that is displayed later when authenticating with Firebase */
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle("Loading");
        mAuthProgressDialog.setMessage("Authenticating with Firebase...");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();

        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                mAuthProgressDialog.hide();
                setAuthenticatedUser(authData);
                if (authData != null) getConfigJSON();
                else jsonScreen.setText("");
            }
        };
        /* Check if the user is authenticated with Firebase already. If this is the case we can set the authenticated
         * user and hide hide any login buttons */
        mFirebaseRef.addAuthStateListener(mAuthStateListener);

 /*       if (findViewById(R.id.climbsession_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            ((ClimbSessionListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.climbsession_list))
                    .setActivateOnItemClick(true);
        }*/

//        getConfigJSON();
//        initFAB();
//        initPreferences();


    }
    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
    private void getConfigJSON(){
        // Get a reference to our posts
//        Firebase ref = new Firebase(getResources().getString(R.string.firebase_url) + "/foor");
        // Attach an listener to read the data at our posts reference
//        mFirebaseRef.orderByChild("questions").addValueEventListener(new ValueEventListener() {
        mFirebaseRef.orderByPriority().addValueEventListener(new ValueEventListener() {
            //                mFirebaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
//TODO: Lost my nerves with GSON TypeAdapters, Maps and Firebase.
//TODO: Repair ugly parsing by StringBuilder
                System.out.println(snapshot.getValue());
                StringBuilder sb = new StringBuilder();
                try {

                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
/*                Questionnaire questionnaire = new Questionnaire();
//                questionnaire.setId((String) map.get("id"));
                questionnaire.setDescription((String) map.get("description"));
                questionnaire.setInterval(safeLongToInt((Long) map.get("interval")));
                questionnaire.setTitle((String) map.get("title"));

                System.out.println(questionnaire.getId());
                System.out.println(questionnaire.getDescription());
                System.out.println(Integer.toString(questionnaire.getInterval()));
                System.out.println(questionnaire.getTitle());*/

                    sb.append("{\"id\":\"");
                    sb.append((String) map.get("id"));
                    sb.append("\",\"description\":\"");
//                            sb.append("{\"description\":\"");
                    sb.append((String) map.get("description"));
                    sb.append("\",\"interval\":");
//                            sb.append(safeLongToInt((Long) map.get("interval")));
                    sb.append(Integer.valueOf((String) map.get("interval")));
                    sb.append(",\"title\":\"");
                    sb.append((String) map.get("title"));
                    sb.append("\",\"questions\":[");

                    for (DataSnapshot child : snapshot.getChildren()) {

//                                            List<Question> questions = new ArrayList<Question>();

                        for (DataSnapshot child2 : child.getChildren()) {
                            Map<Object, Object> map2 = (Map<Object, Object>) child2.getValue();
    /*                        Question question = new Question();
                            question.setQuestion((String) map2.get("question"));
                            question.setType((String) map2.get("type"));*/

                            sb.append("{\"question\":\"");
                            sb.append((String) map2.get("question"));
                            sb.append("\",\"type\":\"");
                            sb.append((String) map2.get("type"));
                            sb.append("\",\"choices\":[");

                            for (DataSnapshot child3 : child2.getChildren()) {

//                                List<Choice> choices = new ArrayList<Choice>();

                                for (DataSnapshot child4 : child3.getChildren()) {

                                    Map<Object, Object> map3 = (Map<Object, Object>) child4.getValue();
    /*                                Choice choice = new Choice();
                                    choice.setEmoji((String) map3.get("emoji"));
                                    choice.setTitle((String) map3.get("title"));
                                    choice.setValue((safeLongToInt((Long) map3.get("value"))));
                                    choices.add(choice);*/

                                    sb.append("{\"emoji\":\"");
                                    sb.append((String) map3.get("emoji"));
                                    sb.append("\",\"title\":\"");
                                    sb.append((String) map3.get("title"));
                                    sb.append("\",\"value\":");
                                    sb.append(safeLongToInt((Long) map3.get("value")));
                                    sb.append("},");
                                }
                                //                            question.setChoices(choices);
                            }
                            sb.deleteCharAt(sb.length() - 1);
                            sb.append("]},");
                            //                        questions.add(question);
                        }
                        //                    questionnaire.setQuestions(questions);
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    sb.append("]}");

                    System.out.println(sb.toString());
//                Gson gson = new Gson();
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();

//                Gson gson = new GsonBuilder()
//                        .registerTypeAdapter(Questionnaire.class, )
//                        .registerTypeAdapter(Id.class, new IdTypeAdapter())
//                        .enableComplexMapKeySerialization()
//                        .serializeNulls()
//                        .setDateFormat(DateFormat.LONG)
//                        .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
//                        .setPrettyPrinting()
//                        .setVersion(1.0)
//                        .create();

//                java.lang.reflect.Type type = new TypeToken <ArrayList<Question>>() {}.getType();
//                configJSON = gson.toJson(questionnaire, Questionnaire.class);
                    configJSON = gson.toJson(snapshot.getValue());
//                System.out.println(configJSON);
//                sendJSONToWear(configJSON);
                    configJSONBuffer = sb.toString();
                    sendJSONToWear();
                    jsonScreen.setMovementMethod(new ScrollingMovementMethod());
                    jsonScreen.setText(configJSON);
                } catch (Exception e) {
                    e.printStackTrace();
                    sb.setLength(0);
                    sb.append("Check Firebase content");
                    jsonScreen.setText(sb.toString());
                    Toast.makeText(getApplicationContext(), "Check Firebase content", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                System.out.println("The read failed: " + firebaseError.getMessage());
            }
        });
    }

//    private void initPreferences() {
//        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
//        PreferenceManager.getDefaultSharedPreferences(this)
//                .registerOnSharedPreferenceChangeListener(this);
//
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        String gradeSystemTypePref = sharedPref.getString(Path.PREF_GRAD_SYSTEM_TYPE, GradeList.SYSTEM_DEFAULT);
//        sendGradeSystemToWear(gradeSystemTypePref);
//    }

//    private void initFAB() {
//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                DialogFragment gradePickerFragment = new GradePickerFragment();
//                gradePickerFragment.show(getSupportFragmentManager(), "gradePicker");
//            }
//        });
//    }

    /**
     * Check for required and optional permissions and request them if needed
     * @return true if there are permissions to ask, false otherwise
     */
    private boolean checkAndRequestPermissions() {
        ArrayList<String> permissionsToAsk = new ArrayList<String>();

        // Check for account permission, request it if not granted.
        // it seems that we cannot simply use Manifest.permission.USE_CREDENTIALS ?
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToAsk.add(Manifest.permission.GET_ACCOUNTS);
        }

        // Request for location permission if not already granted and not already asked
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // We are cool with this one, we do not ask if the user already declined
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                permissionsToAsk.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }

        if(!permissionsToAsk.isEmpty()) {
            String[] permissionsToAskArray = permissionsToAsk.toArray(new String[permissionsToAsk.size()]);
            ActivityCompat.requestPermissions(this, permissionsToAskArray, PERMISSION_UPFRONT_REQUEST);
            return true;
        } else {
            return false;
        }
    }
    //
//    private void sendGradeSystemToWear(String system) {
//        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Path.GRADE_SYSTEM);
//        putDataMapReq.getDataMap().putString(Path.GRADE_SYSTEM_KEY, system);
//        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
//        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
//    }
    private void sendJSONToWear() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Path.JSON);
        putDataMapReq.getDataMap().putString(Path.JSON_KEY, configJSONBuffer);
        putDataMapReq.getDataMap().putBoolean(Path.UISWITCH_KEY, uisState);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleWearApiClient, putDataReq);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if changing configurations, stop tracking firebase session.
        mFirebaseRef.removeAuthStateListener(mAuthStateListener);
    }

    /**
     * This method fires when any startActivityForResult finishes. The requestCode maps to
     * the value passed into startActivityForResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_LOGIN) {
            /* This was a request by the Google API */
            if (resultCode != RESULT_OK) {
                mGoogleLoginClicked = false;
            }
            mGoogleIntentInProgress = false;
            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* If a user is currently authenticated, display a logout menu */
        if (this.mAuthData != null) {
            getMenuInflater().inflate(R.menu.main_menu, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_disconnect) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Unauthenticate from Firebase and from providers where necessary.
     */
    private void logout() {
        if (this.mAuthData != null) {
            /* logout of Firebase */
            mFirebaseRef.unauth();
            /* Logout of the Framework. This step is optional, but ensures the user is not logged into
             * Google+ after logging out of Firebase. */
            if (this.mAuthData.getProvider().equals("google")) {
                /* Logout from Google+ */
                if (mGoogleApiClient.isConnected()) {
                    Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
                    mGoogleApiClient.disconnect();
                }
            }
            /* Update authenticated user and show login buttons */
            setAuthenticatedUser(null);
        }
    }

    /**
     * This method will attempt to authenticate a user to firebase given an oauth_token (and other
     * necessary parameters depending on the provider)
     */
    private void authWithFirebase(final String provider, Map<String, String> options) {
        if (options.containsKey("error")) {
            showErrorDialog(options.get("error"));
        } else {
            mAuthProgressDialog.show();
            // if the provider is not twitter, we just need to pass in the oauth_token
            mFirebaseRef.authWithOAuthToken(provider, options.get("oauth_token"), new AuthResultHandler(provider));
        }
    }

    /**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     */
    private void setAuthenticatedUser(AuthData authData) {
        if (authData != null) {
            /* Hide all the login buttons */
            mGoogleLoginButton.setVisibility(View.GONE);
            mLoggedInStatusTextView.setVisibility(View.VISIBLE);
            jsonScreen.setVisibility(View.VISIBLE);
            uiswitch.setVisibility(View.VISIBLE);
            /* show a provider specific status text */
            String name = null;
            if (authData.getProvider().equals("google")) {
                name = (String) authData.getProviderData().get("displayName");
                Log.d("GOGGLE", authData.getProviderData().toString());
            } else {
                Log.e(TAG, "Invalid provider: " + authData.getProvider());
            }
            if (name != null) {
                mLoggedInStatusTextView.setText("Logged in as " + name + " (" + authData.getProvider() + ")");
            }
        } else {
            /* No authenticated user show all the login buttons */
            mGoogleLoginButton.setVisibility(View.VISIBLE);
            mLoggedInStatusTextView.setVisibility(View.GONE);
            jsonScreen.setVisibility(View.GONE);
            uiswitch.setVisibility(View.GONE);
        }
        this.mAuthData = authData;
        /* invalidate options menu to hide/show the logout button */
        supportInvalidateOptionsMenu();
    }

    /**
     * Show errors to users
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final String provider;

        public AuthResultHandler(String provider) {
            this.provider = provider;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
            mAuthProgressDialog.hide();
            Log.i(TAG, provider + " auth successful");
            setAuthenticatedUser(authData);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            mAuthProgressDialog.hide();
            showErrorDialog(firebaseError.toString());
        }
    }



    /* ************************************
     *              GOOGLE                *
     **************************************
     */
    /* A helper method to resolve the current ConnectionResult error. */
    private void resolveSignInError() {
        if (mGoogleConnectionResult.hasResolution()) {
            try {
                mGoogleIntentInProgress = true;
                mGoogleConnectionResult.startResolutionForResult(this, RC_GOOGLE_LOGIN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    private void getGoogleOAuthTokenAndLogin() {
        mAuthProgressDialog.show();
        /* Get OAuth token in Background */
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;

            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PLUS_LOGIN);
                    token = GoogleAuthUtil.getToken(ClimbTrackerActivity.this, Plus.AccountApi.getAccountName(mGoogleApiClient), scope);
                } catch (IOException transientEx) {
                    /* Network or server error */
                    Log.e(TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(TAG, "Recoverable Google OAuth error: " + e.toString());
                    /* We probably need to ask for permissions, so start the intent if there is none pending */
                    if (!mGoogleIntentInProgress) {
                        mGoogleIntentInProgress = true;
                        Intent recover = e.getIntent();
                        startActivityForResult(recover, RC_GOOGLE_LOGIN);
                    }
                } catch (GoogleAuthException authEx) {
                    /* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. */
                    Log.e(TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }
                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                mGoogleLoginClicked = false;
                if (token != null) {
                    /* Successfully got OAuth token, now login with Google */
                    mFirebaseRef.authWithOAuthToken("google", token, new AuthResultHandler("google"));
                } else if (errorMessage != null) {
                    mAuthProgressDialog.hide();
                    showErrorDialog(errorMessage);
                }
            }
        };
        task.execute();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        /* Connected with Google API, use this to authenticate with Firebase */
        getGoogleOAuthTokenAndLogin();
    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mGoogleIntentInProgress) {
            /* Store the ConnectionResult so that we can use it later when the user clicks on the Google+ login button */
            mGoogleConnectionResult = result;

            if (mGoogleLoginClicked) {
                /* The user has already clicked login so we attempt to resolve all errors until the user is signed in,
                 * or they cancel. */
                resolveSignInError();
            } else {
                Log.e(TAG, result.toString());
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // ignore
    }


/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

// REMOVED from main_menu.xml
//        <item android:id="@+id/action_settings"
//        android:title="@string/action_settings" />
        *//*if (id == R.id.action_settings) {
            Intent intent = new Intent(ClimbTrackerActivity.this, SettingsActivity.class);
            startActivity(intent);
        } else*//* if (id == R.id.action_disconnect) {
            mFirebaseRef.unauth();
            mGoogleAuthApiClient.disconnect();
            mGoogleAuthApiClient.connect();
        }

        return super.onOptionsItemSelected(item);
    }

    *//**
     * Once a user is logged in, take the mAuthData provided from Firebase and "use" it.
     *//*
    private void setAuthenticatedUser(AuthData authData) {
        this.mAuthData = authData;
    }

    public void onConnectionFailed(ConnectionResult result) {
        if (!mIntentInProgress && result.hasResolution()) {
            try {
                mIntentInProgress = true;
                startIntentSenderForResult(result.getResolution().getIntentSender(), RC_SIGN_IN, null, 0, 0, 0);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mIntentInProgress = false;
                mGoogleAuthApiClient.connect();
            }
        }
    }

    protected void onActivityResult(int requestCode, int responseCode, Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            mIntentInProgress = false;
            if (!mGoogleAuthApiClient.isConnecting()) {
                mGoogleAuthApiClient.connect();
            }
        }
    }

    private void getGoogleOAuthTokenAndLogin() {
        *//* Get OAuth token in Background *//*
        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            String errorMessage = null;

            @Override
            protected String doInBackground(Void... params) {
                String token = null;

                try {
                    String scope = String.format("oauth2:%s", Scopes.PROFILE);
                    token = GoogleAuthUtil.getToken(ClimbTrackerActivity.this, Plus.AccountApi.getAccountName(mGoogleAuthApiClient), scope);
                } catch (IOException transientEx) {
                    *//* Network or server error *//*
                    Log.e(LOG_TAG, "Error authenticating with Google: " + transientEx);
                    errorMessage = "Network error: " + transientEx.getMessage();
                } catch (UserRecoverableAuthException e) {
                    Log.w(LOG_TAG, "Recoverable Google OAuth error: " + e.toString());
                    *//* We probably need to ask for permissions, so start the intent if there is none pending *//*
                    mGoogleAuthApiClient.connect();
                } catch (GoogleAuthException authEx) {
                    *//* The call is not ever expected to succeed assuming you have already verified that
                     * Google Play services is installed. *//*
                    Log.e(LOG_TAG, "Error authenticating with Google: " + authEx.getMessage(), authEx);
                    errorMessage = "Error authenticating with Google: " + authEx.getMessage();
                }

                return token;
            }

            @Override
            protected void onPostExecute(String token) {
                // Authenticate
                // see https://www.firebase.com/docs/android/guide/login/google.html
                mFirebaseRef.authWithOAuthToken("google", token, new Firebase.AuthResultHandler() {
                    @Override
                    public void onAuthenticated(AuthData authData) {
                        // the Google user is now authenticated with Firebase
                        setAuthenticatedUser(authData);
                    }

                    @Override
                    public void onAuthenticationError(FirebaseError firebaseError) {
                        Toast.makeText(ClimbTrackerActivity.this, firebaseError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        };
        task.execute();
    }

    public void onConnected(Bundle connectionHint) {
        getGoogleOAuthTokenAndLogin();
    }

    public void onConnectionSuspended(int cause) {
        mGoogleAuthApiClient.connect();
    }*/


/*    /**
     * Callback method from {@link ClimbSessionListFragment.Callbacks}
     * indicating that the item was selected.
     */
/*    @Override
    public void onItemSelected(ClimbSession session) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putLong(ClimbSessionDetailFragment.ARG_FIRST_CLIMB_TIME, session.getFirstClimbDate().getTime());
            arguments.putLong(ClimbSessionDetailFragment.ARG_LAST_CLIMB_TIME, session.getLastClimbDate().getTime());
            ClimbSessionDetailFragment fragment = new ClimbSessionDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.climbsession_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, ClimbSessionDetailActivity.class);
            detailIntent.putExtra(ClimbSessionDetailFragment.ARG_FIRST_CLIMB_TIME, session.getFirstClimbDate().getTime());
            detailIntent.putExtra(ClimbSessionDetailFragment.ARG_LAST_CLIMB_TIME, session.getLastClimbDate().getTime());
            startActivity(detailIntent);
        }
    }*/


/*    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Path.PREF_GRAD_SYSTEM_TYPE)) {
*//*            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("settings")
                    .setAction("changed-system")
                    .build());*//*

            String gradeSystemTypePref = sharedPreferences.getString(Path.PREF_GRAD_SYSTEM_TYPE, GradeList.SYSTEM_DEFAULT);
            sendGradeSystemToWear(gradeSystemTypePref);
        }
    }*/

/*    @Override
    public void onGradeSelected(String grade) {
*//*        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("action")
                .setAction("save-climb")
                .build());*//*

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String gradeSystemType = sharedPref.getString(Path.PREF_GRAD_SYSTEM_TYPE, GradeList.SYSTEM_DEFAULT);

        double latitude = 0;
        double longitude = 0;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(lastLocation != null) {
                latitude = lastLocation.getLatitude();
                longitude = lastLocation.getLongitude();
            }
        }

        Climb newClimb = new Climb(new Date(), grade, gradeSystemType, latitude, longitude);
        mNewClimbRef = mFirebaseRef.child("users")
                .child(mAuthData.getUid())
                .child("climbs")
                .push();
        mNewClimbRef.setValue(newClimb);

        final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.mainLayout);
        Snackbar.make(coordinatorLayout, R.string.climb_confirmation, Snackbar.LENGTH_LONG).setAction(R.string.climb_save_undo, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNewClimbRef.removeValue();
            }
        }).show();
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_UPFRONT_REQUEST: {
                // first check if we can login
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED) {

                    // if we can, check if we can see the location, if not, display a message.
                    for(int i = 0; i < grantResults.length; i++ ) {
                        if(permissions[i].equals(Manifest.permission.ACCESS_COARSE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_DENIED ) {
                            // no problem, just say we are not going to store location
                            final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.mainLayout);
                            Snackbar.make(coordinatorLayout, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show();
                        }
                    }
//                    mGoogleAuthApiClient.connect();
                } else {
                    // We need this permission to log in, explain and ask again
                    final CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.mainLayout);
                    Snackbar.make(coordinatorLayout, R.string.account_permission_denied, Snackbar.LENGTH_INDEFINITE)
                            .setAction(android.R.string.ok, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Request the permission again.
                                    ActivityCompat.requestPermissions(ClimbTrackerActivity.this,
                                            new String[]{Manifest.permission.GET_ACCOUNTS},
                                            PERMISSION_UPFRONT_REQUEST);
                                }
                            })
                            .show();
                }
            }

        }
    }
}
