package sg.edu.smu.ecology;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * Created by Quentin ROY (quentin@quentinroy.fr) on 18/6/16.
 * <p>
 * This class is responsible for the connection to the other devices that are part of the ecology.
 * In this goal, it uses different connectors. Connectors abstracts the way messages are sent
 * through the different supported network protocols.
 */
public class EcologyConnection extends BaseConnector {

    private final static String TAG = EcologyConnection.class.getSimpleName();

    // Registers if the ecology connection is connected.
    private boolean isConnected = false;

    /**
     * List of core node connectors, see {@link #addCoreConnector(Connector)}.
     */
    private List<Connector> coreConnectorList = new Vector<>();

    /**
     * List of dependent node connectors, see {@link #addDependentConnector(Connector)}.
     */
    private List<Connector> dependentConnectorList = new Vector<>();

    /**
     * Adds a core connector to the ecology connection.
     * Core nodes are supposed to be all linked together. Message received from a core has also be
     * send to all the other cores.
     *
     * @param connector the connector to add.
     */
    public void addCoreConnector(Connector connector) {
        coreConnectorList.add(connector);
        // Add an inner receiver to the connector to forward the connector's messages
        // to this instance appropriate handlers.
        connector.setReceiver(new CoreConnectorReceiver(connector));
    }

    /**
     * Adds a dependent connector to the ecology connection.
     * <p>
     * Every message that is received from a dependent node need to be forwarded to the other
     * devices, and each message coming from any devices need to be forwarded to the every
     * dependent nodes.
     *
     * @param connector the connector to add.
     */
    public void addDependentConnector(Connector connector) {
        dependentConnectorList.add(connector);
        // Add an inner receiver to the connector to forward the connector's messages
        // to this instance appropriate handlers.
        connector.setReceiver(new DependentConnectorReceiver(connector));
    }

    /**
     * Called when a message is received from a core node.
     *
     * @param message the message data content.
     */
    private void onCoreMessage(List<Object> message) {
        List<Object> msg = new ArrayList<>(message);
        getReceiver().onMessage(msg);

        List<Object> dependentMessage = Collections.unmodifiableList(msg);
        // Forwards the message to all the dependent devices
        for (Connector dependentConnector : dependentConnectorList) {
            dependentConnector.sendMessage(dependentMessage);
        }
    }

    /**
     * Called when a message is received from a dependent node.
     *
     * @param message the message data content.
     */
    private void onDependentMessage(List<Object> message) {
        List<Object> msg = new ArrayList<>(message);
        getReceiver().onMessage(msg);

        List<Object> coreMessage = Collections.unmodifiableList(msg);
        // Forward the dependent messages to all the connected core devices
        for (Connector coreConnector : coreConnectorList) {
            coreConnector.sendMessage(coreMessage);
        }
    }

    /**
     * Send a message to the other devices part of the ecology.
     */
    @Override
    public void sendMessage(List<Object> message) {
        // Send message to all the connected core devices
        for (Connector coreConnector : coreConnectorList) {
            coreConnector.sendMessage(message);
        }

        // Send message to all the connected dependent devices
        for (Connector dependentConnector : dependentConnectorList) {
            dependentConnector.sendMessage(message);
        }
    }

    /**
     * Connect to the ecology.
     */
    public void connect(Context context, String deviceId) {
        // Connect all connectors.
        for (Connector connector : dependentConnectorList) {
            connector.connect(context, deviceId);
        }
        for (Connector connector : coreConnectorList) {
            connector.connect(context, deviceId);
        }
    }

    /**
     * Disconnect from the ecology.
     */
    public void disconnect() {
        // Disconnect all connectors.
        for (Connector connector : dependentConnectorList) {
            connector.disconnect();
        }
        for (Connector connector : coreConnectorList) {
            connector.disconnect();
        }
    }

    /**
     * @return true if all connectors are connected.
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Base class for the inner connector receivers.
     * <p>
     * Forward {@link Connector.Receiver#onDeviceConnected(String deviceId)} and
     * {@link Connector.Receiver#onDeviceDisconnected(String deviceId)} to
     * {@link #onDeviceConnected(Integer)} )} and {@link #onDeviceDisconnected(Integer)}
     */
    private abstract class ConnectorReceiver implements Connector.Receiver {
        public final Connector connector;

        ConnectorReceiver(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onDeviceConnected(String deviceId) {
            EcologyConnection.this.onDeviceConnected(deviceId);

        }

        @Override
        public void onDeviceDisconnected(String deviceId) {
            EcologyConnection.this.onDeviceDisconnected(deviceId);
        }
    }

    /**
     * Called when a device is connected
     *
     * @param deviceId the id of the device that got connected
     */
    private void onDeviceConnected(String deviceId) {
        getReceiver().onDeviceConnected(deviceId);
    }

    /**
     * Called when a device gets disconnected
     *
     * @param deviceId the id of the device that got disconnected
     */
    private void onDeviceDisconnected(String deviceId) {
        getReceiver().onDeviceDisconnected(deviceId);
    }

    /**
     * Dependent node connector receiver inner class.
     * <p>
     * Forward {@link Connector.Receiver#onMessage(List<Object>)} to
     * {@link #onDependentMessage(List<Object>)}.
     */
    private class DependentConnectorReceiver extends ConnectorReceiver {
        DependentConnectorReceiver(Connector connector) {
            super(connector);
        }

        @Override
        public void onMessage(List<Object> message) {
            onDependentMessage(message);
        }
    }

    /**
     * Core node connector receiver inner class.
     * <p>
     * Forward {@link Connector.Receiver#onMessage(List<Object>)} to
     * {@link #onCoreMessage(List<Object>)}.
     */
    private class CoreConnectorReceiver extends ConnectorReceiver {
        CoreConnectorReceiver(Connector connector) {
            super(connector);
        }

        @Override
        public void onMessage(List<Object> message) {
            onCoreMessage(message);
        }
    }
}
