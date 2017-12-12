package sg.edu.smu.ecology.connector;

import android.content.Context;

import sg.edu.smu.ecology.EcologyMessage;

/**
 * Main interface for connectors. Connector abstracts the way a message can be sent to other
 * devices.
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
 */
public interface Connector {

    /**
     * Send a message through the connector.
     *
     * @param message the content of the message
     */
    public void sendMessage(EcologyMessage message);

    /**
     * Set the receiver of the connector.
     *
     * @param receiver
     */
    public void setReceiver(Receiver receiver);

    /**
     * Ask the connector to connect itself.
     *
     * @param context
     * @param deviceId
     */
    public void connect(Context context, String deviceId);

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
         * @param message the content of the message
         */
        public void onMessage(EcologyMessage message);

        /**
         * Receive the device connection notifications
         *
         * @param deviceId the id of the device that got connected
         */
        public void onDeviceConnected(String deviceId);

        /**
         * Receive the device disconnection notifications
         *
         * @param deviceId the id of the device that got disconnected
         */
        public void onDeviceDisconnected(String deviceId);

        /**
         * Receive the ecology connection notification
         */
        public void onConnected();

        /**
         * Receive the ecology disconnection notification
         */
        public void onDisconnected();

    }

}
