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

import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

import fi.vtt.climblib.Path;

/**
 * Listens to DataItems
 */
public class WearDataLayerListenerService extends WearableListenerService {

    private static final String TAG = "WearDataLayerListener";

    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            Log.d(TAG, "Event: " + event.getDataItem().toString());
            Uri uri = event.getDataItem().getUri();
            String path = uri.getPath();

/*            if (path.equals(Path.GRADE_SYSTEM)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String gradeSystem = dataMapItem.getDataMap().getString(Path.GRADE_SYSTEM_KEY);
                if (gradeSystem != null) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Path.PREF_GRAD_SYSTEM_TYPE, gradeSystem);
                    editor.commit();
                }
            }
            else*/ if (path.equals(Path.JSON)) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                String json = dataMapItem.getDataMap().getString(Path.JSON_KEY);
                boolean uiswitch = dataMapItem.getDataMap().getBoolean(Path.UISWITCH_KEY);
                Log.d("JSON", json);
                Log.d("SWITCH", String.valueOf(uiswitch));

                if (json != null) {
                    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(Path.JSON, json);
                    editor.putBoolean(Path.UISWITCH, uiswitch);
                    editor.commit();
                }
            }
        }
    }

}
