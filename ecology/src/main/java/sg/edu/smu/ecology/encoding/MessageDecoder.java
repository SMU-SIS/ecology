/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */

package sg.edu.smu.ecology.encoding;

import java.util.List;

import sg.edu.smu.ecology.EcologyMessage;

/**
 * Decode byte arrays (usually created by {@link MessageEncoder} into messages (i.e. lists of
 * objects).
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
 */
public class MessageDecoder {
    private DataDecoder decoder = new DataDecoder();

    /**
     * Decode a byte array into a message.
     *
     * @param data   The byte array
     * @param length (optional) The amount of byte to read from the byte array.
     * @return The ecology message.
     */
    public EcologyMessage decode(byte[] data, int length) {
        EcologyMessage message = new EcologyMessage(decoder.convertMessageArgs(data, length));
        message.setTargetType((Integer) message.fetchArgument());
        message.setTargets((List<String>) message.fetchArgument());
        message.setSource((String) message.fetchArgument());
        return message;
    }

    /**
     * Decode a byte array into a message.
     *
     * @param data The byte array
     * @return The ecology message.
     */
    public EcologyMessage decode(byte[] data) {
        return decode(data, data.length);
    }
}
