package fi.vtt.climblib;

/**
 * Created by ttepan on 7.4.2016.
 */

import java.util.ArrayList;
import java.util.List;

public class Question {

    private List<Choice> choices = new ArrayList<Choice>();
    private String name;
    private String question;
    private String type;

    /**
     * No args constructor for use in serialization
     *
     */
    public Question() {
    }

    /**
     *
     * @param name
     * @param choices
     * @param question
     * @param type
     */
    public Question(List<Choice> choices, String name, String question, String type) {
        this.choices = choices;
        this.name = name;
        this.question = question;
        this.type = type;
    }

    /**
     *
     * @return
     * The choices
     */
    public List<Choice> getChoices() {
        return choices;
    }

    /**
     *
     * @param choices
     * The choices
     */
    public void setChoices(List<Choice> choices) {
        this.choices = choices;
    }

    /**
     *
     * @return
     * The name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The question
     */
    public String getQuestion() {
        return question;
    }

    /**
     *
     * @param question
     * The question
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     *
     * @return
     * The type
     */
    public String getType() {
        return type;
    }

    /**
     *
     * @param type
     * The type
     */
    public void setType(String type) {
        this.type = type;
    }

}
