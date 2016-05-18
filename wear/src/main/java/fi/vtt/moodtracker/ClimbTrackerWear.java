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
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import fi.vtt.climblib.Choice;
import fi.vtt.climblib.Path;
import fi.vtt.climblib.Questionnaire;

public class ClimbTrackerWear extends WearableActivity implements WearableListView.ClickListener,
        SensorEventListener {

    private Questionnaire questionnaire;
    private ArrayList<Choice> choices;
    private WearableListView listView;
    private TextView header;
//    private CountDownTimer countDownTimer;
    private int qListPosition = 0;
    private boolean qListLastElem = false;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private SensorManager mSensorManager;
    public int heartBeat;

    public static final String QUESTIONNAIRE_ID = "questionnaireId";
    public static final String QUESTION = "question";
    public static final String CHOICE_TITLE = "choiceTitle";
    public static final String CHOICE_VALUE = "choiceValue";
    public static final String HEARTBEAT = "heartbeat";
    public static final String QL_LAST_ELEMENT = "lastElement";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_climb_tracker_wear);
        setAmbientEnabled();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean(Path.UISWITCH, false)) {
            Intent in = new Intent(this, MoodButtons.class);
            startActivity(in);
            finish();
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, mHeartRateSensor,  SensorManager.SENSOR_DELAY_NORMAL);

        String json = sharedPref.getString(Path.JSON, "");
        Gson gson = new GsonBuilder().serializeNulls().create();
        questionnaire = gson.fromJson(json, Questionnaire.class);

        header = (TextView) findViewById(R.id.header);
        header.bringToFront();
        listView = (WearableListView) findViewById(R.id.list);
        listView.setClickListener(this);
        listView.addOnScrollListener(new WearableListView.OnScrollListener() {
            @Override
            public void onScroll(int i) {
                header.setY(header.getY() - i);
            }
            @Override
            public void onAbsoluteScrollChange(int i) {
            }
            @Override
            public void onScrollStateChanged(int i) {
            }
            @Override
            public void onCentralPositionChanged(int i) {
            }
        });
        nextQuestion();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        heartBeat = Math.round(event.values[0]);
//        Log.d("HEARTBEAT", Integer.toString(heartBeat));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        long[] vibrationPattern = {0, 600};
        final int indexInPatternToRepeat = -1; //-1 - don't repeat
        vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
    }

    public void nextQuestion() {

        if(qListPosition < questionnaire.getQuestions().size()) {
            if (qListPosition == 0)
                vibrate();
            qListLastElem = false;
//            listView.setVisibility(ListView.VISIBLE);

            choices = new ArrayList<Choice>(questionnaire.getQuestions().get(qListPosition).getChoices());
            listView.setAdapter(new MoodAdapter(this, choices));
            header.setText(questionnaire.getQuestions().get(qListPosition).getQuestion());
            header.setY(0); //Fucking listView scrolling finally works

            qListPosition++;

            if(qListPosition >= questionnaire.getQuestions().size()) {
                qListLastElem = true;
            }
//            setVibrateTimer();
        }
        else {
//            listView.setVisibility(ListView.INVISIBLE);
            qListPosition = 0;

            final long millis = questionnaire.getInterval() * 60 * 1000;
            long msAlarm = SystemClock.elapsedRealtime() + millis;

            Intent myIntent = new Intent(ClimbTrackerWear.this, MoodAlarmReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(ClimbTrackerWear.this, 0, myIntent, 0);

            alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, msAlarm, alarmIntent);
            finish();
        }
    }

/*    public void setVibrateTimer() {
        //cancel the old countDownTimer
        if(countDownTimer != null)
            countDownTimer.cancel();
        final long ms = 60000;
        final long msTick = ms + 1000;

        countDownTimer = new CountDownTimer(ms, msTick) {
            @Override
            public void onFinish() {
                onExitAmbient();
                vibrate();
//                    nextQuestion();
                setVibrateTimer();
            }
            @Override
            public void onTick(long millisUntilFinished) {
            }
        };
        countDownTimer.start();
    }*/

    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

//        if(countDownTimer != null)
//            countDownTimer.cancel();

        Intent intent = new Intent(this, ClimbConfirmation.class);
        intent.putExtra(QUESTIONNAIRE_ID, questionnaire.getId());
        intent.putExtra(QUESTION, header.getText());
        intent.putExtra(CHOICE_TITLE, choices.get((int) viewHolder.itemView.getTag()).getTitle());
        intent.putExtra(CHOICE_VALUE, choices.get((int) viewHolder.itemView.getTag()).getValue());
        intent.putExtra(HEARTBEAT, heartBeat);
        intent.putExtra(QL_LAST_ELEMENT, qListLastElem);

//        startActivity(intent);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                listView.scrollToPosition(0);
                nextQuestion();
            }
/*            if (resultCode == Activity.RESULT_CANCELED) {
                setVibrateTimer();
            }*/
        }
    }//onActivityResult

    @Override
    public void onTopEmptyRegionClick() {}

    private static final class MoodAdapter extends WearableListView.Adapter {
        private final LayoutInflater mInflater;
        private ArrayList<Choice> mChoices;

        private MoodAdapter(Context context, ArrayList<Choice> ch) {
            mInflater = LayoutInflater.from(context);
            mChoices = ch;
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new WearableListView.ViewHolder(
                    mInflater.inflate(R.layout.route_grade_list_item_layout, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ImageView circle = (ImageView) holder.itemView.findViewById(R.id.circle);
            TextView emoji = (TextView) holder.itemView.findViewById(R.id.listemoji);
            if (mChoices.get(position).getEmoji() != null &&
                    !mChoices.get(position).getEmoji().isEmpty()) {
                circle.setVisibility(View.GONE);
                emoji.setVisibility(View.VISIBLE);
                emoji.setText(mChoices.get(position).getEmoji());
            }
            else {
                circle.setVisibility(View.VISIBLE);
                emoji.setVisibility(View.GONE);
            }

            TextView view = (TextView) holder.itemView.findViewById(R.id.name);
            view.setText(mChoices.get(position).getTitle());
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mChoices.size();
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
//        BoxInsetLayout bil = (BoxInsetLayout) findViewById(R.id.bil);
//        bil.setBackgroundColor(Color.BLACK);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.llist);
        rl.setBackgroundColor(Color.BLACK);
//        TextView n = (TextView) findViewById(R.id.name);
//        n.setTextColor(Color.WHITE);
        header.setTextColor(Color.WHITE);
        header.setBackgroundColor(Color.BLACK);
        header.getPaint().setAntiAlias(false);
}
    @Override
    public void onExitAmbient(){
//        BoxInsetLayout bil = (BoxInsetLayout) findViewById(R.id.bil);
//        bil.setBackgroundColor(Color.WHITE);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.llist);
        rl.setBackgroundColor(Color.WHITE);
//        TextView n = (TextView) findViewById(R.id.name);
//        n.setTextColor(Color.BLACK);
        header.setTextColor(Color.BLACK);
        header.setBackgroundColor(Color.WHITE);
        header.getPaint().setAntiAlias(true);
        super.onExitAmbient();
    }
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        vibrate();
//        onExitAmbient();
    }
    @Override
    public void onDestroy() {
//        if(countDownTimer != null)
//            countDownTimer.cancel();
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }
}
