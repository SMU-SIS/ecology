package sg.edu.smu.ecology;

import java.util.Arrays;
import java.util.Collections;

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

    /**
     * The data sync instance
     */
    private DataSync dataSync;

    /**
     * Whether this is the data reference or not
     */
    private Boolean isReference;

    /**
     * @param name        the name of the room
     * @param ecology     the ecology this room is part of
     * @param isReference true when the device is the data sync reference
     */
    public Room(String name, Ecology ecology, Boolean isReference) {
        this(name, ecology, new EventBroadcasterFactory(), new DataSyncFactory(), isReference);
    }

    /**
     * Special constructor only for testing
     *
     * @param name                    the name of the room
     * @param ecology                 the ecology this room is part of
     * @param eventBroadcasterFactory to create event broadcaster that is part of this room
     * @param dataSyncFactory         to create data sync instance
     * @param isReference             true when the device is the data sync reference
     */
    Room(String name, Ecology ecology, EventBroadcasterFactory eventBroadcasterFactory,
         DataSyncFactory dataSyncFactory, Boolean isReference) {
        if (name == null || name.length() == 0 || name.equals(" ")) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.ecology = ecology;
        this.eventBroadcasterFactory = eventBroadcasterFactory;
        this.dataSyncFactory = dataSyncFactory;
        this.isReference = isReference;
    }

    /**
     * Get the ecology instance
     *
     * @return the ecology instance
     */
    public Ecology getEcology() {
        return ecology;
    }

    /**
     * @return the event broadcaster associated with the room.
     */
    public EventBroadcaster getEventBroadcaster() {
        if (eventBroadcaster == null) {
            eventBroadcaster = eventBroadcasterFactory.createEventBroadcaster(new EventBroadcaster.Connector() {
                @Override
                public void onEventBroadcasterMessage(EcologyMessage message) {
                    Room.this.onEventBroadcasterMessage(message);
                }
            });
        }
        return eventBroadcaster;
    }

    /**
     * @return the data sync object.
     */
    public DataSync getDataSyncObject() {
        if (dataSync == null) {
            dataSync = dataSyncFactory.createDataSync(new DataSync.Connector() {
                @Override
                public void onMessage(EcologyMessage message) {
                    Room.this.onDataSyncMessage(message);
                }
            }, new DataSync.SyncDataChangeListener() {
                @Override
                public void onDataUpdate(Object dataId, Object newValue, Object oldValue) {
                    getEventBroadcaster().publishLocalEvent(Settings.SYNC_DATA,
                            Arrays.asList(dataId, newValue, oldValue));
                }
            }, isReference);
        }
        return dataSync;
    }

    /**
     * Handle a message from the corresponding room instances in the other devices.
     *
     * @param message the content of the message
     */
    void onMessage(EcologyMessage message) {
        Integer messageId = (Integer) message.fetchArgument();

        // Check if the received message is a sync data message or an event broadcaster event and
        // route them accordingly.
        if (messageId == SYNC_DATA_MESSAGE_ID) {
            dataSync.onMessage(message);
        } else if (messageId == EVENT_MESSAGE_ID) {
            getEventBroadcaster().onRoomMessage(message);
        }
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology by adding the event
     * message id.
     *
     * @param message the message
     */
    private void onEventBroadcasterMessage(EcologyMessage message) {
        message.addArgument(EVENT_MESSAGE_ID);
        ecology.onRoomMessage(name, message);
    }

    /**
     * Forward the message coming from the Data Sync to the ecology by adding the data sync
     * message id.
     *
     * @param message the message
     */
    private void onDataSyncMessage(EcologyMessage message) {
        message.addArgument(SYNC_DATA_MESSAGE_ID);
        ecology.onRoomMessage(name, message);
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId    the id of the device that got connected
     * @param isReference if the device is the data reference or not
     */
    void onDeviceConnected(String deviceId, Boolean isReference) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_CONNECTED,
                Collections.<Object>singletonList(deviceId));
        if (isReference) {
            dataSync.onConnected();
        }
    }

    /**
     * Called when a device is disconnected
     *
     * @param deviceId    the id of the device that got disconnected
     * @param isReference if the device is the data reference or not
     */
    void onDeviceDisconnected(String deviceId, Boolean isReference) {
        getEventBroadcaster().publishLocalEvent(Settings.DEVICE_DISCONNECTED,
                Collections.<Object>singletonList(deviceId));
        if (isReference) {
            dataSync.onDisconnected();
        }
    }

    static class EventBroadcasterFactory {
        EventBroadcaster createEventBroadcaster(EventBroadcaster.Connector connector) {
            return new EventBroadcaster(connector);
        }
    }

    static class DataSyncFactory {
        DataSync createDataSync(DataSync.Connector connector,
                                DataSync.SyncDataChangeListener dataSyncChangeListener,
                                boolean dataSyncReference) {
            return new DataSync(connector, dataSyncChangeListener, dataSyncReference);
        }
    }
}
