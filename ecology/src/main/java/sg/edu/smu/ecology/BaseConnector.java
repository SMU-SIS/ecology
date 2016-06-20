package sg.edu.smu.ecology;


import android.os.Bundle;

import java.util.List;
import java.util.Vector;

/**
 * Created by Quentin ROY on 18/6/16.
 *
 * Base implementation of {@link Connector} with receiver management.
 */
public abstract class BaseConnector implements Connector {

    /**
     * List of receivers that subscribed to the connector.
     */
    private List<Receiver> receivers = new Vector<Receiver>();

    /**
     * @see Connector#addReceiver(Receiver)
     */
    public void addReceiver(Receiver receiver){
        receivers.add(receiver);
    }

    /**
     * Pass a message to all receivers (see {@link Connector.Receiver#onMessage(short, Bundle)}).
     *
     * @param type    the message's type
     * @param message the message's content.
     */
    protected void passMessageToReceiver(short type, Bundle message){
        for(Receiver receiver: receivers){
            receiver.onMessage(type, message);
        }
    }
}
