package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Quentin ROY on 20/6/16.
 * <p>
 * Represent an ecology space dedicated to a same set of functionalities (e.g. an application).
 */
public class Room {
    private final static String TAG = Room.class.getSimpleName();

    /**
     * The ecology of the room.
     */
    private final Ecology ecology;

    /**
     * The name of the room.
     */
    private String name;

    /**
     * The event broadcaster associated with the room.
     */
    private EventBroadcaster eventBroadcaster;

    /**
     * @param name    the name of the room
     * @param ecology the ecology this room is part of
     */
    public Room(String name, Ecology ecology) {
        if (name == null || name.length() == 0 || name.equals(" ")) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.ecology = ecology;
        this.eventBroadcaster = new EventBroadcaster(this);
    }

    /**
     * Special constructor only for testing
     *
     * @param name             the name of the room
     * @param ecology          the ecology this room is part of
     * @param eventBroadcaster the event broadcaster that is part of this room
     */
    Room(String name, Ecology ecology, EventBroadcaster eventBroadcaster) {
        this.name = name;
        this.ecology = ecology;
        this.eventBroadcaster = eventBroadcaster;
    }

    /**
     * @return the event broadcaster associated with the room.
     */
    public EventBroadcaster getEventBroadcaster() {
        return eventBroadcaster;
    }

    /**
     * Handle message from the room.
     *
     * @param message the content of the message
     */
    void onMessage(List<Object> message) {
        // Currently, only event broadcaster messages are supported.
        eventBroadcaster.onRoomMessage(message);
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology.
     *
     * @param message the message
     */
    void onEventBroadcasterMessage(List<Object> message) {
        ecology.onRoomMessage(name, message);
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId the id of the device that got connected
     */
    void onDeviceConnected(String deviceId) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_CONNECTED,
                Collections.<Object>singletonList(deviceId));
    }

    /**
     * Called when a device is disconnected
     *
     * @param deviceId the id of the device that got disconnected
     */
    void onDeviceDisconnected(String deviceId) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_DISCONNECTED,
                Collections.<Object>singletonList(deviceId));
    }
}
