package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Anuroop PATTENA VANIYAR (anurooppv@smu.edu.sg) on 1/6/2016.
 * <p/>
 * Event broadcaster are used to publish and received events to and from anywhere in the ecology.
 */
public class EventBroadcaster {

    private final static String TAG = EventBroadcaster.class.getSimpleName();

    interface Connector {
        void onEventBroadcasterMessage(EcologyMessage message);
    }

    /**
     * The recipient for event broadcaster messages.
     */
    private final Connector connector;
    private Map<String, List<EventReceiver>> eventReceivers = new HashMap<>();

    /**
     * @param connector the recipient for event broadcaster messages.
     */
    public EventBroadcaster(Connector connector) {
        this.connector = connector;
    }

    /**
     * Handle the messages coming from the room.
     *
     * @param message the message
     */
    void onRoomMessage(EcologyMessage message) {
        // Only message event are supported.
        handleEventMessage(message);
    }

    private void handleEventMessage(EcologyMessage message) {
        EcologyMessage msg = new EcologyMessage(message.getArguments());

        // Grab the event's type.
        String eventType;
        try {
            eventType = (String) msg.fetchArgument();
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unrecognized event message format.");
        }
        passEventToReceivers(eventType, msg.getArguments());
    }

    // Forward an event to the receivers.
    private void passEventToReceivers(String eventType, List<Object> data) {
        // Fetch the list of event receiver for this particular event type.
        List<EventReceiver> thisEventReceivers = eventReceivers.get(eventType);
        if (thisEventReceivers == null) {
            return;
        }

        // Event receivers cannot modify the received data
        List<Object> receivedMessage = Collections.unmodifiableList(data);

        // Forward the event to the receivers.
        for (EventReceiver receiver : thisEventReceivers) {
            receiver.handleEvent(eventType, receivedMessage);
        }
    }

    /**
     * Register an event receiver for the events of a certain type.
     *
     * @param eventType     the even type
     * @param eventReceiver the receiver to subscribed.
     */
    public void subscribe(String eventType, EventReceiver eventReceiver) {
        List<EventReceiver> thisEventReceivers = eventReceivers.get(eventType);

        // If there is not receiver yet registered for this event, create a receiver list.
        if (thisEventReceivers == null) {
            thisEventReceivers = new ArrayList<>();
            eventReceivers.put(eventType, thisEventReceivers);
        }
        thisEventReceivers.add(eventReceiver);
    }

    /**
     * Unsubscribe an event receiver for the events of a certain type.
     *
     * @param eventType     the even type
     * @param eventReceiver the receiver to unsubscribed
     */
    public void unsubscribe(String eventType, EventReceiver eventReceiver) {
        List<EventReceiver> thisEventReceivers = eventReceivers.get(eventType);

        if (thisEventReceivers != null) {
            thisEventReceivers.remove(eventReceiver);

            // If there is not receivers remaining for this particular event type, remove the list.
            if (thisEventReceivers.isEmpty()) {
                eventReceivers.remove(eventType);
            }
        }
    }

    /**
     * Publish an event. The event will be transmitted to any receiver that subscribe to the event's
     * type from any device of the ecology.
     *
     * @param eventType the event type
     * @param data      the event's data
     */
    public void publish(String eventType, List<Object> data) {
        // Create the message to be sent to the other devices of the ecology.
        EcologyMessage message = new EcologyMessage(data);
        message.addArgument(eventType);

        connector.onEventBroadcasterMessage(message);
        // Pass the event to the local receivers.
        publishLocalEvent(eventType, data);
    }

    /**
     * Publish an event. The event will be transmitted to any receiver that subscribe to the event's
     * type from any device of the ecology.
     *
     * @param eventType the event type
     */
    public void publish(String eventType) {
        publish(eventType, Collections.emptyList());
    }

    /**
     * Publish an event. The event will be transmitted to any receiver that subscribe to the event's
     * type from any device of the ecology.
     *
     * @param eventType the event type
     * @param dataArgs  the event's data
     */
    public void publishWithArgs(String eventType, Object... dataArgs) {
        publish(eventType, Arrays.asList(dataArgs));
    }

    /**
     * Publish an event locally (the event is not shared with the other device of the ecology).
     *
     * @param eventType the event type
     * @param data      the event's data
     */
    void publishLocalEvent(String eventType, List<Object> data) {
        passEventToReceivers(eventType, data);
    }
}
