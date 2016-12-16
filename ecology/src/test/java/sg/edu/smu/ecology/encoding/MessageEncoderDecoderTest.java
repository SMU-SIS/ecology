package sg.edu.smu.ecology.encoding;

import org.junit.Before;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;


/**
 * Created by Quentin ROY on 15/12/16.
 *
 * Tests for {@link MessageEncoder} and {@link MessageDecoder}.
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
        assertThat(decodedMessage).isEmpty();
    }

    @Test
    public void oneIntMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(5));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly(5);
    }

    @Test
    public void oneFloatMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(4.5f));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly(4.5f);
    }

    @Test
    public void oneDoubleMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(4.5));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly(4.5);
    }

    @Test
    public void oneTrueBooleanMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(true));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly(true);
    }

    @Test
    public void oneFalseBooleanMessageEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList(false));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly(false);
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
        assertThat(decodedMessage).containsExactly("simple easy string");
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
        assertThat(decodedMessage).containsExactly("%ïô${å}thing[ò]#@«|");
    }

    @Test
    public void oneByteArrayEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(
                Collections.<Object>singletonList(new byte[]{
                        (byte)0xe0, (byte)0x4f, (byte)0xd0, (byte)0x20, (byte)0xea, (byte)0x3a, (byte)0x69,
                        (byte)0x10, (byte)0xa2, (byte)0xd8, (byte)0x08, (byte)0x00, (byte)0x2b, (byte)0x30,
                        (byte)0x30, (byte)0x9d
                })
        );
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).hasSize(1);
        assertThat(decodedMessage.get(0)).isInstanceOf(byte[].class);
        assertThat((byte[]) decodedMessage.get(0)).asList().containsExactly(
                (byte)0xe0, (byte)0x4f, (byte)0xd0, (byte)0x20, (byte)0xea, (byte)0x3a, (byte)0x69,
                (byte)0x10, (byte)0xa2, (byte)0xd8, (byte)0x08, (byte)0x00, (byte)0x2b, (byte)0x30,
                (byte)0x30, (byte)0x9d
        ).inOrder();
    }

    @Test
    public void oneCharEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList('c'));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly('c');
    }

    @Test
    public void oneUnicodeCharEncoding() throws CharacterCodingException {
        // Encode the message.
        byte[] encodedMessage = encoder.encode(Collections.<Object>singletonList('î'));
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).containsExactly('î');
    }

    @Test
    public void oneNullEncoding() throws CharacterCodingException {
        List<Object> message = Collections.singletonList(null);
        // Encode the message.
        byte[] encodedMessage = encoder.encode(message);
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).isEqualTo(message);
    }

    @Test
    public void oneEmptyListEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(Collections.emptyList());
        // Encode the message.
        byte[] encodedMessage = encoder.encode(message);
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).isEqualTo(message);
    }

    @Test
    public void oneListEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(Arrays.asList(
                5, 4, "hello", 2.5, true, 'c', "ˆˆø"
        ));
        // Encode the message.
        byte[] encodedMessage = encoder.encode(message);
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).isEqualTo(message);
    }

    @Test(expected=MessageData.UnsupportedDataTypeException.class)
    public void unsupportedTypeThrows() {
        MessageData data = new MessageData();
        data.addArgument(new Object());
    }

    @Test
    public void nestedListsEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(
                Arrays.asList(
                        Arrays.asList(
                                "ouch",
                                Arrays.asList(
                                        Arrays.asList(4, 'c'),
                                        0
                                ),
                                Collections.emptyList()
                        ),
                        2.5
                )
        );
        // Encode the message.
        byte[] encodedMessage = encoder.encode(message);
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).isEqualTo(message);
    }

    @Test
    public void manyThingsAtOnceEncoding() throws CharacterCodingException {
        List<Object> message = Arrays.asList(
                8, "something",
                Arrays.asList(4, 0.4, 'k'),
                0, 4.6, 5, '0',
                Arrays.asList(3, "stuff", Arrays.asList(2, 6.3, true), 2),
                'c',
                Arrays.asList(
                        new byte[]{0, -120, 123, 8, 4},
                        Arrays.asList(
                                "ouch", Arrays.asList(
                                        Arrays.asList(4, 'c'), 0
                                )
                        ), 2.5
                ),
                "îIø", 'z', 'å', '7'
        );
        // Encode the message.
        byte[] encodedMessage = encoder.encode(message);
        // Decode it.
        List<Object> decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage).hasSize(message.size());
        // Check the nested list containing the byte array first. Arrays are an issue as
        // their memory addresses are compared instead of their contents when they are inside a list
        // which is subject of an isEqualTo assertion.
        assertThat(decodedMessage.get(9)).isInstanceOf(List.class);
        List decodedNestedList = (List) decodedMessage.get(9);
        List messageNestedList = (List) message.get(9);
        assertThat(decodedNestedList).hasSize(messageNestedList.size());
        assertThat(decodedNestedList.get(0)).isInstanceOf(byte[].class);
        assertThat((byte[])decodedNestedList.get(0)).isEqualTo(messageNestedList.get(0));
        for(int i=1;i<messageNestedList.size();i++){
            assertThat(decodedNestedList.get(i)).isEqualTo(messageNestedList.get(i));
        }
        for(int i=0;i<message.size();i++){
            // Do not use isEqualTo assertion on the list that contains the byte arrays as it would
            // fail.
            if(i != 9) {
                assertThat(decodedMessage.get(i)).isEqualTo(message.get(i));
            }
        }


    }


}
