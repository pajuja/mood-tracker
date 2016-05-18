package fi.vtt.moodtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by ttepan on 18.4.2016.
 */
public class MoodAlarmReceiver extends BroadcastReceiver
{

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent service1 = new Intent(context, MoodAlarmService.class);
        context.startService(service1);
        Log.i("App", "called receiver method");
    }

}
