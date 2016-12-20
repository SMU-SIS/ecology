package sg.edu.smu.ecology.encoding;

import java.util.List;

/**
 * Created by Quentin ROY on 15/12/16.
 *
 * Decode byte arrays (usually created by {@link MessageEncoder} into messages (i.e. lists of
 * objects).
 */
public class MessageDecoder {
    private DataDecoder decoder = new DataDecoder();

    /**
     * Decode a byte array into a message.
     *
     * @param data The byte array
     * @param length (optional) The amount of byte to read from the byte array.
     * @return The message (a list of objects).
     */
    public List<Object> decode(byte[] data, int length) {
        return decoder.convertMessageArgs(data, length);
    }

    /**
     * Decode a byte array into a message.
     *
     * @param data The byte array
     * @return The message (a list of objects).
     */
    public List<Object> decode(byte[] data){
        return decode(data, data.length);
    }
}
