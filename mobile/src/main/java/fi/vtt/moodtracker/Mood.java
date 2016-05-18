package fi.vtt.moodtracker;

/**
 * Created by TTEPAN on 11.4.2016.
 */
import java.util.Date;

public class Mood {
    private String questionnaireId;
    private Date date;
    private String ISODate;
    private String question;
    private String choiceTitle;
    private int choiceValue;
    private int heartbeat;
    private double latitude;
    private double longitude;

    // Required default constructor for Firebase object mapping
    @SuppressWarnings("unused")
    private Mood() {
    }

    public Mood(String questionnaireId, Date date, String ISODate, String question,
                String choiceTitle, int choiceValue, int heartbeat, double latitude, double longitude) {
        this.questionnaireId = questionnaireId;
        this.date = date;
        this.ISODate = ISODate;
        this.question = question;
        this.choiceTitle = choiceTitle;
        this.choiceValue = choiceValue;
        this.heartbeat = heartbeat;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    public String getQuestionnaireId() { return questionnaireId;  }
    public Date getDate() { return date; }
    public String getISODate() {
        return ISODate;
    }
    public String getQuestion() {
        return question;
    }
    public String getChoiceTitle() {
        return choiceTitle;
    }
    public int getChoiceValue() {return choiceValue;}
    public int getHeartbeat() {return heartbeat;}
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() { return longitude; }

}

