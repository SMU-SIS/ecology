package sg.edu.smu.ecology;


import java.util.List;
import java.util.Vector;

/**
 * Created by Quentin ROY on 18/6/16.
 * <p/>
 * Base implementation of {@link Connector} with receiver management.
 */
public abstract class BaseConnector implements Connector {

    /**
     * List of receivers that subscribed to the connector.
     */
    private List<Receiver> receivers = new Vector<Receiver>();

    public List<Receiver> getReceivers() {
        return receivers;
    }

    /**
     * @see Connector#addReceiver(Receiver)
     */
    public void addReceiver(Receiver receiver) {
        receivers.add(receiver);
    }

    /**
     * Pass a message to all receivers (see {@link Connector.Receiver#onMessage(List<Object>)}).
     *
     * @param message the message's content.
     */
    protected void passMessageToReceiver(List<Object> message) {
        for (Receiver receiver : receivers) {
            receiver.onMessage(message);
        }
    }
}
