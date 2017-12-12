package sg.edu.smu.ecology.encoding;

import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sg.edu.smu.ecology.EcologyMessage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;


/**
 * Tests for {@link MessageEncoder} and {@link MessageDecoder}.
 *
 * @author Quentin Roy
 */
public class MessageEncoderDecoderTest {
    private MessageEncoder encoder;
    private MessageDecoder decoder;

    private EcologyMessage ecologyMessage;

    @Before
    public void setUp() throws Exception {
        encoder = new MessageEncoder();
        decoder = new MessageDecoder();

        // Create the mock of EcologyMessage
        ecologyMessage = mock(EcologyMessage.class);

        PowerMockito.when(ecologyMessage.getSource()).thenReturn("Mobile");
        PowerMockito.when(ecologyMessage.getTargetType()).thenReturn(EcologyMessage.TARGET_TYPE_SPECIFIC);
        PowerMockito.when(ecologyMessage.getTargets()).thenReturn(Collections.singletonList("Watch"));
    }

    @Test
    public void emptyMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.emptyList());

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEmpty();
    }

    @Test
    public void oneIntMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.<Object>singletonList(5));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly(5);
    }

    @Test
    public void oneFloatMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.<Object>singletonList(4.5f));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly(4.5f);
    }

    @Test
    public void oneDoubleMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.<Object>singletonList(4.5));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly(4.5);
    }

    @Test
    public void oneTrueBooleanMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.<Object>singletonList(true));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly(true);
    }

    @Test
    public void oneFalseBooleanMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(Collections.<Object>singletonList(false));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly(false);
    }

    @Test
    public void oneASCIIStringMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(
                Collections.<Object>singletonList("simple easy string"));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly("simple easy string");
    }

    @Test
    public void oneUnicodeStringMessageEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(
                Collections.<Object>singletonList("%ïô${å}thing[ò]#@«|"));

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly("%ïô${å}thing[ò]#@«|");
    }

    @Test
    public void oneByteArrayEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(
                Collections.<Object>singletonList(new byte[]{
                        (byte) 0xe0, (byte) 0x4f, (byte) 0xd0, (byte) 0x20, (byte) 0xea, (byte) 0x3a,
                        (byte) 0x69, (byte) 0x10, (byte) 0xa2, (byte) 0xd8, (byte) 0x08, (byte) 0x00,
                        (byte) 0x2b, (byte) 0x30, (byte) 0x30, (byte) 0x9d
                }));
        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).hasSize(1);
        assertThat(decodedMessage.getArguments().get(0)).isInstanceOf(byte[].class);
        assertThat((byte[]) decodedMessage.getArguments().get(0)).asList().containsExactly(
                (byte) 0xe0, (byte) 0x4f, (byte) 0xd0, (byte) 0x20, (byte) 0xea, (byte) 0x3a, (byte) 0x69,
                (byte) 0x10, (byte) 0xa2, (byte) 0xd8, (byte) 0x08, (byte) 0x00, (byte) 0x2b, (byte) 0x30,
                (byte) 0x30, (byte) 0x9d
        ).inOrder();
    }

    @Test
    public void oneCharEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(
                Collections.<Object>singletonList('c'));
        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly('c');
    }

    @Test
    public void oneUnicodeCharEncoding() throws CharacterCodingException {
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(
                Collections.<Object>singletonList('î'));
        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).containsExactly('î');
    }

    @Test
    public void oneNullEncoding() throws CharacterCodingException {
        List<Object> message = Collections.singletonList(null);
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);
        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test
    public void oneEmptyListEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(Collections.emptyList());
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test
    public void oneListEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(Arrays.asList(
                5, 4, "hello", 2.5, true, 'c', "ˆˆø"
        ));
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test(expected = MessageData.UnsupportedDataTypeException.class)
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
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test
    public void emptyMapEncoding() throws CharacterCodingException {
        List<Object> message = Collections.<Object>singletonList(new HashMap<String, Object>());
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test
    public void mapEncoding() throws CharacterCodingException {
        // Create the message.
        Map<Object, Object> map = new HashMap<>();
        map.put("key1", 3);
        map.put(2, 4.2f);
        map.put('d', 4.1);
        map.put("key3", 'c');
        map.put(5.6, null);
        map.put(true, 0);
        map.put(0, "some string");
        map.put("key5", false);
        List<Object> message = Collections.<Object>singletonList(map);
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).isEqualTo(message);
    }

    @Test
    public void manyThingsAtOnceEncoding() throws CharacterCodingException {
        // Create the message.
        Map<Object, Object> map = new HashMap<>();
        map.put('h', 3);
        map.put(false, true);
        map.put(true, false);
        map.put(0, 'ï');
        map.put("key", null);
        final List<Object> message = Arrays.asList(
                8, "something",
                Arrays.asList(4, 0.4, 'k'),
                0, 4.6, 5, null, '0',
                Arrays.asList(
                        3, "stuff", null,
                        Arrays.asList(
                                2, 6.3, null, true
                        ),
                        map, 2
                ),
                'c',
                Arrays.asList(
                        new byte[]{0, -120, 123, 8, 4},
                        Arrays.asList(
                                "ouch", Arrays.asList(
                                        Arrays.asList(4, 'c'), 0
                                )
                        ), 2.5
                ),
                "îIø", 'z', 'å', '7', null
        );
        PowerMockito.when(ecologyMessage.getArguments()).thenReturn(message);

        // Encode the message.
        byte[] encodedMessage = encoder.encode(ecologyMessage);
        // Decode it.
        EcologyMessage decodedMessage = decoder.decode(encodedMessage);
        // Make sure it is the same.
        assertThat(decodedMessage.getArguments()).hasSize(message.size());
        // Check the nested list containing the byte array first. Arrays are an issue as
        // their memory addresses are compared instead of their contents when they are inside a list
        // which is subject of an isEqualTo assertion.
        assertThat(decodedMessage.getArguments().get(10)).isInstanceOf(List.class);
        List decodedNestedList = (List) decodedMessage.getArguments().get(10);
        List messageNestedList = (List) message.get(10);
        assertThat(decodedNestedList).hasSize(messageNestedList.size());
        assertThat(decodedNestedList.get(0)).isInstanceOf(byte[].class);
        assertThat((byte[]) decodedNestedList.get(0)).isEqualTo(messageNestedList.get(0));
        for (int i = 1; i < messageNestedList.size(); i++) {
            assertThat(decodedNestedList.get(i)).isEqualTo(messageNestedList.get(i));
        }
        for (int i = 0; i < message.size(); i++) {
            // Do not use isEqualTo assertion on the list that contains the byte arrays as it would
            // fail.
            if (i != 10) {
                assertThat(decodedMessage.getArguments().get(i)).isEqualTo(message.get(i));
            }
        }


    }


}
