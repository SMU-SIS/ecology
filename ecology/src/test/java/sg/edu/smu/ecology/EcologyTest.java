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

import sg.edu.smu.ecology.connector.Connector;

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
    private Room room;
    @Mock
    private Ecology.RoomFactory roomFactory;
    @Mock
    private Connector connector;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecology = new Ecology(roomFactory, connector);

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

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        String roomName = "room";
        ecology.onRoomMessage(roomName, message);

        // To capture the argument in the sendMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).sendMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Room name will be added at the end of the data before passing it to connector
        Vector<Object> connectorData = new Vector<>(data);
        connectorData.add(roomName);

        // To verify if the connector received the correct data
        assertEquals(messageArgument.getArguments(), connectorData);
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

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // Receiver gets the message
        receiver.onMessage(message);

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        // To verify if the message reaches the correct room
        verify(room, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that correct data is passed
        assertEquals(messageArgument.getArguments(), data.subList(0, data.size() - 1));
    }

    // When message is received from a connector - check for inappropriate rooms
    @Test
    public void testNoRoomFoundReceiverMessage() {
        // Different room name
        String roomName = "room2";
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test", roomName);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology)).thenReturn(room);
        room = ecology.getRoom("room");

        // Receiver gets the message destined for room 2
        receiver.onMessage(message);

        // To verify that the message never reached room
        verify(room, never()).onMessage(any(EcologyMessage.class));
    }

    // When message is received from a connector - check for incorrect message format
    @Test
    public void testIncorrectReceiverMessage() {
        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // Test data - no room name is added
        List<Object> data = Arrays.<Object>asList(1, 23);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        // Receiver receives the message
        receiver.onMessage(message);

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

    // Check if device connected to ecology message is received from connector
    // Also to verify that all the rooms in the ecology receive this message
    @Test
    public void testDeviceConnected() {
        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
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

        String deviceId = "Mobile";
        // Receiver receives the message that the device has been connected to the ecology
        receiver.onDeviceConnected(deviceId);

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onDeviceConnected(deviceId);
        verify(room1, times(1)).onDeviceConnected(deviceId);

    }

    // Check if device disconnected from ecology message is received from connector
    // Also to verify that all the rooms in the ecology receive this message
    @Test
    public void testDeviceDisconnected() {
        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
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

        String deviceId = "Mobile";
        // Receiver receives the message that the device has been disconnected from the ecology
        receiver.onDeviceDisconnected(deviceId);

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onDeviceDisconnected(deviceId);
        verify(room1, times(1)).onDeviceDisconnected(deviceId);
    }
}
