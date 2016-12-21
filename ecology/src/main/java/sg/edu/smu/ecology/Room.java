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

    private final static int SYNC_DATA_MESSAGE_ID = 0;
    private final static int EVENT_MESSAGE_ID = 1;

    /**
     * The ecology of the room.
     */
    private Ecology ecology;

    /**
     * Used for creating event broadcaster associated with the room.
     */
    private EventBroadcasterFactory eventBroadcasterFactory;

    /**
     * Used for creating data sync instance.
     */
    private DataSyncFactory dataSyncFactory;

    /**
     * The name of the room.
     */
    private String name;

    /**
     * The event broadcaster associated with the room.
     */
    private EventBroadcaster eventBroadcaster;
    private EventBroadcaster.Connector connector = new EventBroadcaster.Connector() {
        @Override
        public void onEventBroadcasterMessage(List<Object> message) {
            Room.this.onEventBroadcasterMessage(message);
        }
    };

    /**
     * The data sync instance
     */
    private DataSync dataSync;
    private DataSync.Connector dataSyncConnector = new DataSync.Connector() {
        @Override
        public void onMessage(List<Object> message) {
            Room.this.onDataSyncMessage(message);
        }
    };
    private DataSync.SyncDataChangeListener dataSyncChangeListener =
            new DataSync.SyncDataChangeListener() {
                @Override
                public void onDataUpdate(Object dataId, Object newValue, Object oldValue) {
                    getEventBroadcaster().publishLocalEvent(Settings.SYNC_DATA,
                            Arrays.asList(dataId, newValue, oldValue));
                }
            };

    /**
     * @param name    the name of the room
     * @param ecology the ecology this room is part of
     */
    public Room(String name, Ecology ecology) {
        this(name, ecology, new EventBroadcasterFactory(), new DataSyncFactory());
    }

    /**
     * Special constructor only for testing
     *
     * @param name                    the name of the room
     * @param ecology                 the ecology this room is part of
     * @param eventBroadcasterFactory to create event broadcaster that is part of this room
     * @param dataSyncFactory         to create data sync instance
     */
    Room(String name, Ecology ecology, EventBroadcasterFactory eventBroadcasterFactory,
         DataSyncFactory dataSyncFactory) {
        if (name == null || name.length() == 0 || name.equals(" ")) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.ecology = ecology;
        this.eventBroadcasterFactory = eventBroadcasterFactory;
        this.dataSyncFactory = dataSyncFactory;
    }

    /**
     * @return the event broadcaster associated with the room.
     */
    public EventBroadcaster getEventBroadcaster() {
        if (eventBroadcaster == null) {
            eventBroadcaster = eventBroadcasterFactory.createEventBroadcaster(connector);
        }
        return eventBroadcaster;
    }

    /**
     * @return the data sync object.
     */
    public DataSync getDataSyncObject() {
        if (dataSync == null) {
            dataSync = dataSyncFactory.createDataSync(dataSyncConnector, dataSyncChangeListener);
        }
        return dataSync;
    }

    /**
     * Handle a message from the corresponding room instances in the other devices.
     *
     * @param message the content of the message
     */
    void onMessage(List<Object> message) {
        // Check if the received message is a sync data message or an event broadcaster event and
        // route them accordingly.
        if (message.get(message.size() - 1).equals(SYNC_DATA_MESSAGE_ID)) {
            getDataSyncObject().onMessage(message.subList(0, message.size() - 1));
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

    static class EventBroadcasterFactory {
        EventBroadcaster createEventBroadcaster(EventBroadcaster.Connector connector) {
            return new EventBroadcaster(connector);
        }
    }

    static class DataSyncFactory {
        DataSync createDataSync(DataSync.Connector connector,
                                DataSync.SyncDataChangeListener dataSyncChangeListener) {
            return new DataSync(connector, dataSyncChangeListener);
        }
    }
}
