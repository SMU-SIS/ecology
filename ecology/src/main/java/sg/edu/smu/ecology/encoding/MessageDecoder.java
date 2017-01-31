package sg.edu.smu.ecology.encoding;

import sg.edu.smu.ecology.EcologyMessage;

/**
 * Created by Quentin ROY on 15/12/16.
 * <p>
 * Decode byte arrays (usually created by {@link MessageEncoder} into messages (i.e. lists of
 * objects).
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
        return new EcologyMessage(decoder.convertMessageArgs(data, length));
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
