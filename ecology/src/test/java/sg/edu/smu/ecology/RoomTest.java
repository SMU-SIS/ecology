package sg.edu.smu.ecology;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by anurooppv on 21/7/2016.
 */
public class RoomTest {

    private Ecology ecologyObj;

    @Before
    public void setUp() throws Exception {
        ecologyObj =  new Ecology(new Connector() {
            @Override
            public void sendMessage(List<Object> message) {

            }

            @Override
            public void addReceiver(Receiver receiver) {

            }

            @Override
            public void connect() {

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
    public void testGetEventBroadcaster() throws Exception {
        Room roomObj = new Room("roomAB", ecologyObj);
        // Check if Event Broadcaster object is null or not
        assertNotNull(roomObj.getEventBroadcaster());
    }

    @Test
    public void testOnMessage() throws Exception {

    }

    @Test
    public void testOnEventBroadcasterMessage() throws Exception {

    }
}