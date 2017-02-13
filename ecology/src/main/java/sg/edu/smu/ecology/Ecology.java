package sg.edu.smu.ecology;


import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
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
    private String deviceId;

    /**
     * Whether this is the data reference or not
     */
    private Boolean isDataReference;

    /**
     * Data sync part of the ecology
     */
    private DataSync ecologyDataSync;

    /**
     * @param ecologyConnector the connector used to send messages to the other devices of the
     *                         ecology.
     */
    public Ecology(Connector ecologyConnector, Boolean isDataReference) {
        this(new RoomFactory(), ecologyConnector, isDataReference);
    }

    /**
     * Special constructor only for testing
     *
     * @param roomFactory to create rooms part of this ecology
     * @param connector   the connector used to send messages to the other devices of the
     *                    ecology.
     */
    Ecology(RoomFactory roomFactory, final Connector connector, final Boolean isDataReference) {
        this.roomFactory = roomFactory;
        this.connector = connector;
        this.isDataReference = isDataReference;

        ecologyDataSync = new DataSync(new DataSync.Connector() {
            @Override
            public void onMessage(EcologyMessage message) {
                onEcologyDataSyncMessage(message);
            }
        }, new DataSync.SyncDataChangeListener() {
            @Override
            public void onDataUpdate(Object dataId, Object newValue, Object oldValue) {
                onDevicesListUpdate();
            }
        }, isDataReference);

        this.connector.setReceiver(new Connector.Receiver() {

            @Override
            public void onMessage(EcologyMessage message) {
                Ecology.this.onConnectorMessage(message);
            }

            @Override
            public void onDeviceConnected(String deviceId, Boolean isDeviceDataReference) {
                if (isDataReference) {
                    syncConnectedDeviceId(deviceId);
                }
                Ecology.this.onDeviceConnected(deviceId, isDeviceDataReference);
            }

            @Override
            public void onDeviceDisconnected(String deviceId, Boolean isDeviceDataReference) {
                if (isDataReference) {
                    syncDisconnectedDeviceId(deviceId);
                }
                Ecology.this.onDeviceDisconnected(deviceId, isDeviceDataReference);
            }
        });
    }

    /**
     * Sync the device id of the newly connected device in the ecology data sync
     *
     * @param deviceId the device id of the newly connected device
     */
    private void syncConnectedDeviceId(String deviceId) {
        List<String> devicesList = new ArrayList<>((Collection<? extends String>)
                ecologyDataSync.getData("Devices"));
        devicesList.add(deviceId);
        ecologyDataSync.setData("Devices", devicesList);
    }

    /**
     * Sync the device id of the disconnected device in the ecology data sync
     *
     * @param deviceId the device id of the disconnected device
     */
    private void syncDisconnectedDeviceId(String deviceId) {
        List<String> devicesList = new ArrayList<>((Collection<? extends String>)
                ecologyDataSync.getData("Devices"));
        Iterator<String> iterator = devicesList.iterator();

        // Remove it's own device id
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.equals(deviceId)) {
                iterator.remove();
            }
        }
        ecologyDataSync.setData("Devices", devicesList);
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId        the id of the device that got connected
     * @param isDataReference if the device is the data reference or not
     */
    private void onDeviceConnected(String deviceId, Boolean isDataReference) {
        for (Room room : rooms.values()) {
            room.onDeviceConnected(deviceId, isDataReference);
        }
    }

    /**
     * Called when a device is disconnected.
     *
     * @param deviceId        the id of the device that got disconnected
     * @param isDataReference if the device is the data reference or not
     */
    private void onDeviceDisconnected(String deviceId, Boolean isDataReference) {
        for (Room room : rooms.values()) {
            room.onDeviceDisconnected(deviceId, isDataReference);
        }
    }

    /**
     * Called when devices list in ecology data sync is updated
     */
    private void onDevicesListUpdate() {
        for (Room room : rooms.values()) {
            room.onDevicesListUpdate(new ArrayList<Object>(getAvailableDevices()));
        }
    }

    /**
     * Connect to the ecology.
     */
    void connect(Context context, String deviceId) {
        this.deviceId = deviceId;
        connector.connect(context, deviceId);
        if (isDataReference) {
            Log.i(TAG, "setData ");
            ecologyDataSync.setData("Devices", Collections.singletonList(deviceId));
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
    public String getDeviceId() {
        return deviceId;
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
            ecologyDataSync.onMessage(message);
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
        message.setSource(deviceId);
        connector.sendMessage(message);
    }


    /**
     * Send a data sync message to the other devices of the ecology
     *
     * @param message the content of the message
     */
    private void onEcologyDataSyncMessage(EcologyMessage message) {
        message.addArgument(SYNC_DATA_MESSAGE_ID);
        message.setSource(deviceId);
        connector.sendMessage(message);
    }

    /**
     * @return the list of available devices in the ecology
     */
    public List<String> getAvailableDevices() {
        List<String> devicesList = new ArrayList<>((Collection<? extends String>)
                ecologyDataSync.getData("Devices"));
        Iterator<String> iterator = devicesList.iterator();

        // Remove it's own device id
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (key.equals(deviceId)) {
                iterator.remove();
            }
        }
        return devicesList;
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
            room = roomFactory.createRoom(roomName, this, isDataReference);
            rooms.put(roomName, room);
        }
        return room;
    }

    static class RoomFactory {
        public Room createRoom(String roomName, Ecology ecology, Boolean isDataReference) {
            return new Room(roomName, ecology, isDataReference);
        }
    }
}
