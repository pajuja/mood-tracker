package fi.vtt.moodtracker;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by ttepan on 18.4.2016.
 */
public class MoodAlarmService extends Service {

    @Override
    public IBinder onBind(Intent arg0)
    {
        // TODO Auto-generated method stub
        return null;
    }

//    @SuppressWarnings("static-access")

    @Override
    public void onCreate()
    {
        // TODO Auto-generated method stub
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        Intent myIntent = new Intent(this, ClimbTrackerWear.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(myIntent);
        return START_NOT_STICKY;
    }


    @Override
    public void onDestroy()
    {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}
