/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package sg.edu.smu.ecology.connector;

/**
 * Base implementation of {@link Connector} with receiver management.
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
 */
public abstract class BaseConnector implements Connector {

    /**
     * List of receivers that subscribed to the connector.
     */
    private Receiver receiver;

    protected Receiver getReceiver() {
        return receiver;
    }

    /**
     * @see Connector#setReceiver(Receiver)
     */
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }
}
