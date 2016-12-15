package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;

import sg.edu.smu.ecology.encoding.DataDecoder;
import sg.edu.smu.ecology.encoding.DataEncoder;
import sg.edu.smu.ecology.encoding.MessageData;


/**
 * Created by Quentin ROY on 15/12/16.
 */
public class DataEncoderDecoderTest {

    private static final int MESSAGE_LENGTH = 1024;
    private DataEncoder encoder;
    private DataDecoder decoder;
    private IoBuffer ioBuffer = IoBuffer.allocate(MESSAGE_LENGTH);

    @Before
    public void setUp() throws Exception {
        encoder = new DataEncoder();
        decoder = new DataDecoder();
    }

    @After
    public void tearDown() {
        ioBuffer.clear();
    }

    @Test
    public void emptyMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        MessageData decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length);
        // Make sure it is the same.
        assertEquals(0, decodedData.getArguments().size());
    }

    @Test
    public void oneIntMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(42);
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals(42, decodedData.get(0));
    }

    @Test
    public void oneFloatMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(4.2f);
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals(4.2f, decodedData.get(0));
    }

    @Test
    public void oneDoubleMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(4.2);
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals(4.2, decodedData.get(0));
    }

    @Test
    public void oneTrueBooleanMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(true);
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals(true, decodedData.get(0));
    }

    @Test
    public void oneFalseBooleanMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(false);
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals(false, decodedData.get(0));
    }

    @Test
    public void oneASCIIStringMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument("simple easy string");
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals("simple easy string", decodedData.get(0));
    }

    @Test
    public void oneUnicodeStringMessageEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument("%ïô${å}thing[ò]#@«|");
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals("%ïô${å}thing[ò]#@«|", decodedData.get(0));
    }

    @Test
    public void oneByteArrayEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(new byte[]{(byte)0xe0, 0x4f, (byte)0xd0, 0x20, (byte)0xea, 0x3a, 0x69,
                0x10, (byte)0xa2, (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte)0x9d});
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertArrayEquals(
                new byte[]{(byte)0xe0, 0x4f, (byte)0xd0, 0x20, (byte)0xea, 0x3a, 0x69, 0x10,
                        (byte)0xa2, (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte)0x9d},
                (byte[]) decodedData.get(0)
        );
    }

    @Test
    public void oneCharArrayEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument('c');
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals('c', decodedData.get(0));
    }

    @Test
    public void oneUnicodeCharArrayEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument('î');
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(1, decodedData.size());
        assertEquals('î', decodedData.get(0));
    }

    @Test(expected=MessageData.UnsupportedDataTypeException.class)
    public void unsupportedTypeThrows() {
        MessageData data = new MessageData();
        data.addArgument(new Object());
    }

    @Test
    public void manyThingsAtOnceEncoding() throws CharacterCodingException {
        // Create the message data.
        MessageData data = new MessageData();
        data.addArgument(8);
        data.addArgument(new byte[]{0, -120, 123, 8, 4});
        data.addArgument("something");
        data.addArgument(4.6);
        data.addArgument(5);
        data.addArgument('0');
        data.addArgument(3f);
        data.addArgument(false);
        data.addArgument(true);
        data.addArgument("îIø");
        data.addArgument('z');
        data.addArgument('å');
        data.addArgument('7');
        // Encode it into the buffer.
        encoder.encodeMessage(data, ioBuffer);
        byte[] encodedMessage = Arrays.copyOfRange(ioBuffer.array(), 0, ioBuffer.position());
        // Decode it.
        List<Object> decodedData = decoder.convertMessage(encodedMessage, encodedMessage.length)
                .getArguments();
        // Make sure it is the same.
        assertEquals(13, decodedData.size());
        assertEquals(8, decodedData.get(0));
        assertArrayEquals(new byte[]{0, -120, 123, 8, 4}, (byte[])decodedData.get(1));
        assertEquals("something", decodedData.get(2));
        assertEquals(4.6, decodedData.get(3));
        assertEquals(5, decodedData.get(4));
        assertEquals('0', decodedData.get(5));
        assertEquals(3f, decodedData.get(6));
        assertEquals(false, decodedData.get(7));
        assertEquals(true, decodedData.get(8));
        assertEquals("îIø", decodedData.get(9));
        assertEquals('z', decodedData.get(10));
        assertEquals('å', decodedData.get(11));
        assertEquals('7', decodedData.get(12));
    }


}
