package fi.vtt.climblib;

/**
 * Created by ttepan on 7.4.2016.
 */

import java.util.ArrayList;
import java.util.List;

public class Questionnaire {

    private String id;
    private String description;
    private List<Question> questions = new ArrayList<Question>();
    private int interval;
    private String title;

    /**
     * No args constructor for use in serialization
     *
     */
    public Questionnaire() {
    }

    /**
     *
     * @param title
     * @param interval
     * @param description
     * @param questions
     */
    public Questionnaire(String id, String description, List<Question> questions, int interval, String title) {
        this.id = id;
        this.description = description;
        this.questions = questions;
        this.interval = interval;
        this.title = title;
    }

    /**
     *
     * @return
     * The Id
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @param id
     * The _id
     */
    public void setId(String id) {
        this.id = id;
    }
    /**
     *
     * @return
     * The description
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     * @param description
     * The description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     * @return
     * The questions
     */
    public List<Question> getQuestions() {
        return questions;
    }

    /**
     *
     * @param questions
     * The questions
     */
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    /**
     *
     * @return
     * The interval
     */
    public int getInterval() {
        return interval;
    }

    /**
     *
     * @param interval
     * The interval
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     *
     * @return
     * The title
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     * @param title
     * The title
     */
    public void setTitle(String title) {
        this.title = title;
    }

}

