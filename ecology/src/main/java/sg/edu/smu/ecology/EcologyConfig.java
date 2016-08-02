package sg.edu.smu.ecology;

import java.util.List;
import java.util.Vector;

/**
 * Created by anurooppv on 2/8/2016.
 * <p>
 * This class is used for configuring ecology connectors before connecting to the ecology. Required
 * connectors can be added - core connector as well as dependent connector.
 */
public class EcologyConfig {
    private static final String TAG = EcologyConfig.class.getSimpleName();

    /**
     * List of core connectors
     */
    private List<Connector> coreConnectors = new Vector<>();

    /**
     * List of dependent connectors
     */
    private List<Connector> dependentConnectors = new Vector<>();

    /**
     * Adds a core connector to the ecology connection.
     */
    public void addCoreConnector(Connector connector) {
        coreConnectors.add(connector);
    }

    /**
     * Adds a dependent connector to the ecology connection.
     */
    public void addDependentConnector(Connector connector) {
        dependentConnectors.add(connector);
    }

    /**
     * Returns the list of core connectors
     */
    public List<Connector> getCoreConnectors() {
        return coreConnectors;
    }

    /**
     * Returns the list of dependent connectors
     */
    public List<Connector> getDependentConnectors() {
        return dependentConnectors;
    }

}
