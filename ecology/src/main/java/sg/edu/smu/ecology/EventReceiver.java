package sg.edu.smu.ecology;

import java.util.List;

/**
 * Created by Quentin ROY on 20/6/16.
 * <p/>
 * Interface for objects able to receive events.
 */
public interface EventReceiver {

    /**
     * Handle the events.
     *
     * @param eventType the type of the event
     * @param eventData the data of the event
     */
    public void handleEvent(String eventType, List<Object> eventData);
}