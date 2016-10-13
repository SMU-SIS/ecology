package sg.edu.smu.ecology;

import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Log.class)
public class EcologyTest {

    private Ecology ecology;

    @Mock
    private EcologyConnection ecologyConnection;
    @Mock
    private Room room;
    @Mock
    private Ecology.RoomFactory roomFactory;
    @Mock
    private Wifip2pConnector wifip2pConnector;
    @Mock
    private MsgApiConnector msgApiConnector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecology = new Ecology(roomFactory, ecologyConnection);

        // Prepare the mock
        PowerMockito.mockStatic(Log.class);
    }

    @After
    public void tearDown() throws Exception {
        ecology = null;
    }

    // To check if the message is received by the connector
    @Test
    public void testOnRoomMessage() throws Exception {
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test");

        String roomName = "room";
        ecology.onRoomMessage(roomName, data);

        // Room name will be added at the end of the data before passing it to connector
        Vector<Object> connectorData = new Vector<>(data);
        connectorData.add(roomName);

        // To verify if the connector received the message
        verify(ecologyConnection).sendMessage(connectorData);
    }

    // Check if room is added or not
    @Test
    public void testGetRoom() throws Exception {
        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // To verify if room factory has been called with appropriate arguments
        verify(roomFactory, times(1)).createRoom("room", ecology);

        // To verify that roomFactory is not called on the second time
        room = ecology.getRoom("room");
        verify(roomFactory, times(1)).createRoom("room", ecology);

        // To verify the object returned by the getRoom call is the same one returned by room factory
        assertEquals(room, roomFactory.createRoom("room", ecology));

        // To verify that objects returned are same for a given room name
        assertEquals(ecology.getRoom("room"), ecology.getRoom("room"));

        // To verify that room factory is called for a new room name
        room = ecology.getRoom("room1");
        verify(roomFactory, times(1)).createRoom("room1", ecology);

        // To verify that objects returned are not same for different room names
        assertNotEquals(ecology.getRoom("room"), ecology.getRoom("room1"));
    }

    // When message is received from a connector - check for correct room
    @Test
    public void testCorrectRoomMessage() throws Exception {
        String roomName = "room";
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test", roomName);

        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // Receiver gets the message
        receiver.onMessage(data);

        // To verify if the message reaches the correct room
        verify(room, times(1)).onMessage(data.subList(0, data.size() - 1));
    }

    //When message is received from a connector - check for inappropriate rooms
    @Test
    public void testNoRoomFoundReceiverMessage() {
        // Different room name
        String roomName = "room2";
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test", roomName);

        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // Receiver gets the message destined for room 2
        receiver.onMessage(data);

        // To verify that the message never reached room
        verify(room, never()).onMessage(data.subList(0, data.size() - 1));
    }

    // When message is received from a connector - check for incorrect message format
    @Test
    public void testIncorrectReceiverMessage() {
        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // Test data - no room name is added
        List<Object> data = Arrays.<Object>asList(1, 23);

        // Receiver receives the message
        receiver.onMessage(data);

        // Verify the mock
        PowerMockito.verifyStatic(times(1));

        // Expected - in general
        Log.e(anyString(), anyString());

        // Expected - if we want to verify ClassCastException
        //String TAG = Ecology.class.getSimpleName();
        //Log.e(TAG, "Exception java.lang.Integer cannot be cast to java.lang.String");

        // Expected - if we want to verify IndexOutOfBoundsException - this case empty data must be passed
        //Log.e(TAG,"Exception -1");
    }

    // Check if connector connected to ecology message is received from connector
    // Also to verify that all the rooms in the ecology receive this message
    @Test
    public void testConnectorConnected() {
        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the addReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // One more room is added to the ecology
        Room room1 = mock(Room.class);
        PowerMockito.when(roomFactory.createRoom("room1", ecology)).thenReturn(room1);
        room1 = ecology.getRoom("room1");

        // Receiver receives the message that connector has been connected to the ecology
        receiver.onConnectorConnected();

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onEcologyConnected();
        verify(room1, times(1)).onEcologyConnected();
    }

    // Check if connector disconnected from ecology message is received from connector
    // Also to verify that all the rooms in the ecology receive this message
    @Test
    public void testConnectorDisonnected() {
        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the addReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // One more room is added to the ecology
        Room room1 = mock(Room.class);
        PowerMockito.when(roomFactory.createRoom("room1", ecology)).thenReturn(room1);
        room1 = ecology.getRoom("room1");

        // Receiver receives the message that connector has been disconnected from the ecology
        receiver.onConnectorDisconnected();

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onEcologyDisconnected();
        verify(room1, times(1)).onEcologyDisconnected();
    }
}
