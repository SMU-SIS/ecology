package sg.edu.smu.ecology;


import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.edu.smu.ecology.connector.Connector;

/**
 * Created by Anuroop PATTENA VANIYAR on 1/6/2016.
 * <p>
 * Main Ecology class. Represents a group of devices closely linked together.
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
     * Data sync part of the ecology
     */
    private DataSync ecologyDataSync;

    /**
     * @param ecologyConnector the connector used to send messages to the other devices of the
     *                         ecology.
     * @param isReference      true when the device is the data sync reference
     */
    public Ecology(Connector ecologyConnector, Boolean isReference) {
        this(ecologyConnector, isReference, new RoomFactory(), new DataSyncFactory());
    }

    /**
     * Special constructor only for testing
     *
     * @param connector       the connector used to send messages to the other devices of the ecology
     * @param isReference     true when the device is the data sync reference
     * @param roomFactory     to create rooms part of this ecology
     * @param dataSyncFactory to create data sync instance
     */
    Ecology(final Connector connector, Boolean isReference, RoomFactory roomFactory,
            DataSyncFactory dataSyncFactory) {
        this.connector = connector;
        this.isReference = isReference;
        this.roomFactory = roomFactory;
        this.dataSyncFactory = dataSyncFactory;

        this.connector.setReceiver(new Connector.Receiver() {

            @Override
            public void onMessage(EcologyMessage message) {
                Ecology.this.onConnectorMessage(message);
            }

            @Override
            public void onDeviceConnected(String deviceId) {
                syncConnectedDeviceId(deviceId, false);
            }

            @Override
            public void onDeviceDisconnected(String deviceId) {
                syncDisconnectedDeviceId(deviceId);
            }

            @Override
            public void onConnected() {
                Ecology.this.getEcologyDataSync().onConnected();
            }

            @Override
            public void onDisconnected() {
                Ecology.this.getEcologyDataSync().onDisconnected();
                Ecology.this.getEcologyDataSync().clear();
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
     * @param newDeviceId     the device id of the newly connected device
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
     * @param deviceId        the id of the device that got connected
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
     * @param deviceId        the id of the device that got disconnected
     * @param isReference if the device is the data reference or not
     */
    private void onDeviceDisconnected(String deviceId, Boolean isReference) {
        for (Room room : rooms.values()) {
            room.onDeviceDisconnected(deviceId, isReference);
        }
    }

    /**
     * Connect to the ecology.
     */
    void connect(Context context, String deviceId) {
        myDeviceId = deviceId;
        connector.connect(context, deviceId);

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
    }

    /**
     * Get the device id of the device
     *
     * @return the device id
     */
    public String getMyDeviceId() {
        return myDeviceId;
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
            }, isReference);
        }
        return ecologyDataSync;
    }

    /**
     * Receive messages from the other devices of the ecology.
     *
     * @param message the message content
     */
    private void onConnectorMessage(EcologyMessage message) {
        Integer messageId = (Integer) message.fetchArgument();

        // Check if the received message is an ecology sync data message or a message destined for a
        // room and route them accordingly.
        if (messageId == ROOM_MESSAGE_ID) {
            forwardRoomMessage(message);
        } else if (messageId == SYNC_DATA_MESSAGE_ID) {
            getEcologyDataSync().onMessage(message);
        }
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
        connector.sendMessage(message);
    }

    /**
     * Send a data sync message to the other devices of the ecology
     *
     * @param message the content of the message
     */
    void onEcologyDataSyncMessage(EcologyMessage message) {
        message.addArgument(SYNC_DATA_MESSAGE_ID);
        message.setSource(getMyDeviceId());
        connector.sendMessage(message);
    }

    /**
     * @return the list of available devices in the ecology
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
     * @return the room
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
        public Room createRoom(String roomName, Ecology ecology, Boolean isReference) {
            return new Room(roomName, ecology, isReference);
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
