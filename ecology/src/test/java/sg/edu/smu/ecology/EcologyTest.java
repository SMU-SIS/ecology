package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by anurooppv on 26/7/2016.
 */
public class EcologyTest {

    private Room roomA, roomB;
    private Ecology ecologyA, ecologyB;
    String name = "roomA";
    private EcologyConnection connector;
    private List<Connector.Receiver> receivers;

    @Before
    public void setUp() throws Exception {
        connector = new EcologyConnection();
        ecologyA = new Ecology(connector);

        receivers = connector.getReceivers();
    }

    @After
    public void tearDown() throws Exception {
        connector = null;
        ecologyA = null;
    }

    // When a message is received from room
    @Test
    public void testOnRoomMessage() throws Exception {
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        // No message has reached the ecology from rooms
        assertNull(connector.getMessage());

        ecologyA.onRoomMessage(name, data);
        // Room name will be added at the end of the message
        assertNotEquals(data, connector.getMessage());

        String roomNameA = "roomA";
        data.add(roomNameA);
        // Check if ecology has added the correct room name
        assertEquals(data, connector.getMessage());
    }

    // Check if room is added or not
    @Test
    public void testGetRoom() throws Exception {

        // If room is not found, required room will be created else will return the existing room
        roomA = ecologyA.getRoom(name);

        // Since room has been added
        assertEquals(roomA, ecologyA.getRoom(name));
    }

    // When message is received from a connector - check for correct room
    @Test
    public void testCorrectRoomMessage() throws Exception{
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");
        String roomName = "roomA";
        data.add(roomName);

        roomA = ecologyA.getRoom(roomName);

        // Message destined for room A is received
        for (Connector.Receiver receiver : receivers) {
            receiver.onMessage(data);
        }

        // Message is received at the correct room
        assertEquals(data.subList(0, data.size() - 1), roomA.getMessage());
    }

    //When message is received from a connector - check for inappropriate rooms
    @Test
    public void testNoRoomFoundReceiverMessage(){
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        // Data is destined for room B
        String roomNameB = "roomB";
        data.add(roomNameB);

        String roomNameA = "roomA";
        // Ecology has only room A
        roomA = ecologyA.getRoom(roomNameA);

        // Data destined for room B comes
        for (Connector.Receiver receiver : receivers) {
            receiver.onMessage(data);
        }

        // Message is not received in roomA
        assertNull(roomA.getMessage());
    }

    // When message is received from a connector - check for incorrect message format
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectReceiverMessage(){
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add(23);

        String roomName = "roomA";
        roomA = ecologyA.getRoom(roomName);

        // Receives message of incorrect format
        for (Connector.Receiver receiver : receivers) {
            receiver.onMessage(data);
        }

        // Should throw IllegalArgumentException as expected
        roomA.getMessage();
    }
}