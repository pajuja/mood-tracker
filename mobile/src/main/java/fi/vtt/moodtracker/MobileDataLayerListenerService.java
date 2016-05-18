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
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import fi.vtt.climblib.Path;

/**
 * Listens to DataItems
 */
public class MobileDataLayerListenerService extends WearableListenerService {

    private static final String TAG = "MobileDataLayerListener";

    /** max time between the location is fetched and the time of the climb.
     * Above this, do not save location.
     * It means that the wearable has been far from the phone for a long time and location is probably not relevant.
     * */
    private static final long MAX_TIME_FOR_LOCATION = 1000 * 60 * 60;

    private GoogleApiClient mGoogleApiClient;

    private Firebase mFirebaseRef;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

        mFirebaseRef = new Firebase(getResources().getString(R.string.firebase_url));
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        String gradeSystemType = sharedPref.getString(Path.PREF_GRAD_SYSTEM_TYPE, GradeList.SYSTEM_DEFAULT);

        Location lastLocation = null;

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            lastLocation  = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }

//        TimeZone tz = TimeZone.getTimeZone("UTC");
//        java.text.DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
//        df.setTimeZone(tz);

        for (DataEvent event : events) {
            Log.d(TAG, "Event: " + event.getDataItem().toString());
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();

            if (path.startsWith(Path.MOOD)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String qId = dataMapItem.getDataMap().getString(Path.QUESTIONNAIRE_ID_KEY);
                Date date = new Date(dataMapItem.getDataMap().getLong(Path.MOOD_DATE_KEY));
//                String ISODateUTC = df.format(date);
                String ISODate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(date);
                String q = dataMapItem.getDataMap().getString(Path.QUESTION_KEY);
                String chTitle = dataMapItem.getDataMap().getString(Path.CHOICE_TITLE_KEY);
                int chValue = dataMapItem.getDataMap().getInt(Path.CHOICE_VALUE_KEY);
                int heartbeat = dataMapItem.getDataMap().getInt(Path.HEARTBEAT_KEY);

                // check that climb date and location date are not too far, do not save location if so.
                double latitude = 0;
                double longitude = 0;
                if(lastLocation != null && Math.abs(lastLocation.getTime() - date.getTime()) < MAX_TIME_FOR_LOCATION) {
                    latitude = lastLocation.getLatitude();
                    longitude = lastLocation.getLongitude();
                }

//                if (chValue != null) {
//                    Log.d(TAG, "New Climb, grade : " + routeGradeLabel + " " + climbDate.toString());

                AuthData authData = mFirebaseRef.getAuth();
                String uName = (String) authData.getProviderData().get("displayName");
                if (authData != null) {
                    Mood mood = new Mood(qId, date, ISODate, q, chTitle, chValue, heartbeat,
                            latitude, longitude);
                    mFirebaseRef.child("users")
                            .child(authData.getUid())
                            .child("moods:"+qId)
                            .push().setValue(mood);
                    mFirebaseRef.child("users")
                            .child(authData.getUid())
                            .child("fullname")
                            .setValue(uName);
                }
            }
        }
    }
}
