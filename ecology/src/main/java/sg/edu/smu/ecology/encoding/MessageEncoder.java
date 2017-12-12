package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;

import sg.edu.smu.ecology.EcologyMessage;

/**
 * Encode messages (list of objects) into byte arrays in a format decodable by a
 * {@link MessageDecoder}.
 *
 * @author Quentin ROY
 * @author Anuroop PATTENA VANIYAR
 */
public class MessageEncoder {

    // The data encoder used internally to encode a message.
    private DataEncoder encoder = new DataEncoder();

    /**
     * @param message The ecology message to encode.
     * @return The message encoded.
     * @throws CharacterCodingException
     */
    public byte[] encode(EcologyMessage message) throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        for (Object obj : message.getArguments()) {
            data.addArgument(obj);
        }
        data.addArgument(message.getSource());
        data.addArgument(message.getTargets());
        data.addArgument(message.getTargetType());

        // Allocate an auto-expending buffer to receive the encoded version of the message.
        IoBuffer buffer = IoBuffer.allocate(data.getMaximumByteSize(), false);
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
