package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import static org.mockito.Mockito.when;

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
    @Mock
    private Ecology.DataSyncFactory dataSyncFactory;
    @Mock
    private Ecology.EcologyLooperFactory ecologyLooperFactory;
    @Mock
    private DataSync ecologyDataSync;
    @Mock
    private EcologyLooper ecologyLooper;
    @Mock
    private Handler connectorHandler;
    @Mock
    private Handler ecologyLooperHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecology = new Ecology(connector, false, roomFactory, dataSyncFactory, ecologyLooperFactory);

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
        final List<Object> data = new ArrayList<>();
        data.add(1);
        data.add("test");

        final String roomName = "room";
        final Integer roomMessageId = 1;

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(roomName);
                return null;
            }
        }).when(message).addArgument(roomName);
        PowerMockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(roomMessageId);
                return null;
            }
        }).when(message).addArgument(roomMessageId);

        PowerMockito.when(message.getArguments()).thenReturn(data);

        when(connectorHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        ecology.setConnectorHandler(connectorHandler);
        ecology.onRoomMessage(roomName, message);

        // To capture the argument in the sendMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).sendMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Room name and routing id will be added at the end of the data before passing it to connector
        Vector<Object> connectorData = new Vector<Object>(Arrays.asList(1, "test", roomName, roomMessageId));

        // To verify if the connector received the correct data
        assertEquals(messageArgument.getArguments(), connectorData);
    }

    // To verify if connector receives the right message
    @Test
    public void testOnDataSyncMessage() {
        // Test data
        final List<Object> data = new ArrayList<>();
        data.add(Collections.emptyMap());

        final String roomName = "room";
        final Integer dataSyncMsgId = 0;

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(roomName);
                return null;
            }
        }).when(message).addArgument(roomName);

        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(dataSyncMsgId);
                return null;
            }
        }).when(message).addArgument(dataSyncMsgId);

        PowerMockito.when(message.getArguments()).thenReturn(data);

        when(connectorHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        ecology.setConnectorHandler(connectorHandler);
        ecology.onEcologyDataSyncMessage(message);

        // To capture the argument in the sendMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).sendMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Routing id will be added at the end of the data before passing it to connector
        Vector<Object> connectorData = new Vector<Object>(Arrays.asList(Collections.emptyMap(), dataSyncMsgId));

        // To verify if the connector received the correct data
        assertEquals(messageArgument.getArguments(), connectorData);
    }

    // Check if room is added or not
    @Test
    public void testGetRoom() throws Exception {
        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        // To verify if room factory has been called with appropriate arguments
        verify(roomFactory, times(1)).createRoom("room", ecology, false);

        // To verify that roomFactory is not called on the second time
        room = ecology.getRoom("room");
        verify(roomFactory, times(1)).createRoom("room", ecology, false);

        // To verify the object returned by the getRoom call is the same one returned by room factory
        assertEquals(room, roomFactory.createRoom("room", ecology, false));

        // To verify that objects returned are same for a given room name
        assertEquals(ecology.getRoom("room"), ecology.getRoom("room"));

        // To verify that room factory is called for a new room name
        room = ecology.getRoom("room1");
        verify(roomFactory, times(1)).createRoom("room1", ecology, false);

        // To verify that objects returned are not same for different room names
        assertNotEquals(ecology.getRoom("room"), ecology.getRoom("room1"));
    }

    // When message is received from a connector - check for correct room
    @Test
    public void testCorrectRoomMessage() throws Exception {
        final String roomName = "room";
        Integer roomMessageId = 1;
        // Test data
        final List<Object> data = new ArrayList<>();
        data.add(1);
        data.add("test");
        data.add(roomName);
        data.add(roomMessageId);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        when(ecologyLooperFactory.createEcologyLooper(any(String.class))).thenReturn(ecologyLooper);
        PowerMockito.when(ecologyLooper.getHandler()).thenReturn(ecologyLooperHandler);
        when(ecologyLooperHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        // Receiver gets the message
        receiver.onMessage(message);

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        // To verify if the message reaches the correct room
        verify(room, times(1)).onMessage(messageCaptor.capture());
        verify(ecologyDataSync, never()).onMessage(any(EcologyMessage.class));
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that correct data is passed
        assertEquals(messageArgument.getArguments(), Arrays.<Object>asList(1, "test"));
    }

    // When message is received from a connector - check for inappropriate rooms
    @Test
    public void testNoRoomFoundReceiverMessage() {
        // Different room name
        String roomName = "room2";
        Integer roomMessageId = 1;
        // Test data
        final List<Object> data = new ArrayList<>();
        data.add(1);
        data.add("test");
        data.add(roomName);
        data.add(roomMessageId);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        // To verify if add receiver was called only once
        verify(connector, times(1)).setReceiver(any(Connector.Receiver.class));

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        when(ecologyLooperFactory.createEcologyLooper(any(String.class))).thenReturn(ecologyLooper);
        PowerMockito.when(ecologyLooper.getHandler()).thenReturn(ecologyLooperHandler);
        when(ecologyLooperHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

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

        Integer roomMessageId = 1;
        // Test data - no room name is added
        final List<Object> data = new ArrayList<>();
        data.add(1);
        data.add(23);
        data.add(roomMessageId);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        when(ecologyLooperFactory.createEcologyLooper(any(String.class))).thenReturn(ecologyLooper);
        PowerMockito.when(ecologyLooper.getHandler()).thenReturn(ecologyLooperHandler);
        when(ecologyLooperHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

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

    // To verify if data sync message received from connector is forwarded correctly in ecology
    @Test
    public void testEcologyDataSyncMessage() {
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector).setReceiver(receiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver receiver;
        receiver = receiverCaptor.getValue();

        Integer dataSyncMsgId = 0;
        // Test data - data sync msg id is added
        final List<Object> data = new ArrayList<>();
        data.add(Collections.emptyMap());
        data.add(dataSyncMsgId);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class))).thenReturn(ecologyDataSync);

        // To get the mock room
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        when(ecologyLooperFactory.createEcologyLooper(any(String.class))).thenReturn(ecologyLooper);
        PowerMockito.when(ecologyLooper.getHandler()).thenReturn(ecologyLooperHandler);
        when(ecologyLooperHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        // Receiver receives the message
        receiver.onMessage(message);

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(ecologyDataSync).onMessage(messageCaptor.capture());

        assertEquals(messageCaptor.getValue().getArguments(), data);
        // To verify that the message never reached room
        verify(room, never()).onMessage(any(EcologyMessage.class));
    }

    // To verify if correct list of devices are returned
    @Test
    public void testGetAvailableDevices() {
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class))).thenReturn(ecologyDataSync);

        // When no devices are available, empty list will be returned
        assertEquals(ecology.getAvailableDevices(), Collections.<String>emptyList());

        when(ecologyDataSync.getData("devices")).thenReturn(new HashMap<Object, Object>() {{
            put("mobile", true);
            put("watch", false);
        }});

        assertEquals(ecology.getAvailableDevices(), Arrays.asList("watch", "mobile"));
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
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        // One more room is added to the ecology
        Room room1 = mock(Room.class);
        PowerMockito.when(roomFactory.createRoom("room1", ecology, false)).thenReturn(room1);
        room1 = ecology.getRoom("room1");

        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class))).thenReturn(ecologyDataSync);
        ecologyDataSync = ecology.getEcologyDataSync();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.SyncDataChangeListener> syncDataChangeListenerCaptor =
                ArgumentCaptor.forClass(DataSync.SyncDataChangeListener.class);
        verify(dataSyncFactory).createDataSync(any(DataSync.Connector.class),
                syncDataChangeListenerCaptor.capture(), any(Boolean.class));
        DataSync.SyncDataChangeListener syncDataChangeListener = syncDataChangeListenerCaptor.getValue();

        // Initial data - doesn't contain device id data since it's not connected
        Map<Object, Object> data = new HashMap<Object, Object>() {{
            put("Watch", true);
        }};
        PowerMockito.when(ecologyDataSync.getData("devices")).thenReturn(data);

        String deviceId = "Mobile";
        // Receiver receives the message that the device has been connected to the ecology
        receiver.onDeviceConnected(deviceId);

        // New data - add the newly connected connected device id data
        Map<Object, Object> newData = new HashMap<>(data);
        newData.put(deviceId, false);

        // Verify that new value is set in ecology data sync
        verify(ecologyDataSync, times(1)).setData("devices", newData);

        syncDataChangeListener.onDataUpdate("devices", newData, data);

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onDeviceConnected(deviceId, false);
        verify(room1, times(1)).onDeviceConnected(deviceId, false);

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
        PowerMockito.when(roomFactory.createRoom("room", ecology, false)).thenReturn(room);
        room = ecology.getRoom("room");

        // One more room is added to the ecology
        Room room1 = mock(Room.class);
        PowerMockito.when(roomFactory.createRoom("room1", ecology, false)).thenReturn(room1);
        room1 = ecology.getRoom("room1");

        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class))).thenReturn(ecologyDataSync);
        ecologyDataSync = ecology.getEcologyDataSync();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.SyncDataChangeListener> syncDataChangeListenerCaptor =
                ArgumentCaptor.forClass(DataSync.SyncDataChangeListener.class);
        verify(dataSyncFactory).createDataSync(any(DataSync.Connector.class),
                syncDataChangeListenerCaptor.capture(), any(Boolean.class));
        DataSync.SyncDataChangeListener syncDataChangeListener = syncDataChangeListenerCaptor.getValue();

        final String deviceId = "Mobile";

        // Initial data - contains the device id data since we are assuming that it's already
        // connected
        Map<Object, Object> data = new HashMap<Object, Object>() {{
            put("Watch", true);
            put(deviceId, false);
        }};
        PowerMockito.when(ecologyDataSync.getData("devices")).thenReturn(data);

        // Receiver receives the message that the device has been disconnected from the ecology
        receiver.onDeviceDisconnected(deviceId);

        // New data - remove the disconnected device id
        Map<Object, Object> newData = new HashMap<>(data);
        newData.remove(deviceId);

        // Verify that new value is set in ecology data sync
        verify(ecologyDataSync, times(1)).setData("devices", newData);

        syncDataChangeListener.onDataUpdate("devices", newData, data);

        // To verify that all the rooms in the ecology receive the message
        verify(room, times(1)).onDeviceDisconnected(deviceId, false);
        verify(room1, times(1)).onDeviceDisconnected(deviceId, false);
    }
}
