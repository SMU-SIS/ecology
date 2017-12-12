package sg.edu.smu.ecology;

import java.util.List;

/**
 * Created by Quentin ROY on 20/6/16.
 * <p/>
 * Interface for objects able to receive ecology events. The events can be local as well as from
 * other connected devices in the ecology.
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