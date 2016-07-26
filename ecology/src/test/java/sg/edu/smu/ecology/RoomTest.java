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

    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException(){
        roomB = new Room("", ecologyB);
    }

    @Test
    public void testNullEventBroadcaster() throws Exception{
        assertNotNull(roomA.getEventBroadcaster());
    }

    @Test(expected = NullPointerException.class)
    public void testNullPointerExceptionForEventBroadcaster() {
        roomB.getEventBroadcaster();
    }

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

    @Test
    public void testOnEventBroadcasterMessage() throws Exception {
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        String eventType = "test";
        roomA.getEventBroadcaster().publish(eventType, data);

        /**
         * Since event broadcaster adds the eventype at the end of the message before passing it to
           the room
         */
        assertNotEquals(data, roomA.getMessage());

        data.add(eventType);

        // Check to see if the correct message has reached the correct room
        assertEquals(data, roomA.getMessage());
    }
}