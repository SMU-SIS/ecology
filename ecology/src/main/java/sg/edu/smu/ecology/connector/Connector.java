package sg.edu.smu.ecology.connector;

import android.content.Context;

import sg.edu.smu.ecology.EcologyMessage;

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
         * @param isDataReference if the device is the data reference or not
         */
        public void onDeviceConnected(String deviceId, Boolean isDataReference);

        /**
         * Receive the device disconnection notifications
         *
         * @param deviceId the id of the device that got disconnected
         * @param isDataReference if the device is the data reference or not
         */
        public void onDeviceDisconnected(String deviceId, Boolean isDataReference);

    }

}
