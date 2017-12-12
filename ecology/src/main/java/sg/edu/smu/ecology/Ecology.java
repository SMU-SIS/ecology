package sg.edu.smu.ecology;


import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.edu.smu.ecology.connector.Connector;

/**
 * Main Ecology class. Represents a group of devices closely linked together. The connected devices
 * can synchronize data across them, subscribe to events, send events
 *
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
public class Ecology {
    /**
     * Used for debugging.
     */
    private final static String TAG = Ecology.class.getSimpleName();

    /**
     * To route the incoming messages - indicates ecology data sync message
     */
    private final static int SYNC_DATA_MESSAGE_ID = 0;

    /**
     * To route the incoming messages - indicates message destined for a room
     */
    private final static int ROOM_MESSAGE_ID = 1;

    /**
     * Connector used to send messages to the other devices of the ecology.
     */
    private Connector connector;

    /**
     * Used for creating rooms part of this ecology
     */
    private RoomFactory roomFactory;

    /**
     * A map storing the different rooms of the ecology.
     */
    private Map<String, Room> rooms = new HashMap<>();

    /**
     * The id of this device
     */
    private String myDeviceId;

    /**
     * Whether this device is the reference or not
     */
    private Boolean isReference;

    /**
     * Used for creating data sync instance.
     */
    private DataSyncFactory dataSyncFactory;

    /**
     * Used for creating Ecology Looper instance.
     */
    private EcologyLooperFactory ecologyLooperFactory;

    /**
     * Data sync part of the ecology
     */
    private DataSync ecologyDataSync;

    private EcologyLooper ecologyLooper;

    private Handler connectorHandler;

    /**
     * The application currently in use
     */
    private Application application;

    private ActivityLifecycleTracker activityLifecycleTracker;

    /**
     * @param ecologyConnector the connector used to send messages to the other devices of the
     *                         ecology.
     * @param isReference      true when the device is the data sync reference
     */
    public Ecology(Connector ecologyConnector, Boolean isReference) {
        this(ecologyConnector, isReference, new RoomFactory(), new DataSyncFactory(),
                new EcologyLooperFactory());
    }

    /**
     * Special constructor only for testing
     *
     * @param connector            the connector used to send messages to the other devices of the ecology
     * @param isReference          true when the device is the data sync reference
     * @param roomFactory          to create rooms part of this ecology
     * @param dataSyncFactory      to create data sync instance
     * @param ecologyLooperFactory to create ecology looper instance
     */
    Ecology(final Connector connector, Boolean isReference, RoomFactory roomFactory,
            DataSyncFactory dataSyncFactory, EcologyLooperFactory ecologyLooperFactory) {
        this.connector = connector;
        this.isReference = isReference;
        this.roomFactory = roomFactory;
        this.dataSyncFactory = dataSyncFactory;
        this.ecologyLooperFactory = ecologyLooperFactory;

        this.connector.setReceiver(new Connector.Receiver() {

            @Override
            public void onMessage(EcologyMessage message) {
                Ecology.this.onConnectorMessage(message);
            }

            @Override
            public void onDeviceConnected(String deviceId) {
                Ecology.this.syncConnectedDeviceId(deviceId, false);
            }

            @Override
            public void onDeviceDisconnected(String deviceId) {
                Ecology.this.syncDisconnectedDeviceId(deviceId);
            }

            @Override
            public void onConnected() {
                Ecology.this.getEcologyDataSync().onConnected();
                Ecology.this.onEcologyConnected();
            }

            @Override
            public void onDisconnected() {
                Ecology.this.getEcologyDataSync().onDisconnected();
                Ecology.this.getEcologyDataSync().clear();
                Ecology.this.onEcologyDisconnected();
            }
        });
    }

    /**
     * When the list of available devices in the ecology has been updated
     *
     * @param newValue the new list of available devices
     * @param oldValue the old list of available devices
     */
    private void onDevicesListUpdate(Map<String, Boolean> newValue, Map<String, Boolean> oldValue) {
        newValue = newValue == null ? Collections.<String, Boolean>emptyMap() : newValue;
        oldValue = oldValue == null ? Collections.<String, Boolean>emptyMap() : oldValue;

        for (Map.Entry<String, Boolean> entry : newValue.entrySet()) {
            String entryKey = entry.getKey();
            if (!oldValue.containsKey(entryKey) && !(entryKey).equals(getMyDeviceId())) {
                onDeviceConnected(entryKey, entry.getValue());
            }
        }

        for (Map.Entry<String, Boolean> entry : oldValue.entrySet()) {
            String entryKey = entry.getKey();
            if (!newValue.containsKey(entryKey) && !(entryKey).equals(getMyDeviceId())) {
                onDeviceDisconnected(entryKey, entry.getValue());
            }
        }
    }

    /**
     * Sync the device id of the newly connected device in the ecology data sync
     *
     * @param newDeviceId the device id of the newly connected device
     * @param isReference whether the device is the data reference
     */
    private void syncConnectedDeviceId(String newDeviceId, boolean isReference) {
        // Add the newly connected device id
        Map<Object, Object> devicesMap = new HashMap<>((Map<?, ?>) getEcologyDataSync().getData
                ("devices"));
        devicesMap.put(newDeviceId, isReference);
        getEcologyDataSync().setData("devices", devicesMap);
    }

    /**
     * Sync the device id of the disconnected device in the ecology data sync
     *
     * @param deviceId the device id of the disconnected device
     */
    private void syncDisconnectedDeviceId(String deviceId) {
        Map<Object, Object> devicesMap = new HashMap<>((Map<?, ?>) getEcologyDataSync().getData
                ("devices"));
        devicesMap.remove(deviceId);
        getEcologyDataSync().setData("devices", devicesMap);
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId    the id of the device that got connected
     * @param isReference if the device is the data reference or not
     */
    private void onDeviceConnected(String deviceId, Boolean isReference) {
        for (Room room : rooms.values()) {
            room.onDeviceConnected(deviceId, isReference);
        }
    }

    /**
     * Called when a device is disconnected.
     *
     * @param deviceId    the id of the device that got disconnected
     * @param isReference if the device is the data reference or not
     */
    private void onDeviceDisconnected(String deviceId, Boolean isReference) {
        for (Room room : rooms.values()) {
            room.onDeviceDisconnected(deviceId, isReference);
        }
    }

    /**
     * Called when device is connected to ecology. If the device is the reference, then it means
     * it's ready for accepting connections and if it is a non-reference device, then it means it's
     * connected to the reference.
     */
    private void onEcologyConnected() {
        for (Room room : rooms.values()) {
            room.onEcologyConnected();
        }
    }

    /**
     * Called when a client device gets disconnected from the reference or server device. This also
     * means that the device has been disconnected from ecology.
     */
    private void onEcologyDisconnected() {
        for (Room room : rooms.values()) {
            room.onEcologyDisconnected();
        }
    }

    /**
     * Connect to the ecology.
     */
    void connect(Context context, String deviceId, Application application) {
        myDeviceId = deviceId;
        connector.connect(context, deviceId);
        this.application = application;

        setConnectorHandler(new Handler(context.getMainLooper()));

        // Register for activity lifecyle tracking
        activityLifecycleTracker = new ActivityLifecycleTracker();
        application.registerActivityLifecycleCallbacks(activityLifecycleTracker);

        if (isReference) {
            getEcologyDataSync().setData("devices", new HashMap<Object, Object>() {{
                put(getMyDeviceId(), true);
            }});
        }
    }

    /**
     * Disconnect from the ecology.
     */
    void disconnect() {
        connector.disconnect();
        // Unregister from activity lifecycle tracking
        application.unregisterActivityLifecycleCallbacks(activityLifecycleTracker);
        getEcologyLooper().quit();
    }

    /**
     * Get the device id of this device
     *
     * @return the device id
     */
    public String getMyDeviceId() {
        return myDeviceId;
    }

    /**
     * Get the looper associated with the ecology
     *
     * @return the ecology looper
     */
    private EcologyLooper getEcologyLooper() {
        if (ecologyLooper == null) {
            ecologyLooper = ecologyLooperFactory.createEcologyLooper("EcologyLooperThread");
            ecologyLooper.start();
            ecologyLooper.prepareHandler();
        }
        return ecologyLooper;
    }

    /**
     * Get the connector handler instance.
     *
     * @return the connector handler instance.
     */
    private Handler getConnectorHandler() {
        return connectorHandler;
    }

    /**
     * Get the activity lifecycle tracker instance.
     *
     * @return the activity lifecycle tracker instance.
     */
    ActivityLifecycleTracker getActivityLifecycleTracker() {
        return activityLifecycleTracker;
    }

    /**
     * To set the connector handler. This is mainly used for unit testing
     *
     * @param handler the connector handler
     */
    void setConnectorHandler(Handler handler) {
        connectorHandler = handler;
    }

    /**
     * @return the data sync object.
     */
    DataSync getEcologyDataSync() {
        if (ecologyDataSync == null) {
            // Create ecology data sync
            ecologyDataSync = dataSyncFactory.createDataSync(new DataSync.Connector() {
                @Override
                public void onMessage(EcologyMessage message) {
                    if (isReference) {
                        onEcologyDataSyncMessage(message);
                    }
                }
            }, new DataSync.SyncDataChangeListener() {
                @Override
                public void onDataUpdate(Object dataId, Object newValue, Object oldValue) {
                    if (dataId.equals("devices")) {
                        onDevicesListUpdate((Map<String, Boolean>) newValue,
                                (Map<String, Boolean>) oldValue);
                    }
                }
            }, isReference, this);
        }
        return ecologyDataSync;
    }

    /**
     * Receive messages from the other devices of the ecology.
     *
     * @param message the message content
     */
    private void onConnectorMessage(final EcologyMessage message) {
        getEcologyLooper().getHandler().post(new Runnable() {
            @Override
            public void run() {
                handleMessage(message);
            }
        });
    }

    /**
     * When a message received needs to be forwarded to the correct room
     *
     * @param message the content of the received message
     */
    private void forwardRoomMessage(EcologyMessage message) {
        String targetRoomName = null;
        try {
            targetRoomName = (String) message.fetchArgument();
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            //throw new IllegalArgumentException("Unrecognized message format.");
            Log.e(TAG, "Exception " + e.getMessage());
        }

        Room room = rooms.get(targetRoomName);
        if (room != null) {
            room.onMessage(message);
        }
    }

    /**
     * Send a message to the other devices of the ecology from a specific room.
     *
     * @param roomName the name of the room who send the event
     * @param message  the content of the message
     */
    void onRoomMessage(String roomName, EcologyMessage message) {
        message.addArgument(roomName);
        message.addArgument(ROOM_MESSAGE_ID);
        message.setSource(getMyDeviceId());
        sendConnectorMessage(message);
    }

    /**
     * Send a data sync message to the other devices of the ecology
     *
     * @param message the content of the message
     */
    void onEcologyDataSyncMessage(EcologyMessage message) {
        message.addArgument(SYNC_DATA_MESSAGE_ID);
        message.setSource(getMyDeviceId());
        sendConnectorMessage(message);
    }

    /**
     * Send the message after moving it into the connector's context looper
     *
     * @param message the content of the message
     */
    private void sendConnectorMessage(final EcologyMessage message) {
        getConnectorHandler().post(new Runnable() {
            @Override
            public void run() {
                connector.sendMessage(message);
            }
        });
    }

    /**
     * Get the list of available devices in the ecology.
     *
     * @return the list of device ids of the available devices in the ecology
     */
    public List<String> getAvailableDevices() {
        if (getEcologyDataSync().getData("devices") != null) {
            return new ArrayList<>((Collection<? extends String>) ((Map<?, ?>) getEcologyDataSync().
                    getData("devices")).keySet());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Get a room. The room will be created if needed.
     *
     * @param roomName the name of the room
     * @return the room instance
     */
    public Room getRoom(String roomName) {
        Room room = rooms.get(roomName);
        if (room == null) {
            room = roomFactory.createRoom(roomName, this, isReference);
            rooms.put(roomName, room);
        }
        return room;
    }

    static class RoomFactory {
        Room createRoom(String roomName, Ecology ecology, Boolean isReference) {
            return new Room(roomName, ecology, isReference);
        }
    }

    static class DataSyncFactory {
        DataSync createDataSync(DataSync.Connector connector,
                                DataSync.SyncDataChangeListener dataSyncChangeListener,
                                boolean dataSyncReference, Ecology ecology) {
            return new DataSync(connector, dataSyncChangeListener, dataSyncReference, ecology);
        }
    }

    static class EcologyLooperFactory {
        EcologyLooper createEcologyLooper(String name) {
            return new EcologyLooper(name);
        }
    }

    /**
     * Handle the messages in the ecology looper
     *
     * @param msg the message in th ecology looper
     */
    private void handleMessage(EcologyMessage msg) {
        // Check the message type and route them accordingly.
        switch ((Integer) msg.fetchArgument()) {
            // A message destined for a room
            case ROOM_MESSAGE_ID:
                forwardRoomMessage(msg);
                break;

            // A message destined for ecology data sync
            case SYNC_DATA_MESSAGE_ID:
                getEcologyDataSync().onMessage(msg);
                break;
        }
    }

    /**
     * Get the handler associated with the ecology looper
     *
     * @return the handler instance
     */
    Handler getHandler() {
        return getEcologyLooper().getHandler();
    }
}
