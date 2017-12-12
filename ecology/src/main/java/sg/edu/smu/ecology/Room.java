package sg.edu.smu.ecology;

import android.content.Context;

import java.util.Arrays;
import java.util.Collections;

/**
 * Represent an ecology space dedicated to a same set of functionalities (e.g. an application).
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
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
     * Used for creating data sync instance.
     */
    private DataSyncFactory dataSyncFactory;

    /**
     * The name of the room.
     */
    private String name;

    /**
     * Used for creating event broadcaster manager instance.
     */
    private EventBroadcasterManagerFactory eventBroadcasterManagerFactory;

    /**
     * The data sync instance
     */
    private DataSync dataSync;

    /**
     * Whether this is the data reference or not
     */
    private Boolean isReference;

    private EventBroadcasterManager eventBroadcasterManager;

    /**
     * Creates a new room part of the ecology.
     *
     * @param name        the name of the room
     * @param ecology     the ecology this room is part of
     * @param isReference true when this device is the data sync reference
     */
    public Room(String name, Ecology ecology, Boolean isReference) {
        this(name, ecology, isReference, new DataSyncFactory(),
                new EventBroadcasterManagerFactory());
    }

    /**
     * Special constructor only for testing
     *
     * @param name            the name of the room
     * @param ecology         the ecology this room is part of
     * @param dataSyncFactory to create data sync instance
     * @param isReference     true when the device is the data sync reference
     */
    Room(String name, Ecology ecology, Boolean isReference, DataSyncFactory dataSyncFactory,
         EventBroadcasterManagerFactory eventBroadcasterManagerFactory) {
        if (name == null || name.length() == 0 || name.equals(" ")) {
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.ecology = ecology;
        this.isReference = isReference;
        this.dataSyncFactory = dataSyncFactory;
        this.eventBroadcasterManagerFactory = eventBroadcasterManagerFactory;
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
     * Get the event broadcaster associated with this room for a given context. Event broadcaster
     * is used to subscribe and unsubscribe ecology events as well as to publish events.
     *
     * @param context the context associated with the event broadcaster
     * @return the event broadcaster associated with the room and context.
     */
    public EventBroadcaster getEventBroadcaster(Context context) {
        return getEventBroadcasterManager().getEventBroadcaster(context);
    }

    /**
     * Get the event broadcaster manager instance required to get the event broadcaster instances
     *
     * @return the event broadcaster manager object
     */
    EventBroadcasterManager getEventBroadcasterManager() {
        if (eventBroadcasterManager == null) {
            eventBroadcasterManager = eventBroadcasterManagerFactory.createEventBroadcasterManager(this);
        }

        return eventBroadcasterManager;
    }

    /**
     * Get a data sync object required to sync data across connected devices part of the ecology.
     * If the object was already created, this will return the already created instance.
     *
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
                    getEventBroadcasterManager().postLocalEvent(Settings.SYNC_DATA,
                            Arrays.asList(dataId, newValue, oldValue));
                }
            }, isReference, getEcology());
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
            getDataSyncObject().onMessage(message);
        } else if (messageId == EVENT_MESSAGE_ID) {
            getEventBroadcasterManager().forwardMessage(message);
        }
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology by adding the event
     * message id.
     *
     * @param message the message
     */
    void onEventBroadcasterMessage(EcologyMessage message) {
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
        getEventBroadcasterManager().postLocalEvent(Settings.DEVICE_CONNECTED,
                Collections.<Object>singletonList(deviceId));

        if (isReference) {
            getDataSyncObject().onConnected();
        }
    }

    /**
     * Called when a device is disconnected
     *
     * @param deviceId    the id of the device that got disconnected
     * @param isReference if the device is the data reference or not
     */
    void onDeviceDisconnected(String deviceId, Boolean isReference) {
        getEventBroadcasterManager().postLocalEvent(Settings.DEVICE_DISCONNECTED,
                Collections.<Object>singletonList(deviceId));

        if (isReference) {
            getDataSyncObject().onDisconnected();
        }
    }

    /**
     * Called when this device is connected to the ecology.
     */
    void onEcologyConnected() {
        getEventBroadcasterManager().postLocalEvent(Settings.ECOLOGY_CONNECTED,
                Collections.emptyList());
    }

    /**
     * Called when this device is disconnected from the ecology.
     */
    void onEcologyDisconnected() {
        getEventBroadcasterManager().postLocalEvent(Settings.ECOLOGY_DISCONNECTED,
                Collections.emptyList());
    }

    static class DataSyncFactory {
        DataSync createDataSync(DataSync.Connector connector,
                                DataSync.SyncDataChangeListener dataSyncChangeListener,
                                boolean dataSyncReference, Ecology ecology) {
            return new DataSync(connector, dataSyncChangeListener, dataSyncReference, ecology);
        }
    }

    static class EventBroadcasterManagerFactory {
        EventBroadcasterManager createEventBroadcasterManager(Room room) {
            return new EventBroadcasterManager(room);
        }
    }
}
