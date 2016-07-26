package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

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

    @Before
    public void setUp() throws Exception {
        connector = new EcologyConnection();
        connector.addCoreConnector(Mockito.mock(Connector.class));
        ecologyA = new Ecology(connector);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testOnRoomMessage() throws Exception {
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        ecologyA.onRoomMessage(name, data);
        // Room name will be added at the end of the message
        assertNotEquals(data, connector.getMessage());

        data.add(name);
        // Check if ecology has added the room name
        assertEquals(data, connector.getMessage());
    }

    @Test
    public void testGetRoom() throws Exception {

        // No room has been added
        assertNull(ecologyA.getRoomsFromName(name));

        roomA = ecologyA.getRoom(name);

        // Since room has been added
        assertEquals(roomA, ecologyA.getRoomsFromName(name));
    }
}