package sg.edu.smu.ecology;

import android.content.Context;

import sg.edu.smu.ecology.connector.Connector;
import sg.edu.smu.ecology.connector.bluetooth.BluetoothServerConnector;

/**
 * Created by anurooppv on 1/8/2016.
 * This class is used to get Ecology instance required to connect and send messages to other devices
 * <p>
 */
public class EcologyCreator {

    private static final String TAG = EcologyCreator.class.getSimpleName();
    private static Ecology ecology;

    /**
     * Initial connection to the ecology - returns the ecology instance
     *
     * @param connector the connector that connects to the ecology
     * @param context   the context of the application
     * @param deviceId  the id of the device
     * @return the ecology instance
     */
    public static Ecology connect(Connector connector, Context context, String deviceId) {
        // Ecology can be only connected once
        if (ecology != null) {
            throw new EcologyAlreadyConnectedException("The ecology has already been connected.");
        }

        Boolean isDataReference = false;
        if (connector instanceof BluetoothServerConnector) {
            isDataReference = true;
        }

        ecology = new Ecology(connector, isDataReference);
        // Connect to the ecology
        ecology.connect(context, deviceId);

        return ecology;
    }

    // Returns the ecology instance once the ecology is connected
    public static Ecology getEcology() {
        return ecology;
    }

    // Disconnects from the ecology
    public static void disconnect() {
        ecology.disconnect();
        ecology = null;
    }

    protected static class EcologyAlreadyConnectedException extends RuntimeException {
        public EcologyAlreadyConnectedException(String message) {
            super(message);
        }
    }

}
