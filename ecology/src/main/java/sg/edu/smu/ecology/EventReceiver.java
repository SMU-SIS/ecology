package sg.edu.smu.ecology;

/**
 * Created by Quentin ROY on 20/6/16.
 *
 * Interface for objects able to receive events.
 */
public interface EventReceiver {

    /**
     * Handle the events.
     *
     * @param event an event
     */
    public void handleEvent(Event event);
}