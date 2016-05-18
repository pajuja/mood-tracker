package fi.vtt.moodtracker;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import fi.vtt.climblib.Choice;
import fi.vtt.climblib.Path;
import fi.vtt.climblib.Questionnaire;

public class MoodButtons extends WearableActivity implements View.OnClickListener,
        SensorEventListener {

    private RelativeLayout rl;
    private TextView questionTV;
    private Questionnaire questionnaire;
    private ArrayList<Choice> choices;
    private int qListPosition = 0;
    private boolean qListLastElem = false;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    private SensorManager mSensorManager;
    private int heartBeat;

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
        setContentView(R.layout.activity_mood_buttons); //Not in use
        setAmbientEnabled();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String json = sharedPref.getString(Path.JSON, "");
        Gson gson = new GsonBuilder().serializeNulls().create();
        questionnaire = gson.fromJson(json, Questionnaire.class);
        Log.d("QUEST", questionnaire.getQuestions().get(0).getType());

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

            choices = new ArrayList<Choice>(questionnaire.getQuestions().get(qListPosition).getChoices());
//ButtonUI starts ******** Thanks for the algorithm Konstantino Sparakis:
//http://stackoverflow.com/questions/22700007/a-circle-of-textviews-in-android?lq=1
            rl = new RelativeLayout(this);
            setContentView(rl);
            // Change the radius to change size of cirlce
            double radius = 140;
            // Change these to control the x and y position of the center of the circle
            // For more screen friendly code changing this to margins might be more efficient
            double cx = 150, cy = 10;
            double angleStart = 0;
            double angleMax = 0;
            double angleIncr = 45;

            if (questionnaire.getQuestions().get(qListPosition).getType().equals("choice_1_5")) {
                angleStart = -180;
                angleMax = -405;
            } else if (questionnaire.getQuestions().get(qListPosition).getType().equals("choice_1_7")) {
                angleStart = -135;
                angleMax = -450;
            }
            int cListPosition = 0;
            for (double angle = angleStart; angle > angleMax; angle -= angleIncr) {
                double radAngle = Math.toRadians(angle);
                double x = (Math.cos(radAngle)) * radius + cx;
                double y = (1 - Math.sin(radAngle)) * radius + cy;

                TextView textView = new TextView(this);
                if (choices.get(cListPosition).getEmoji() == null |
                        choices.get(cListPosition).getEmoji().equals(""))
                    textView.setText(choices.get(cListPosition).getTitle());
                else
                    textView.setText(choices.get(cListPosition).getEmoji());
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
//                textView.setText(Double.toString(angle));
                textView.setTag(cListPosition);
                textView.setOnClickListener(this);

                //the 150 is the width of the actual text view and the 50 is the height
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(120, 120);
                //the margins control where each of textview is placed
                lp.leftMargin = (int) x;
                lp.topMargin = (int) y;

                rl.addView(textView, lp);
                cListPosition++;
            }
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.CENTER_VERTICAL);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
            questionTV = new TextView(this);
            questionTV.setText(questionnaire.getQuestions().get(qListPosition).getQuestion());
            questionTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            questionTV.setPadding(10, 10, 10, 10);
            questionTV.setWidth(200);
            rl.addView(questionTV, lp);
//ButtonUI ends ****************************************************
            qListPosition++;

            if(qListPosition >= questionnaire.getQuestions().size()) {
                qListLastElem = true;
            }
        }
        else {
            qListPosition = 0;

            final long millis = questionnaire.getInterval() * 60 * 1000;
            long msAlarm = SystemClock.elapsedRealtime() + millis;

            Intent myIntent = new Intent(MoodButtons.this, MoodAlarmReceiver.class);
            alarmIntent = PendingIntent.getBroadcast(MoodButtons.this, 0, myIntent, 0);

            alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
            alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, msAlarm, alarmIntent);
            finish();
        }
    }
    @Override
    public void onClick(View view) {

        Intent intent = new Intent(this, ClimbConfirmation.class);
        intent.putExtra(QUESTIONNAIRE_ID, questionnaire.getId());
        intent.putExtra(QUESTION, questionTV.getText());
        intent.putExtra(CHOICE_TITLE, choices.get((int) view.getTag()).getTitle());
        intent.putExtra(CHOICE_VALUE, choices.get((int) view.getTag()).getValue());
        intent.putExtra(HEARTBEAT, heartBeat);
        intent.putExtra(QL_LAST_ELEMENT, qListLastElem);

//        startActivity(intent);
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if(resultCode == Activity.RESULT_OK){
                nextQuestion();
            }
/*            if (resultCode == Activity.RESULT_CANCELED) {
                setVibrateTimer();
            }*/
        }
    }//onActivityResult

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        vibrate();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            rl.setBackgroundColor(Color.BLACK);
            questionTV.setTextColor(Color.WHITE);
        } else {
            rl.setBackground(null);
            questionTV.setTextColor(Color.BLACK);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
