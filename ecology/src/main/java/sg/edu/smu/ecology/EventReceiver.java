/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */


package sg.edu.smu.ecology;

import java.util.List;

/**
 * Interface for objects able to receive ecology events. The events can be local as well as from
 * other connected devices in the ecology.
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
 */
public interface EventReceiver {

    /**
     * Handle the events.
     *
     * @param eventType the type of the event
     * @param eventData the data of the event
     */
    public void handleEvent(String eventType, List<Object> eventData);
}