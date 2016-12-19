package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;


/**
 * Created by Quentin ROY on 15/12/16.
 *
 * Tests for {@link MessageData} and in particular the estimated message size in bytes (necessary
 * for {@link MessageEncoder}.
 */
public class MessageDataTest {

    private MessageData msgData;
    private DataEncoder encoder;
    private IoBuffer buffer;

    @Before
    public void setUp(){
        msgData = new MessageData();
        encoder = new DataEncoder();
        buffer = IoBuffer.allocate(64);
        buffer.setAutoExpand(true);
    }

    @After
    public void cleanUp(){
        buffer.free();
    }

    @Test
    public void numbersEncodingSizeTest() throws CharacterCodingException {
        msgData.addArgument(5);
        msgData.addArgument(3.5);
        msgData.addArgument(3.5f);

        // Encode the message in the buffer.
        encoder.encodeMessage(msgData, buffer);

        // Check that the message has the size evaluated by MessageData
        assertThat(buffer.position()).isEqualTo(msgData.getByteSize());
    }

    @Test
    public void stringEncodingSizeTest() throws CharacterCodingException {
        msgData.addArgument("hello");
        msgData.addArgument('c');

        // Encode the message in the buffer.
        encoder.encodeMessage(msgData, buffer);

        // Check that the message has the size evaluated by MessageData
        assertThat(buffer.position()).isEqualTo(msgData.getByteSize());
    }

    @Test
    public void booleanEncodingSizeTest() throws CharacterCodingException {
        msgData.addArgument(true);
        msgData.addArgument(false);
        msgData.addArgument(true);

        // Encode the message in the buffer.
        encoder.encodeMessage(msgData, buffer);

        // Check that the message has the size evaluated by MessageData
        assertThat(buffer.position()).isEqualTo(msgData.getByteSize());
    }

    @Test
    public void nullEncodingSizeTest() throws CharacterCodingException {
        msgData.addArgument((Object) null);
        msgData.addArgument((Object) null);

        // Encode the message in the buffer.
        encoder.encodeMessage(msgData, buffer);

        // Check that the message has the size evaluated by MessageData
        assertThat(buffer.position()).isEqualTo(msgData.getByteSize());
    }

    @Test
    public void listEncodingSizeTest() throws CharacterCodingException {
        msgData.addArgument(Arrays.asList(1, 4.5, 3f, true, "stuff"));

        // Encode the message in the buffer.
        encoder.encodeMessage(msgData, buffer);

        // Check that the message has the size evaluated by MessageData
        assertThat(buffer.position()).isEqualTo(msgData.getByteSize());
    }

}
