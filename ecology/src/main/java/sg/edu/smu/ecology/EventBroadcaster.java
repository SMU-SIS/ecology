package sg.edu.smu.ecology;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
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
    /**
     * The room the event broadcaster is part of.
     */
    private final Room room;
    private Map<String, List<EventReceiver>> eventReceivers = new HashMap<>();
    private Handler mainLoopHandler = new Handler(Looper.getMainLooper());

    /**
     * @param room the room the event broadcaster is part of.
     */
    public EventBroadcaster(Room room) {
        this.room = room;
    }

    /**
     * Handle the messages coming from the room.
     *
     * @param message the message
     */
    void onRoomMessage(List<Object> message) {
        // Only message event are supported.
        handleEventMessage(message);
    }

    /**
     * Handle the ecology connector connected messages coming from the room.
     *
     * @param message the message
     */
    void onConnectorConnectedMessage(List<Object> message) {
        handleEventMessage(message);
    }

    /**
     * Handle the ecology connector disconnected messages coming from the room.
     *
     * @param message the message
     */
    void onConnectorDisconnectedMessage(List<Object> message) {
        handleEventMessage(message);
    }

    private void handleEventMessage(List<Object> message) {
        // Grab the event's type.
        String eventType;
        try {
            eventType = (String) message.get(message.size() - 1);
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unrecognized event message format.");
        }
        passEventToReceivers(eventType, message.subList(0, message.size() - 1));
    }


    // Forward an event to the receivers.
    private void passEventToReceivers(final String eventType, final List<Object> data){
        // Fetch the list of event receiver for this particular event type.
        List<EventReceiver> thisEventReceivers = eventReceivers.get(eventType);
        if (thisEventReceivers == null) {
            return;
        }
        // Forward the event to the receivers.
        for (final EventReceiver receiver : thisEventReceivers) {
            mainLoopHandler.post(new Runnable() {
                @Override
                public void run() {
                    receiver.handleEvent(eventType, data);
                }
            });
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
        List<Object> msg = new ArrayList<>(data);
        msg.add(eventType);
        room.onEventBroadcasterMessage(msg);
        // Pass the event to the local receivers.
        publishLocalEvent(eventType, data);
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
