package sg.edu.smu.ecology;


import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anuroop PATTENA VANIYAR on 1/6/2016.
 * <p/>
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
     * A map storing the different rooms of the ecology.
     */
    private Map<String, Room> rooms = new HashMap<>();

    /**
     * @param ecologyConnector the connector used to send messages to the other devices of the
     *                         ecology.
     */
    public Ecology(Connector ecologyConnector) {
        this.connector = ecologyConnector;

        // Use an intermediate receiver to allows private {@link onMessage},
        // {@link onConnectorConnected} and {@link onConnectorDisconnected} on the ecology instance.
        this.connector.addReceiver(new Connector.Receiver() {

            @Override
            public void onMessage(short type, Bundle message) {
                Ecology.this.onConnectorMessage(type, message);
            }

            @Override
            public void onConnectorConnected() {
                Ecology.this.onConnectorConnected();
            }

            @Override
            public void onConnectorDisconnected() {
                Ecology.this.onConnectorDisconnected();
            }
        });
    }

    /**
     * Receive messages from the other devices of the ecology.
     *
     * @param type    the type of message
     * @param message the message content
     */
    private void onConnectorMessage(short type, Bundle message) {
        // Check how should be handled the message.
        if (type == MessageTypes.EVENT) {
            // In case of room message fetch the target room and forward it the message.
            String targetRoomName = message.getString("room");
            Room room = rooms.get(targetRoomName);
            if (room != null) {
                room.onMessage(type, message);
            }
        } else {
            Log.w(TAG, "Unsupported message type: " + type);
        }
    }

    /**
     * Called when the ecology connector is connected.
     */
    private void onConnectorConnected() {
        // TODO
    }

    /**
     * Called when the ecology connector is disconnected.
     */
    private void onConnectorDisconnected() {
        // TODO
    }

    /**
     * Send a message to the other devices of the ecology from a specific room.
     *
     * @param roomName the name of the room who send the event
     * @param type     the type of message to send
     * @param message  the content of the message
     */
    void onRoomMessage(String roomName, short type, Bundle message) {
        message.putString("room", roomName);
        connector.sendMessage(type, message);
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
            room = new Room(roomName, this);
            rooms.put(roomName, room);
        }
        return room;
    }
}
