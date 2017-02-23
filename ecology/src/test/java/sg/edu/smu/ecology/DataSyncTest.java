package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 20/12/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataSyncTest {

    @Mock
    private DataSync.Connector connector;
    @Mock
    private DataSync.SyncDataChangeListener syncDataChangeListener;

    private DataSync dataSync1, dataSync2;

    @Before
    public void setUp() throws Exception {
        dataSync1 = new DataSync(connector, syncDataChangeListener, true);
        dataSync2 = new DataSync(connector, syncDataChangeListener, false);
    }

    @After
    public void tearDown() throws Exception {
        dataSync1 = null;
        dataSync2 = null;
    }

    // To verify that when a new sync data is set, connector and sync data change listener are
    // invoked correctly and also if sync data has been saved correctly
    @Test
    public void testSetData() {
        dataSync1.setData("color", "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Acts as a routing id to differentiate between initial sync message and other data sync
        // messages
        Integer dataSyncMessageIndicator = 0;

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red",
                dataSyncMessageIndicator));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // To check if the new sync data has been saved correctly
        assertEquals(dataSync1.getData("color"), "red");
    }

    // To verify that when the device receives a sync data change from another device in the
    // ecology, sync data change listener is invoked correctly and also if sync data has been saved
    // correctly
    @Test
    public void testOnDataSyncMessage() {
        Integer dataSyncMessageIndicator = 0;
        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn(dataSyncMessageIndicator, "black",
                "color");

        dataSync1.onMessage(message);

        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "black", null);

        // To check if the new sync data has been saved correctly
        assertEquals(dataSync1.getData("color"), "black");
    }

    // To verify that when a data reference device gets a initial data sync request, it sends the
    // current data sync values to the requested device.
    @Test
    public void testOnInitialDataSyncMessageDataRef() {
        Integer initDataSyncRequest = 1;
        Integer initDataSyncResponse = 2;

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn(initDataSyncRequest);
        PowerMockito.when(message.getSource()).thenReturn("Watch");

        // The data reference device receives a initial sync data request
        dataSync1.onMessage(message);

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Since the device is the data reference, it will send back the current sync data
        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList(Collections.emptyMap(),
                initDataSyncResponse));
        // Check if the target type is specific
        Assert.assertEquals(messageArgument.getTargetType().intValue(),
                EcologyMessage.TARGET_TYPE_SPECIFIC);
        // Check if the target is the source device id of the requester
        assertEquals(messageArgument.getTargets(), Collections.singletonList("Watch"));
    }

    // When a non data reference device receives a initial data sync request, it will save the data
    @Test
    public void testOnInitialDataSyncMessageNotDataRefEmptyData() {
        Integer initDataSyncResponse = 2;

        // When an empty map is received
        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn(initDataSyncResponse,
                Collections.emptyMap());

        dataSync2.setData("color", "black");
        dataSync2.setData("number", 4);

        // The data reference device receives a initial sync data request
        dataSync2.onMessage(message);

        // Since the initial data sync is empty, all the current data will be set to null
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", null, "black");
        verify(syncDataChangeListener, times(1)).onDataUpdate("number", null, 4);
    }

    // When a non data reference device receives a initial data sync request, it will save the data
    @Test
    public void testOnInitialDataSyncMessageNotDataRefAllNewData() {
        Integer initDataSyncResponse = 2;

        // When initial data is having a new key and data
        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn(initDataSyncResponse,
                new HashMap<Object, Object>() {{
                    put("activity", "walking");
                }});

        dataSync2.setData("color", "black");
        dataSync2.setData("number", 4);

        // The data reference device receives a initial sync data request
        dataSync2.onMessage(message);

        // Since the initial data sync has a new key and value, the new data will be saved and other
        // key - value data not present will be set to null and removed
        verify(syncDataChangeListener, times(1)).onDataUpdate("activity", "walking", null);
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", null, "black");
        verify(syncDataChangeListener, times(1)).onDataUpdate("number", null, 4);
    }

    // When a non data reference device receives a initial data sync request, it will save the data
    @Test
    public void testOnInitialDataSyncMessageNotDataRefAllUpdateData() {
        Integer initDataSyncResponse = 2;

        // When initial data is having a new key and data as well as old key and data
        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn(initDataSyncResponse,
                new HashMap<Object, Object>() {{
                    put("activity", "walking");
                    put("number", 5);
                }});

        dataSync2.setData("color", "black");
        dataSync2.setData("number", 4);

        // The data reference device receives a initial sync data request
        dataSync2.onMessage(message);

        // All the new key - value will be added and existing key data will be updated and rest will
        // be removed
        verify(syncDataChangeListener, times(1)).onDataUpdate("activity", "walking", null);
        verify(syncDataChangeListener, times(1)).onDataUpdate("number", 5, 4);
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", null, "black");
    }

    // To verify that when an non-existent sync data is requested, a null is returned
    @Test
    public void testGetNonExistentData() {
        assertEquals(dataSync1.getData("color"), null);
    }

    // To verify that when a sync data value is over written, new value will be returned when queried
    @Test
    public void testOverwriteSyncData() {
        dataSync1.setData("color", "red");
        assertEquals(dataSync1.getData("color"), "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        Integer dataSyncMessageIndicator = 0;

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red",
                dataSyncMessageIndicator));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // Over-write the data sync value corresponding to the given key
        dataSync1.setData("color", "blue");
        assertEquals(dataSync1.getData("color"), "blue");

        verify(connector, times(2)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument2;
        messageArgument2 = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument2.getArguments(), Arrays.asList("color", "blue",
                dataSyncMessageIndicator));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "blue", "red");
    }

    // To verify if message or event is not sent when the user sets the same data as before
    @Test
    public void testWhenSameSyncDataSet() {
        dataSync1.setData("color", "red");
        assertEquals(dataSync1.getData("color"), "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        Integer dataSyncMessageIndicator = 0;

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red",
                dataSyncMessageIndicator));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // Set the same data as before
        dataSync1.setData("color", "red");
        assertEquals(dataSync1.getData("color"), "red");

        // To check if connector is not invoked
        verify(connector, times(1)).onMessage(any(EcologyMessage.class));

        // To check if sync data change listener is not invoked
        verify(syncDataChangeListener, never()).onDataUpdate("color", "red", "red");
    }
}
