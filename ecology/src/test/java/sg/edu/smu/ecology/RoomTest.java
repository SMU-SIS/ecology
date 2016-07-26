package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomTest {

    private Room roomA, roomB;
    private Ecology ecologyA, ecologyB;

    @Before
    public void setUp() throws Exception {
        ecologyA = Mockito.mock(Ecology.class);
        ecologyB = Mockito.mock(Ecology.class);
        String name = "roomA";
        roomA = new Room(name, ecologyA);
    }

    @After
    public void tearDown() throws Exception {
        roomA = null;
    }

    // Check to see if room has been created with the proper name.
    @Test
    public void testGetCorrectRoomName() throws Exception{
        // Expected name
        String roomName = "roomA";
        assertEquals(roomName, roomA.getRoomName());
    }

    @Test
    public void testGetCorrectEcology() throws Exception{

        //assertEquals(ecologyB, room.getEcology());
        assertEquals(ecologyA, roomA.getEcology());
    }

    // When room passes message to event broadcaster
    @Test
    public void testOnMessage() throws Exception {
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        // Room hasn't received the message yet
        assertNull(roomA.getEventBroadcaster().getMessage());

        roomA.onMessage(data);

        // Check if event broadcaster has received the correct message
        assertEquals(data, roomA.getEventBroadcaster().getMessage());
    }

    // When room receives message from event broadcaster
    @Test
    public void testOnEventBroadcasterMessage() throws Exception {
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        String eventType = "test";
        roomA.getEventBroadcaster().publish(eventType, data);

        //Since event broadcaster adds the eventype at the end of the message before passing it to
        //the room
        assertNotEquals(data, roomA.getMessage());

        data.add(eventType);

        // Check to see if the correct message has reached the correct room
        assertEquals(data, roomA.getMessage());
    }

    // Improper value is passed while creating a room
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException(){
        roomB = new Room("", ecologyB);
    }

    // When a room is created, an event broadcaster object is created
    @Test
    public void testNullEventBroadcaster() throws Exception{
        assertNotNull(roomA.getEventBroadcaster());
    }

    // When a room is not created, event broadcaster will not be created
    @Test(expected = NullPointerException.class)
    public void testNullPointerExceptionForEventBroadcaster() {
        roomB.getEventBroadcaster();
    }
}