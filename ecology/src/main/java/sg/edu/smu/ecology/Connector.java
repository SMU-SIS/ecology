package sg.edu.smu.ecology;

import android.os.Bundle;

/**
 * Created by Quentin ROY on 17/6/16.
 * <p/>
 * Main interface for connectors. Connector abstracts the way a message can be sent to other
 * devices.
 */
public interface Connector {

    /**
     * Send a message through the connector.
     *
     * @param type    the type of the message to send
     * @param message the content of the message
     */
    public void sendMessage(short type, Bundle message);

    /**
     * Add a receiver to the connector.
     *
     * @param receiver
     */
    public void addReceiver(Receiver receiver);

    /**
     * Ask the connector to connect itself.
     */
    public void connect();

    /**
     * Request a disconnection.
     */
    public void disconnect();

    /**
     * @return true if the connector is currently connected.
     */
    public boolean isConnected();

    /**
     * Interface for the receiver of the connector: i.e. the object able to handle the messages
     * received by a connector.
     */
    public interface Receiver {

        /**
         * Handle a connector message.
         *
         * @param type    the type of the message that has been received
         * @param message the content of the message
         */
        public void onMessage(short type, Bundle message);

        /**
         * Receive the connection notifications.
         */
        public void onConnectorConnected();

        /**
         * Receive the disconnection notifications.
         */
        public void onConnectorDisconnected();
    }

}
