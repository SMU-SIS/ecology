package sg.edu.smu.ecology;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public Connector getConnector() {
        return connector;
    }

    /**
     * A map storing the different rooms of the ecology.
     */
    private Map<String, Room> rooms = new HashMap<>();

    public Room getRoomsFromName(String name) {
        return rooms.get(name);
    }

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
            public void onMessage(List<Object> message) {
                Ecology.this.onConnectorMessage(message);
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
     * @param message the message content
     */
    private void onConnectorMessage(List<Object> message) {
        String targetRoomName;
        try {
            targetRoomName = (String) message.get(message.size() - 1);
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unrecognized message format.");
        }
        Room room = rooms.get(targetRoomName);
        if (room != null) {
            room.onMessage(message.subList(0, message.size() - 1));
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
     * @param message  the content of the message
     */
    void onRoomMessage(String roomName, List<Object> message) {
        List<Object> msg = new ArrayList<>(message);
        msg.add(roomName);
        connector.sendMessage(msg);
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
