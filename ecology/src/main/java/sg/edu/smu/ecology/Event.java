package sg.edu.smu.ecology;

import android.os.Bundle;

/**
 * Main event class.
 */
public class Event {

    private final String type;
    private final Bundle data;

    /**
     * @param type the event's type
     * @param data the event's data
     */
    public Event(String type, Bundle data){
        this.type = type;
        this.data = data;
    }

    /**
     * @return the event's type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the event's data
     */
    public Bundle getData() {
        return data;
    }
}
