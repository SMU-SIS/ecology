package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Quentin ROY on 15/12/16.
 *
 * Encode messages (list of objects) into byte arrays in a format decodable by a
 * {@link MessageDecoder}.
 */
public class MessageEncoder {
    // The initial capacity of the auto-expending IoBuffer allocated for the storage of the
    // encoded message.
    private final static int INITIAL_BUFFER_SIZE = 64;

    // The data encoder used internally to encode a message.
    private DataEncoder encoder = new DataEncoder();

    /**
     *
     * @param message The message to encode.
     * @return The message encoded.
     * @throws CharacterCodingException
     */
    public byte[] encode(List<Object> message) throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        for(Object obj: message){
            data.addArgument(obj);
        }
        // Allocate an auto-expending buffer to receive the encoded version of the message.
        IoBuffer buffer = IoBuffer.allocate(INITIAL_BUFFER_SIZE, false);
        buffer.setAutoExpand(true);
        try {
            // Encode the message into the buffer.
            encoder.encodeMessage(data, buffer);
            return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.position());
        } finally {
            // Make sure the buffer is always freed even if the encoding did not work.
            buffer.free();
        }
    }
}
