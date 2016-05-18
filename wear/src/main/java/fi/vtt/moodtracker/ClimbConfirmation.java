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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Date;
import java.util.UUID;

import fi.vtt.climblib.Path;

public class ClimbConfirmation extends Activity implements
        DelayedConfirmationView.DelayedConfirmationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ClimbConfirmation";

    private GoogleApiClient mGoogleApiClient;

//    private String routeGradeLabelToSave;
    private String qId;
    private String q;
    private String chTitle;
    private int chValue;
    private int heartbeat;
    private boolean lastElem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mGoogleApiClient.connect();

        DelayedConfirmationView mDelayedView = (DelayedConfirmationView) findViewById(R.id.delayed_confirm);
        mDelayedView.setListener(this);

//        routeGradeLabelToSave = getIntent().getStringExtra(ClimbTrackerWear.EXTRA_ROUTE_GRADE_LABEL);
        qId = getIntent().getStringExtra(ClimbTrackerWear.QUESTIONNAIRE_ID);
        q = getIntent().getStringExtra(ClimbTrackerWear.QUESTION);
        chTitle = getIntent().getStringExtra(ClimbTrackerWear.CHOICE_TITLE);
        chValue = getIntent().getIntExtra(ClimbTrackerWear.CHOICE_VALUE, 0);
        heartbeat = getIntent().getIntExtra(ClimbTrackerWear.HEARTBEAT, 0);
        lastElem = getIntent().getBooleanExtra(ClimbTrackerWear.QL_LAST_ELEMENT, false);

        // change the recap text
        TextView climbRecapText = (TextView) findViewById(R.id.climb_recap);
//        String recapString = getString(R.string.climb_recap, routeGradeLabelToSave);
        String recapString = getString(R.string.climb_recap, Integer.toString(chValue));
        climbRecapText.setText(recapString);

        // Four seconds to cancel the action
        mDelayedView.setTotalTimeMs(2000);
        // Start the timer
        mDelayedView.start();
    }

    @Override
    public void onTimerFinished(View view) {
        // save only if user hasn't canceled
        if(!this.isFinishing()) {
            saveClimb();

            if(lastElem) {
//                String savedString = getString(R.string.climb_saved, routeGradeLabelToSave);
                Intent intent = new Intent(this, ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.climb_saved));
                startActivity(intent);
            }
        }
        Intent returnIntent = new Intent();
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    /** User canceled by clicking the "X" button */
    @Override
    public void onTimerSelected(View view) {
        Toast.makeText(this, R.string.climb_canceled, Toast.LENGTH_SHORT).show();
//        Intent returnIntent = new Intent();
//        setResult(Activity.RESULT_CANCELED, returnIntent);
        finish();
    }

    private void saveClimb() {
        // Create a unique identifier for this data item
//        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Path.CLIMB + '/' + UUID.randomUUID());
//        putDataMapReq.getDataMap().putString(Path.ROUTE_GRADE_LABEL_KEY, routeGradeLabelToSave);
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(Path.MOOD + '/' + UUID.randomUUID());
        putDataMapReq.getDataMap().putString(Path.QUESTIONNAIRE_ID_KEY, qId);
        putDataMapReq.getDataMap().putLong(Path.MOOD_DATE_KEY, (new Date().getTime()));
        putDataMapReq.getDataMap().putString(Path.QUESTION_KEY, q);
        putDataMapReq.getDataMap().putString(Path.CHOICE_TITLE_KEY, chTitle);
        putDataMapReq.getDataMap().putInt(Path.CHOICE_VALUE_KEY, chValue);
        putDataMapReq.getDataMap().putInt(Path.HEARTBEAT_KEY, heartbeat);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected(): Successfully connected to Google API client");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection to Google API client has failed");
    }

    @Override
    public void onConnectionSuspended(int i) {}

}