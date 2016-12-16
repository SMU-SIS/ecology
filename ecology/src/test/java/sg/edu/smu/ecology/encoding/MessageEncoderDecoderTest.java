package sg.edu.smu.ecology.encoding;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import sg.edu.smu.ecology.encoding.MessageData;
import sg.edu.smu.ecology.encoding.MessageDecoder;
import sg.edu.smu.ecology.encoding.MessageEncoder;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;


/**
 * Created by Quentin ROY on 15/12/16.
 */
public class MessageEncoderDecoderTest {
    private MessageEncoder encoder;
    private MessageDecoder decoder;

    @Before
    public void setUp() throws Exception {
        encoder = new MessageEncoder();
        decoder = new MessageDecoder();
    }

    @Test
    public void emptyMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.emptyList());
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(0, decodedMessage.size());
    }

    @Test
    public void oneIntMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(5));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals(5, decodedMessage.get(0));
    }

    @Test
    public void oneFloatMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(4.5f));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals(4.5f, decodedMessage.get(0));
    }

    @Test
    public void oneDoubleMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(4.5));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals(4.5, decodedMessage.get(0));
    }

    @Test
    public void oneTrueBooleanMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(true));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals(true, decodedMessage.get(0));
    }

    @Test
    public void oneFalseBooleanMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(false));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals(false, decodedMessage.get(0));
    }

    @Test
    public void oneASCIIStringMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(
                Collections.<Object>singletonList("simple easy string")
        );
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals("simple easy string", decodedMessage.get(0));
    }

    @Test
    public void oneUnicodeStringMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(
                Collections.<Object>singletonList("%ïô${å}thing[ò]#@«|")
        );
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals("%ïô${å}thing[ò]#@«|", decodedMessage.get(0));
    }

    @Test
    public void oneByteArrayEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(
                Collections.<Object>singletonList(new byte[]{
                        (byte)0xe0, 0x4f, (byte)0xd0, 0x20, (byte)0xea, 0x3a, 0x69, 0x10,
                        (byte)0xa2, (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte)0x9d
                })
        );
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertArrayEquals(
                new byte[]{(byte)0xe0, 0x4f, (byte)0xd0, 0x20, (byte)0xea, 0x3a, 0x69, 0x10,
                        (byte)0xa2, (byte)0xd8, 0x08, 0x00, 0x2b, 0x30, 0x30, (byte)0x9d},
                (byte[]) decodedMessage.get(0)
        );
    }

    @Test
    public void oneCharArrayEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList('c'));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals('c', decodedMessage.get(0));
    }

    @Test
    public void oneUnicodeCharArrayEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList('î'));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(1, decodedMessage.size());
        assertEquals('î', decodedMessage.get(0));
    }

    @Test(expected=MessageData.UnsupportedDataTypeException.class)
    public void unsupportedTypeThrows() {
        MessageData data = new MessageData();
        data.addArgument(new Object());
    }

    @Test
    public void manyThingsAtOnceEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Arrays.<Object>asList(
            8, new byte[]{0, -120, 123, 8, 4}, "something", 4.6, 5, '0', 3f, false, true, "îIø",
            'z', 'å', '7'
        ));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertEquals(13, decodedMessage.size());
        assertEquals(8, decodedMessage.get(0));
        assertArrayEquals(new byte[]{0, -120, 123, 8, 4}, (byte[])decodedMessage.get(1));
        assertEquals("something", decodedMessage.get(2));
        assertEquals(4.6, decodedMessage.get(3));
        assertEquals(5, decodedMessage.get(4));
        assertEquals('0', decodedMessage.get(5));
        assertEquals(3f, decodedMessage.get(6));
        assertEquals(false, decodedMessage.get(7));
        assertEquals(true, decodedMessage.get(8));
        assertEquals("îIø", decodedMessage.get(9));
        assertEquals('z', decodedMessage.get(10));
        assertEquals('å', decodedMessage.get(11));
        assertEquals('7', decodedMessage.get(12));
    }


}
