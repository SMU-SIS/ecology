package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
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
    private Room.EventBroadcasterFactory eventBroadcasterFactory;
    @Mock
    private Room.DataSyncFactory dataSyncFactory;
    @Mock
    private EventBroadcaster eventBroadcaster;
    @Mock
    private DataSync dataSync;
    private Room room;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        room = new Room(roomName, ecology, eventBroadcasterFactory, dataSyncFactory);
    }

    @After
    public void tearDown() throws Exception {
        room = null;
    }

    // To verify if event broadcaster receives the right message from room
    @Test
    public void testOnReceiveEventBroadcasterMessage() throws Exception {
        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        // To get the mock data sync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // Test data - contains event message routing id
        List<Object> data1 = Arrays.<Object>asList(1, "test1", 1);
        room.onMessage(data1);

        // To verify if event broadcaster receives the correct data from room
        verify(eventBroadcaster, times(1)).onRoomMessage(data1.subList(0, data1.size() - 1));

        // To verify that data sync doesn't receive the data from room
        verify(dataSync, never()).onMessage(data1.subList(0, data1.size() - 1));
    }

    // To verify if data sync receives the right message from room
    @Test
    public void testOnReceiveDataSyncMessage() throws Exception {
        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        // To get the mock data sync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // Test data - contains data sync message routing id
        List<Object> data2 = Arrays.<Object>asList(1, "test2", 0);
        room.onMessage(data2);

        // To verify if data sync receives the correct data from room
        verify(dataSync, times(1)).onMessage(data2.subList(0, data2.size() - 1));

        // To verify that event broadcaster doesn't receive the data from room
        verify(eventBroadcaster, never()).onRoomMessage(data2.subList(0, data2.size() - 1));
    }

    // To verify if ecology receives the event broadcaster message from Room
    @Test
    public void testOnEventBroadcasterMessage() throws Exception {
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test1");

        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        // To capture the argument in the createEventBroadcaster method
        ArgumentCaptor<EventBroadcaster.Connector> connectorCaptor =
                ArgumentCaptor.forClass(EventBroadcaster.Connector.class);
        verify(eventBroadcasterFactory).createEventBroadcaster(connectorCaptor.capture());
        EventBroadcaster.Connector connector = connectorCaptor.getValue();

        connector.onEventBroadcasterMessage(data);

        List<Object> ecologyData = new ArrayList<>(data);
        // Add event message id
        ecologyData.add(1);
        // To verify that ecology receives the correct message from room
        verify(ecology).onRoomMessage(roomName, ecologyData);
    }

    // To verify if ecology receives the data sync message from Room
    @Test
    public void testOnDataSyncMessage() throws Exception {
        // Test data
        List<Object> data = Arrays.<Object>asList(1, "test1");

        // To get the mock DataSync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.Connector> connectorCaptor =
                ArgumentCaptor.forClass(DataSync.Connector.class);
        verify(dataSyncFactory).createDataSync(connectorCaptor.capture(),
                any(DataSync.SyncDataChangeListener.class));
        DataSync.Connector dataSyncConnector = connectorCaptor.getValue();

        dataSyncConnector.onMessage(data);

        List<Object> ecologyData = new ArrayList<>(data);
        // Add data sync message id
        ecologyData.add(0);
        // To verify that ecology receives the correct message from room
        verify(ecology).onRoomMessage(roomName, ecologyData);
    }

    // To verify if a local event is published when a sync data update happens
    @Test
    public void testOnSyncDataUpdate() {
        // To get the mock DataSync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.SyncDataChangeListener> syncDataChangeListenerCaptor =
                ArgumentCaptor.forClass(DataSync.SyncDataChangeListener.class);
        verify(dataSyncFactory).createDataSync(any(DataSync.Connector.class),
                syncDataChangeListenerCaptor.capture());
        DataSync.SyncDataChangeListener syncDataChangeListener = syncDataChangeListenerCaptor.getValue();

        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        syncDataChangeListener.onDataUpdate("Color", "Red", "Blue");

        // To verify that a local event is published with correct data
        verify(eventBroadcaster, times(1)).publishLocalEvent(Settings.SYNC_DATA,
                Arrays.<Object>asList("Color", "Red", "Blue"));
    }

    // Improper value is passed while creating a room
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException() {
        room = new Room("", ecology);
    }

    // Check if a device connected to ecology message received is published as a local event
    @Test
    public void testOnDeviceConnectedMessage() {
        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        String deviceId = "Mobile";
        // Room receives the message that a device has been connected to the ecology
        room.onDeviceConnected(deviceId);
        String deviceConnected = "device:connected";
        // Verify that a local event is published with correct data
        verify(eventBroadcaster, times(1)).publishLocalEvent(deviceConnected,
                Collections.<Object>singletonList(deviceId));
    }

    // Check if a device disconnected message received is published as a local event
    @Test
    public void testOnDeviceDisconnectedMessage() {
        // To get the mock eventBroadcaster
        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster
                (any(EventBroadcaster.Connector.class))).thenReturn(eventBroadcaster);
        eventBroadcaster = room.getEventBroadcaster();

        String deviceId = "Mobile";
        // Room receives the message that a device has been disconnected from the ecology
        room.onDeviceDisconnected(deviceId);
        String deviceDisconnected = "device:disconnected";
        // Verify that a local event is published with correct data
        verify(eventBroadcaster, times(1)).publishLocalEvent(deviceDisconnected,
                Collections.<Object>singletonList(deviceId));
    }
}