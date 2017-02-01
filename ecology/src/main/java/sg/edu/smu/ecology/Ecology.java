package sg.edu.smu.ecology;


import android.content.Context;
import android.util.Log;

import java.util.HashMap;
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
     * @param ecologyConnector the connector used to send messages to the other devices of the
     *                         ecology.
     */
    public Ecology(Connector ecologyConnector) {
        this(new RoomFactory(), ecologyConnector);
    }

    /**
     * Special constructor only for testing
     *
     * @param roomFactory to create rooms part of this ecology
     * @param connector   the connector used to send messages to the other devices of the
     *                    ecology.
     */
    Ecology(RoomFactory roomFactory, Connector connector) {
        this.roomFactory = roomFactory;
        this.connector = connector;

        this.connector.setReceiver(new Connector.Receiver() {

            @Override
            public void onMessage(EcologyMessage message) {
                Ecology.this.onConnectorMessage(message);
            }

            @Override
            public void onDeviceConnected(String deviceId) {
                Ecology.this.onDeviceConnected(deviceId);
            }

            @Override
            public void onDeviceDisconnected(String deviceId) {
                Ecology.this.onDeviceDisconnected(deviceId);
            }
        });
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId the id of the device that got connected
     */
    private void onDeviceConnected(String deviceId) {
        for (Room room : rooms.values()) {
            room.onDeviceConnected(deviceId);
        }
    }

    /**
     * Called when a device is disconnected.
     *
     * @param deviceId the id of the device that got disconnected
     */
    private void onDeviceDisconnected(String deviceId) {
        for (Room room : rooms.values()) {
            room.onDeviceDisconnected(deviceId);
        }
    }

    /**
     * Connect to the ecology.
     */
    void connect(Context context, String deviceId) {
        this.deviceId = deviceId;
        connector.connect(context, deviceId);
    }

    /**
     * Disconnect from the ecology.
     */
    void disconnect() {
        connector.disconnect();
    }

    /**
     * Receive messages from the other devices of the ecology.
     *
     * @param message the message content
     */
    private void onConnectorMessage(EcologyMessage message) {
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
        message.setSource(deviceId);
        connector.sendMessage(message);
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
            room = roomFactory.createRoom(roomName, this);
            rooms.put(roomName, room);
        }
        return room;
    }

    static class RoomFactory {
        public Room createRoom(String roomName, Ecology ecology) {
            return new Room(roomName, ecology);
        }
    }
}
