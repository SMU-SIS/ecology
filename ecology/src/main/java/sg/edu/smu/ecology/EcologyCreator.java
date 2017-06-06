package sg.edu.smu.ecology;

import android.app.Application;
import android.content.Context;

import sg.edu.smu.ecology.connector.Connector;

/**
 * Created by anurooppv on 1/8/2016.
 * This class is used to create the Ecology instance required to get connected. Once connected,
 * this class can be used to retrieve the created ecology instance. Ecology is created only once
 * per application.
 * <p>
 */
public class EcologyCreator {
    private static final String TAG = EcologyCreator.class.getSimpleName();
    private static Ecology ecology;

    /**
     * Connect to the ecology. 
     *
     * @param connector       the connector that connects to the ecology
     * @param context         the context of the application/activity
     * @param deviceId        the id of this device
     * @param isDataReference whether this device should be the data reference or not
     * @param application     the application currently in use
     * @return the ecology instance
     */
    public static Ecology connect(Connector connector, Context context, String deviceId,
                                  Boolean isDataReference, Application application) {
        // Ecology can be only connected once
        if (ecology != null) {
            throw new EcologyAlreadyConnectedException("The ecology has already been connected.");
        }

        ecology = new Ecology(connector, isDataReference);
        // Connect to the ecology
        ecology.connect(context, deviceId, application);

        return ecology;
    }

    /**
     * Get the ecology instance to which the device is currently connected to.
     *
     * @return the ecology instance
     */
    public static Ecology getEcology() {
        return ecology;
    }

    /**
     * Disconnect from the ecology
     */
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
