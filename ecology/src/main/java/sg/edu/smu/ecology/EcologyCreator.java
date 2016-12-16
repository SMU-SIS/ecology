package sg.edu.smu.ecology;

import android.content.Context;

/**
 * Created by anurooppv on 1/8/2016.
 * This class is used to get Ecology instance required to connect and send messages to other devices
 * <p>
 */
public class EcologyCreator{

    private static final String TAG = EcologyCreator.class.getSimpleName();
    private static Ecology ecology;

    // Initial connection to the ecology - returns the ecology instance
    public static Ecology connect(EcologyConfig config, Context context, String deviceId) {
        // Ecology can be only connected once
        if (ecology != null) {
            throw new EcologyAlreadyConnectedException("The ecology has already been connected.");
        }

        EcologyConnection ecologyConnection = new EcologyConnection();

        // Adds a core connector to the ecology connection.
        for (Connector coreConnector : config.getCoreConnectors()) {
            ecologyConnection.addCoreConnector(coreConnector);
        }

        // Adds a dependent connector to the ecology connection.
        for (Connector dependentConnector : config.getDependentConnectors()) {
            ecologyConnection.addDependentConnector(dependentConnector);
        }

        ecology = new Ecology(ecologyConnection);
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
