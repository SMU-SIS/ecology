package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.Arrays;
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
     * To store the sync data
     */
    private SyncData syncData = new SyncData();

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
        if (message.get(message.size() - 1).equals("syncData")) {
            handleReceivedSyncData(message);
        }
        getEventBroadcaster().onRoomMessage(message);

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
    public void onDeviceConnected(String deviceId) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_CONNECTED,
                Collections.<Object>singletonList(deviceId));
    }

    /**
     * Called when a device is disconnected
     *
     * @param deviceId the id of the device that got disconnected
     */
    public void onDeviceDisconnected(String deviceId) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_DISCONNECTED,
                Collections.<Object>singletonList(deviceId));
    }

    /**
     * To set any integer value that needs to be synced across the devices in the ecology
     *
     * @param key  the key linking to the data
     * @param data the data to be synced
     */
    public void setInteger(Object key, Integer data) {
        syncData.setDataSyncValue(key, data);
        getEventBroadcaster().publish("syncData", new ArrayList<Object>(Arrays.asList(key, data)));
    }

    /**
     * To get the sync data
     *
     * @param key the key paired to the sync data
     * @return the sync data
     */
    public Integer getInteger(Object key) {
        return (Integer) syncData.getDataSyncValue(key);
    }

    /**
     * Handle the received sync data from any device in the ecology
     *
     * @param message the message received
     */
    private void handleReceivedSyncData(List<Object> message) {
        syncData.setDataSyncValue(message.get(0), message.get(1));
    }
}
