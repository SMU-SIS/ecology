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
    private Receiver receiver;

    protected Receiver getReceiver() {
        return receiver;
    }

    /**
     * @see Connector#setReceiver(Receiver)
     */
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }
}
