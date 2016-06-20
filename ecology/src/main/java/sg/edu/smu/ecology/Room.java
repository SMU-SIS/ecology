package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;

/**
 * Created by Quentin ROY on 20/6/16.
 * <p/>
 * Represent an ecology space dedicated to a same set of functionalities (e.g. an application).
 */
public class Room {

    private final static String TAG = Room.class.getSimpleName();

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
     * @param name    the name of the room
     * @param ecology the ecology this room is part of
     */
    public Room(String name, Ecology ecology) {
        this.name = name;
        this.ecology = ecology;
        this.eventBroadcaster = new EventBroadcaster(this);
    }

    /**
     * @return the event broadcaster associated with the room.
     */
    public EventBroadcaster getEventBroadcaster() {
        return eventBroadcaster;
    }

    /**
     * Handle message from the room.
     *
     * @param type    the type of message
     * @param message the content of the message
     */
    void onMessage(short type, Bundle message) {
        if (type == MessageTypes.EVENT) {
            // Sent the event to the broadcaster.
            eventBroadcaster.onRoomMessage(type, message);
        } else {
            Log.w(TAG, "Unknown message type: " + type + ".");
        }
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology.
     *
     * @param type    the message type
     * @param message the message
     */
    void onEventBroadcasterMessage(short type, Bundle message){
        ecology.onRoomMessage(name, type, message);
    }

    /**
     * Called when the room connector is connected.
     */
    private void onConnectorConnected() {
        // TODO
    }

    /**
     * Called when the room connector is disconnected.
     */
    private void onConnectorDisconnected() {
        // TODO
    }
}
