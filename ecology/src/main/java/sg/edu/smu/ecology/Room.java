package sg.edu.smu.ecology;

import java.util.List;

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
    
    private List<Object> message;

    /**
     * @param name    the name of the room
     * @param ecology the ecology this room is part of
     */
    public Room(String name, Ecology ecology) {

        if(name == null || name.length() == 0){
            throw new IllegalArgumentException();
        }

        this.name = name;
        this.ecology = ecology;
        this.eventBroadcaster = new EventBroadcaster(this);
    }

    /**
     * @return the event broadcaster associated with the room.
     */
    public EventBroadcaster getEventBroadcaster() {
        if(eventBroadcaster == null){
            throw new NullPointerException();
        }
        return eventBroadcaster;
    }

    /**
     * Handle message from the room.
     *
     * @param message the content of the message
     */
    void onMessage(List<Object> message) {
        // Currently, only event broadcaster messages are supported.
        eventBroadcaster.onRoomMessage(message);
    }

    /**
     * Forward the message coming from the event broadcaster to the ecology.
     *
     * @param message the message
     */
    void onEventBroadcasterMessage(List<Object> message) {
        this.message = message;
        ecology.onRoomMessage(name, message);
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

    public String getRoomName() {
        return name;
    }

    public Ecology getEcology() {
        return ecology;
    }

    public List<Object> getMessage() {
        return message;
    }
}
