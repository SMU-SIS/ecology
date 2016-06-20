package sg.edu.smu.ecology;

import android.os.Bundle;
import android.util.Log;

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

    private Map<String, List<EventReceiver>> eventReceivers = new HashMap<>();

    /**
     * The room the event broadcaster is part of.
     */
    private final Room room;

    /**
     * @param room the room the event broadcaster is part of.
     */
    public EventBroadcaster(Room room) {
        this.room = room;
    }

    /**
     * Handle the messages coming from the room.
     *
     * @param type the type of message
     * @param message the message
     */
    void onRoomMessage(short type, Bundle message) {
        if (type == MessageTypes.EVENT) {
            // Extract the event data from the message.
            byte[] eventData = message.getByteArray("event");
            if (eventData == null) {
                Log.w(TAG, "Received event message without event data.");
                return;
            }
            // Parse the event.
            Event event = EventEncoder.unpack(eventData);
            if(event == null){
                Log.w(TAG, "Event parsing failed");
            } else {
                onRoomEvent(event);
            }
        } else {
            Log.w(TAG, "Unknown message type " + type + ".");
            return;
        }
    }

    void onRoomEvent(Event event){
        // Fetch the list of event receiver for this particular event type.
        List<EventReceiver> thisEventReceivers = eventReceivers.get(event.getType());
        if(thisEventReceivers == null) {
            return;
        }
        // Forward the event.
        for (EventReceiver receiver : thisEventReceivers) {
            receiver.handleEvent(event);
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

        // If there is not receiver yet registered for this event, create one.
        if(thisEventReceivers == null){
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

        if(thisEventReceivers != null){
            thisEventReceivers.remove(eventReceiver);

            // If there is not receivers remaining for this particular event type, remove the list.
            if(thisEventReceivers.isEmpty()){
                eventReceivers.remove(eventType);
            }
        }
    }

    /**
     * Publish an event. The event will be transmitted to any receiver that subscribe to the event's
     * type from any device of the ecology
     *
     * @param eventType the event type
     * @param data      the event's data
     */
    public void publish(String eventType, Bundle data) {
        Event event = new Event(eventType, data);
        Bundle message = new Bundle();
        message.putByteArray("event", EventEncoder.pack(event));
        room.onEventBroadcasterMessage(MessageTypes.EVENT, message);
    }
}
