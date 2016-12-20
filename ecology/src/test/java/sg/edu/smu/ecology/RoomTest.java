package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomTest {

    private final String roomName = "room";
    @Mock
    private Ecology ecology;
    @Mock
    private EventBroadcaster eventBroadcaster;
    private Room room;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        room = new Room(roomName, ecology, eventBroadcaster);
    }

    @After
    public void tearDown() throws Exception {
        room = null;
    }

    // To verify if eventbroadcaster receives the right message from room
    @Test
    public void testOnMessage() throws Exception {
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test1");

        assertEquals(eventBroadcaster, room.getEventBroadcaster());

        room.onMessage(data);
        // To verify if event broadcaster receives the correct data from room
        verify(eventBroadcaster).onRoomMessage(data);
    }

    // To verify if ecology receives the message from Room
    @Test
    public void testOnEventBroadcasterMessage() throws Exception {
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test1");

        room.onEventBroadcasterMessage(data);

        // To verify ecology receives the message from room
        verify(ecology).onRoomMessage(roomName, data);
    }

    // Improper value is passed while creating a room
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException() {
        room = new Room("", ecology);
    }

    // Check if a device connected to ecology message received is published as a local event
    @Test
    public void testOnDeviceConnectedMessage() {
        String deviceId = "Mobile";
        // Room receives the message that a device has been connected to the ecology
        room.onDeviceConnected(deviceId);
        String deviceConnected = "device:connected";
        // Verify that a local event is published
        verify(eventBroadcaster, times(1)).publishLocalEvent(deviceConnected,
                Collections.<Object>singletonList(deviceId));
    }

    // Check if a device disconnected message received is published as a local event
    @Test
    public void testOnDeviceDisconnectedMessage() {
        String deviceId = "Mobile";
        // Room receives the message that a device has been disconnected from the ecology
        room.onDeviceDisconnected(deviceId);
        String deviceDisconnected = "device:disconnected";
        // Verify that a local event is published
        verify(eventBroadcaster, times(1)).publishLocalEvent(deviceDisconnected,
                Collections.<Object>singletonList(deviceId));
    }
}