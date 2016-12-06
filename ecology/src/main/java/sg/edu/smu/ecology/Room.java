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

    private final static int SYNC_DATA_MESSAGE_ID = 0;
    private final static int EVENT_MESSAGE_ID = 1;

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
    private DataSync dataSync;

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
        this.eventBroadcaster = new EventBroadcaster(new EventBroadcaster.Connector() {
            @Override
            public void onEventBroadcasterMessage(List<Object> message) {
                Room.this.onEventBroadcasterMessage(message);
            }
        });

        this.dataSync = new DataSync(new DataSync.Connector() {
            @Override
            public void onMessage(List<Object> message) {
                Room.this.onDataSyncMessage(message);
            }
        });
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
     * @return the data sync object associated with the room.
     */
    public DataSync getDataSyncObject() {
        return dataSync;
    }

    /**
     * Handle message from the room.
     *
     * @param message the content of the message
     */
    void onMessage(List<Object> message) {
        // Check if the received message is a sync data message or an event broadcaster event and
        // route them accordingly.
        if (message.get(message.size() - 1).equals(SYNC_DATA_MESSAGE_ID)) {
            handleReceivedSyncData(message);
        } else if (message.get(message.size() - 1).equals(EVENT_MESSAGE_ID)) {
            getEventBroadcaster().onRoomMessage(message.subList(0, message.size() - 1));
        }
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology by adding the event
     * message id.
     *
     * @param message the message
     */
    private void onEventBroadcasterMessage(List<Object> message) {
        List<Object> msg = new ArrayList<>(message);
        msg.add(EVENT_MESSAGE_ID);
        ecology.onRoomMessage(name, msg);
    }

    /**
     * Forward the message coming from the Data Sync to the ecology by adding the data sync
     * message id.
     *
     * @param message the message
     */
    private void onDataSyncMessage(List<Object> message) {
        List<Object> msg = new ArrayList<>(message);
        msg.add(SYNC_DATA_MESSAGE_ID);
        ecology.onRoomMessage(name, msg);
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

    /**
     * Handle the received sync data from any device in the ecology
     *
     * @param message the message received
     */
    private void handleReceivedSyncData(List<Object> message) {
        dataSync.getSyncDataChangeListener().onDataUpdate(message.subList(0, message.size() - 1));
        getEventBroadcaster().publishLocalEvent("syncData",
                message.subList(0, message.size() - 1));
    }
}
