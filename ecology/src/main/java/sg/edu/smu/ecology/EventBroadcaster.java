package sg.edu.smu.ecology;

import android.app.Activity;
import android.content.Context;

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

    /**
     * This class represents an entry of the given event type. Each entry includes an event
     * receiver of the event and a boolean value denoting if this event is a background event or
     * not. An event type can have multiple entries.
     */
    private class EventBroadcasterEntry {
        private final EventReceiver eventReceiver;
        private final boolean backgroundEvent;

        EventBroadcasterEntry(EventReceiver eventReceiver, boolean backgroundEvent) {
            this.eventReceiver = eventReceiver;
            this.backgroundEvent = backgroundEvent;
        }

        EventReceiver getEventReceiver() {
            return eventReceiver;
        }

        boolean isBackgroundEvent() {
            return backgroundEvent;
        }
    }

    /**
     * The recipient for event broadcaster messages.
     */
    private final Connector connector;
    /**
     * The context of this event broadcaster
     */
    private Context context;
    private ActivityLifecycleTracker activityLifecycleTracker;
    private Map<String, List<EventBroadcasterEntry>> eventReceivers = new HashMap<>();

    /**
     * @param connector the recipient for event broadcaster messages.
     */
    public EventBroadcaster(Connector connector, Context context, ActivityLifecycleTracker tracker) {
        this.connector = connector;
        this.context = context;
        activityLifecycleTracker = tracker;
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
        // Grab the event's type.
        String eventType;
        try {
            eventType = (String) message.fetchArgument();
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unrecognized event message format.");
        }
        passEventToReceivers(eventType, message.getArguments());
    }

    // Forward an event to the receivers.
    private void passEventToReceivers(String eventType, List<Object> data) {
        // Fetch the list of event broadcaster entries for this particular event type.
        List<EventBroadcasterEntry> thisEventBroadcasterEntry = eventReceivers.get(eventType);
        if (thisEventBroadcasterEntry == null) {
            return;
        }

        // Event receivers cannot modify the received data
        List<Object> receivedMessage = Collections.unmodifiableList(data);

        // Forward the event to the receivers.
        for (EventBroadcasterEntry eventBroadcasterEntry : thisEventBroadcasterEntry) {
            // Check if the event is required in the background or not
            if (eventBroadcasterEntry.isBackgroundEvent()) {
                // Passed as a background event
                eventBroadcasterEntry.getEventReceiver().handleEvent(eventType, receivedMessage);
            } else {
                // Check the context type before forwarding the received message
                if (context instanceof Activity) {
                    // Check for current foreground activity
                    if (activityLifecycleTracker.getCurrentForegroundActivity() ==
                            (Activity) context) {
                        eventBroadcasterEntry.getEventReceiver().handleEvent(eventType, receivedMessage);
                    }
                } else {
                    eventBroadcasterEntry.getEventReceiver().handleEvent(eventType, receivedMessage);
                }

            }
        }
    }

    /**
     * Register an event receiver for the events of a certain type.
     *
     * @param eventType     the even type
     * @param eventReceiver the receiver to subscribed.
     */
    public void subscribe(String eventType, EventReceiver eventReceiver) {
        subscribe(eventType, eventReceiver, false);
    }

    /**
     * Register an event receiver for the events of a certain type.
     *
     * @param eventType       the even type
     * @param eventReceiver   the receiver to subscribed.
     * @param backgroundEvent if the event is a background event or not
     */
    public void subscribe(String eventType, EventReceiver eventReceiver, boolean backgroundEvent) {
        List<EventBroadcasterEntry> thisEventBroadcasterEntry = eventReceivers.get(eventType);

        // If no event broadcaster entry has been registered for this event yet, create a list.
        if (thisEventBroadcasterEntry == null) {
            thisEventBroadcasterEntry = new ArrayList<>();
            eventReceivers.put(eventType, thisEventBroadcasterEntry);
        }
        thisEventBroadcasterEntry.add(new EventBroadcasterEntry(eventReceiver, backgroundEvent));
    }

    /**
     * Unsubscribe an event receiver for the events of a certain type.
     *
     * @param eventType     the even type
     * @param eventReceiver the receiver to unsubscribed
     */
    public void unsubscribe(String eventType, EventReceiver eventReceiver) {
        List<EventBroadcasterEntry> thisEventBroadcasterEntry = eventReceivers.get(eventType);

        if (thisEventBroadcasterEntry != null) {
            for (EventBroadcasterEntry eventBroadcasterEntry : thisEventBroadcasterEntry) {
                if (eventBroadcasterEntry.getEventReceiver() == eventReceiver) {
                    thisEventBroadcasterEntry.remove(eventBroadcasterEntry);
                }
            }

            // If no event broadcaster entry exists for this particular event type, remove the list.
            if (thisEventBroadcasterEntry.isEmpty()) {
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
        message.setTargetType(EcologyMessage.TARGET_TYPE_BROADCAST);

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

    interface Connector {
        void onEventBroadcasterMessage(EcologyMessage message);
    }
}