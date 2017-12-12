/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */


package sg.edu.smu.ecology;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
@RunWith(MockitoJUnitRunner.class)
public class RoomTest {

    private final String roomName = "room";
    @Mock
    private Ecology ecology;
    @Mock
    private Room.DataSyncFactory dataSyncFactory;
    @Mock
    private Room.EventBroadcasterManagerFactory eBMFactory;
    @Mock
    private EventBroadcaster eventBroadcaster;
    @Mock
    private DataSync dataSync;
    @Mock
    private Context context;
    @Mock
    private EventBroadcasterManager eventBroadcasterManager;
    private Room room;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        room = new Room(roomName, ecology, false, dataSyncFactory, eBMFactory);
    }

    @After
    public void tearDown() throws Exception {
        room = null;
    }

    // To verify if event broadcaster receives the right message from room
    @Test
    public void testOnReceiveEventBroadcasterMessage() throws Exception {
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        // To get the mock data sync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class), any(Ecology.class))
        ).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // Test data - contains event message routing id
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");
        data.add(1);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        room.onMessage(message);

        // To capture the argument in the forwardMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(eventBroadcasterManager, times(1)).forwardMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To verify if event broadcaster receives the correct data from room
        assertEquals(messageArgument.getArguments(), Arrays.<Object>asList(10, "test"));

        // To verify that data sync doesn't receive the data from room
        verify(dataSync, never()).onMessage(any(EcologyMessage.class));
    }

    // To verify if data sync receives the right message from room
    @Test
    public void testOnReceiveDataSyncMessage() throws Exception {
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        // To get the mock data sync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class), any(Ecology.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // Test data - contains data sync message routing id
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");
        data.add(0);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return data.remove(data.size() - 1);
            }
        }).when(message).fetchArgument();

        room.onMessage(message);

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(dataSync, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To verify if event broadcaster receives the correct data from room
        assertEquals(messageArgument.getArguments(), Arrays.<Object>asList(10, "test"));

        // To verify that event broadcaster doesn't receive the data from room
        verify(eventBroadcasterManager, never()).forwardMessage(any(EcologyMessage.class));
    }

    // To verify if ecology receives the event broadcaster message from Room
    @Test
    public void testOnEventBroadcasterMessage() throws Exception {
        // Test data
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(1);
                return null;
            }
        }).when(message).addArgument(1);

        room.onEventBroadcasterMessage(message);

        // To capture the argument in the onRoomMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        ArgumentCaptor<String> roomNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(ecology, times(1)).onRoomMessage(roomNameCaptor.capture(), messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();
        String roomNameValue = roomNameCaptor.getValue();

        // Add data sync message id at the end
        List<Object> ecologyData = Arrays.<Object>asList(10, "test", 1);

        // To verify that ecology receives the correct message from room
        assertEquals(messageArgument.getArguments(), ecologyData);
        // To verify that correct room name is passed to the ecology
        assertEquals(roomNameValue, roomName);
    }

    // To verify if ecology receives the data sync message from Room
    @Test
    public void testOnDataSyncMessage() throws Exception {
        // Test data
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");

        // To get the mock DataSync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class), any(Ecology.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.Connector> connectorCaptor =
                ArgumentCaptor.forClass(DataSync.Connector.class);
        verify(dataSyncFactory).createDataSync(connectorCaptor.capture(),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class), any(Ecology.class));
        DataSync.Connector dataSyncConnector = connectorCaptor.getValue();

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);
        PowerMockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                data.add(0);
                return null;
            }
        }).when(message).addArgument(0);

        dataSyncConnector.onMessage(message);

        // To capture the argument in the onRoomMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        ArgumentCaptor<String> roomNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(ecology, times(1)).onRoomMessage(roomNameCaptor.capture(), messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();
        String roomNameValue = roomNameCaptor.getValue();

        // Add data sync message id at the end
        List<Object> ecologyData = Arrays.<Object>asList(10, "test", 0);

        // To verify that ecology receives the correct message from room
        assertEquals(messageArgument.getArguments(), ecologyData);
        // To verify that correct room name is passed to the ecology
        assertEquals(roomNameValue, roomName);
    }

    // To verify if a local event is published when a sync data update happens
    @Test
    public void testOnSyncDataUpdate() {
        // To get the mock DataSync
        PowerMockito.when(dataSyncFactory.createDataSync(any(DataSync.Connector.class),
                any(DataSync.SyncDataChangeListener.class), any(Boolean.class), any(Ecology.class))).thenReturn(dataSync);
        dataSync = room.getDataSyncObject();

        // To capture the argument in the createDataSync method
        ArgumentCaptor<DataSync.SyncDataChangeListener> syncDataChangeListenerCaptor =
                ArgumentCaptor.forClass(DataSync.SyncDataChangeListener.class);
        verify(dataSyncFactory).createDataSync(any(DataSync.Connector.class),
                syncDataChangeListenerCaptor.capture(), any(Boolean.class), any(Ecology.class));
        DataSync.SyncDataChangeListener syncDataChangeListener = syncDataChangeListenerCaptor.getValue();

        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        syncDataChangeListener.onDataUpdate("Color", "Red", "Blue");

        // To verify that a local event is published with correct data
        verify(eventBroadcasterManager, times(1)).postLocalEvent(Settings.SYNC_DATA,
                Arrays.<Object>asList("Color", "Red", "Blue"));
    }

    // Improper value is passed while creating a room
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalArgumentException() {
        room = new Room("", ecology, false);
    }

    // Check if a device connected to ecology message received is published as a local event
    @Test
    public void testOnDeviceConnectedMessage() {
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        String deviceId = "Mobile";
        // Room receives the message that a device has been connected to the ecology
        room.onDeviceConnected(deviceId, false);
        String deviceConnected = "device:connected";
        // Verify that a local event is published with correct data
        verify(eventBroadcasterManager, times(1)).postLocalEvent(deviceConnected,
                Collections.<Object>singletonList(deviceId));
    }

    // Check if a device disconnected message received is published as a local event
    @Test
    public void testOnDeviceDisconnectedMessage() {
        // To get the mock eventBroadcaster manager
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        String deviceId = "Mobile";
        // Room receives the message that a device has been disconnected from the ecology
        room.onDeviceDisconnected(deviceId, false);
        String deviceDisconnected = "device:disconnected";
        // Verify that a local event is published with correct data
        verify(eventBroadcasterManager, times(1)).postLocalEvent(deviceDisconnected,
                Collections.<Object>singletonList(deviceId));
    }

    // Check if a local event is published when the room receives ecology connected message
    @Test
    public void testOnEcologyConnectedMessage() {
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        room.onEcologyConnected();
        String ecologyConnected = "ecology:connected";
        // Verify that a local event is published with correct data
        verify(eventBroadcasterManager, times(1)).postLocalEvent(ecologyConnected,
                Collections.<Object>emptyList());
    }

    // Check if a local event is published when the room receives ecology disconnected message
    @Test
    public void testOnEcologyDisconnectedMessage() {
        PowerMockito.when(eBMFactory.createEventBroadcasterManager(any(Room.class))).thenReturn(
                eventBroadcasterManager);
        eventBroadcasterManager = room.getEventBroadcasterManager();

        room.onEcologyDisconnected();
        String ecologyDisconnected = "ecology:disconnected";
        // Verify that a local event is published with correct data
        verify(eventBroadcasterManager, times(1)).postLocalEvent(ecologyDisconnected,
                Collections.<Object>emptyList());
    }
}