package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(PowerMockRunner.class)
public class EcologyTest {

    private Ecology ecology;

    @Mock
    private EcologyConnection ecologyConnection;
    @Mock
    private Room room;
    @Mock
    private Ecology.RoomFactory roomFactory;
    @Mock
    private Connector.Receiver receiver;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecology = new Ecology(roomFactory,ecologyConnection);
    }

    @After
    public void tearDown() throws Exception {
        ecology = null;
    }

    // To check if the message is received by the connector
    @Test
    public void testOnRoomMessage() throws Exception {
        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");

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
        room = ecology.getRoom("room");
        assertEquals(room, ecology.getRoom("room"));
    }

    // When message is received from a connector - check for correct room
    @Test
    public void testCorrectRoomMessage() throws Exception{
        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");
        String roomName = "room";
        data.add(roomName);

        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
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
    public void testNoRoomFoundReceiverMessage(){
        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");
        // Different room name
        String roomName = "room2";
        data.add(roomName);

        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
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
    @Test(expected = IllegalArgumentException.class)
    public void testIncorrectReceiverMessage(){
        // Test data - no room name is added
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add(23);

        // To verify if add receiver was called only once
        verify(ecologyConnection, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(receiverCaptor.capture());
        receiver = receiverCaptor.getValue();

        // Receiver gets the message destined for a room
        // Since the message is in inappropriate format, exception will be thrown
        receiver.onMessage(data);
    }
}