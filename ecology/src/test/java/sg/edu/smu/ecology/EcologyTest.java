package sg.edu.smu.ecology;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by anurooppv on 21/7/2016.
 */
public class EcologyTest {

    //Create ecology object
    Ecology ecologyObj;

    @Before
    public void setUp() throws Exception {
        ecologyObj = new Ecology(new Connector() {
            @Override
            public void sendMessage(List<Object> message) {

            }

            @Override
            public void addReceiver(Receiver receiver) {

            }

            @Override
            public void connect(Context activity) {

            }

            @Override
            public void disconnect() {

            }

            @Override
            public boolean isConnected() {
                return false;
            }
        });
    }

    @Test
    public void testOnRoomMessage() throws Exception {
        String roomName = "roomA";

        //Create a list object which has the message data
        List<Object> msg = new ArrayList<>();

        assertEquals(0, msg.size());
        msg.add(10);
        assertEquals(1, msg.size());
        msg.add("data");
        assertEquals(2, msg.size());

        // Add room name at the end
        msg.add(roomName);
        assertEquals(3, msg.size());

        // Check if roomName is added at the end
        assertEquals(roomName, msg.get(msg.size() - 1));
    }

    @Test
    public void testGetRoom() throws Exception {
        String roomName = "roomAB";
        String roomName1 = "roomA";

        // Call getRoom method - creates a new room
        Room room = ecologyObj.getRoom(roomName);

        // Expected data - returns the room in the list
        Room roomExpected = ecologyObj.getRoom(roomName);

        // Verify data
        assertEquals(room, roomExpected);

        /*Boolean check = room.equals(roomExpected);
        assertFalse(check);*/
    }
}