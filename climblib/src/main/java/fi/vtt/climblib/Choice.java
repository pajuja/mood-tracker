package fi.vtt.climblib;

/**
 * Created by ttepan on 7.4.2016.
 */
public class Choice {

    private String emoji;
    private String title;
    private int value;

    /**
     * No args constructor for use in serialization
     *
     */
    public Choice() {
    }

    /**
     *
     * @param emoji
     * @param title
     * @param value
     */
    public Choice(String emoji, String title, int value) {
        this.emoji = emoji;
        this.title = title;
        this.value = value;
    }

    /**
     *
     * @return
     * The emoji
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     *
     * @param emoji
     * The emoji
     */
    public void setEmoji(String emoji) {
        this.emoji = emoji;
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

    /**
     *
     * @return
     * The value
     */
    public int getValue() {
        return value;
    }

    /**
     *
     * @param value
     * The value
     */
    public void setValue(int value) {
        this.value = value;
    }

}
