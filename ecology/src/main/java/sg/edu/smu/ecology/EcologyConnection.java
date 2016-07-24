package sg.edu.smu.ecology;

import android.content.Context;

import java.util.List;
import java.util.Vector;

/**
 * Created by Quentin ROY (quentin@quentinroy.fr) on 18/6/16.
 * <p/>
 * This class is responsible for the connection to the other devices that are part of the ecology.
 * In this goal, it uses different connectors. Connectors abstracts the way messages are sent
 * through the different supported network protocols.
 */
public class EcologyConnection extends BaseConnector {

    private final static String TAG = EcologyConnection.class.getSimpleName();

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
        connector.addReceiver(new CoreConnectorReceiver(connector));
    }

    /**
     * Adds a dependent connector to the ecology connection.
     * <p/>
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
        connector.addReceiver(new DependentConnectorReceiver(connector));
    }

    /**
     * Called when a message is received from a core node.
     *
     * @param message the message data content.
     */
    private void onCoreMessage(List<Object> message) {
        // TODO
    }

    /**
     * Called when a message is received from a dependent node.
     *
     * @param message the message data content.
     */
    private void onDependentMessage(List<Object> message) {
        passMessageToReceiver(message);
    }

    /**
     * Send a message to the other devices part of the ecology.
     */
    @Override
    public void sendMessage(List<Object> message) {

        for(int i = 0; i< coreConnectorList.size(); i++){
            coreConnectorList.get(i).sendMessage(message);

        }

        for(int i = 0; i< dependentConnectorList.size(); i++){
            dependentConnectorList.get(i).sendMessage(message);
        }

    }

    /**
     * Connect to the ecology.
     */
    public void connect(Context activity) {
        // Connect all connectors.
        for (Connector connector : dependentConnectorList) {
            connector.connect(activity);
        }
        for (Connector connector : coreConnectorList) {
            connector.connect(activity);
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
     * @return true if every connector are connected.
     */
    public boolean isConnected() {
        for (Connector connector : dependentConnectorList) {
            if (!connector.isConnected()) {
                return false;
            }
        }
        for (Connector connector : coreConnectorList) {
            if (!connector.isConnected()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Called when a connector has been disconnected.
     *
     * @param connector
     */
    private void onConnectorConnected(Connector connector) {
        // TODO
    }

    /**
     * Called when a connector is connected.
     *
     * @param connector
     */
    private void onConnectorDisconnected(Connector connector) {
        // TODO
    }

    /**
     * Base class for the inner connector receivers.
     * <p/>
     * Forward {@link Connector.Receiver#onConnectorConnected()} and
     * {@link Connector.Receiver#onConnectorDisconnected()} to
     * {@link #onConnectorConnected(Connector)} and {@link #onConnectorDisconnected(Connector)}
     */
    private abstract class ConnectorReceiver implements Connector.Receiver {
        public final Connector connector;

        ConnectorReceiver(Connector connector) {
            this.connector = connector;
        }

        @Override
        public void onConnectorConnected() {
            EcologyConnection.this.onConnectorConnected(connector);
        }

        @Override
        public void onConnectorDisconnected() {
            EcologyConnection.this.onConnectorDisconnected(connector);
        }
    }

    /**
     * Dependent node connector receiver inner class.
     * <p/>
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
     * <p/>
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
