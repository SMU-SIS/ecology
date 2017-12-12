package sg.edu.smu.ecology;

import android.app.Application;
import android.content.Context;

import sg.edu.smu.ecology.connector.Connector;

/**
 * This class is used to create the Ecology instance. The device can be connected to the Ecology to
 * send events and sync data across other connected devices in the ecology. This class also helps to
 * disconnect from the ecology when required.
 *
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
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
     * Get the ecology instance. The ecology is created only once per application. Hence this method
     * is used to get the already created ecology instance.
     * @return the ecology instance
     */
    public static Ecology getEcology() {
        return ecology;
    }

    /**
     * Disconnect from ecology. This disconnects this device from ecology and hence from the devices
     * it was connected to.
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
