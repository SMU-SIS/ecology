package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;

import java.util.Arrays;

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

    private DataSync dataSync;

    @Before
    public void setUp() throws Exception {
        dataSync = new DataSync(connector, syncDataChangeListener);
    }

    @After
    public void tearDown() throws Exception {
        dataSync = null;
    }

    // To verify that when a new sync data is set, connector and sync data change listener are
    // invoked correctly and also if sync data has been saved correctly
    @Test
    public void testSetData() {
        dataSync.setData("color", "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red"));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // To check if the new sync data has been saved correctly
        assertEquals(dataSync.getData("color"), "red");
    }

    // To verify that when the device receives a sync data change from another device in the
    // ecology, sync data change listener is invoked correctly and also if sync data has been saved
    // correctly
    @Test
    public void testOnMessage() {
        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.fetchArgument()).thenReturn("black", "color");

        dataSync.onMessage(message);

        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "black", null);

        // To check if the new sync data has been saved correctly
        assertEquals(dataSync.getData("color"), "black");
    }

    // To verify that when an non-existent sync data is requested, a null is returned
    @Test
    public void testGetNonExistentData() {
        assertEquals(dataSync.getData("color"), null);
    }

    // To verify that when a sync data value is over written, new value will be returned when queried
    @Test
    public void testOverwriteSyncData() {
        dataSync.setData("color", "red");
        assertEquals(dataSync.getData("color"), "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red"));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // Over-write the data sync value corresponding to the given key
        dataSync.setData("color", "blue");
        assertEquals(dataSync.getData("color"), "blue");

        verify(connector, times(2)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument2;
        messageArgument2 = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument2.getArguments(), Arrays.asList("color", "blue"));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "blue", "red");
    }

    // To verify if message or event is not sent when the user sets the same data as before
    @Test
    public void testWhenSameSyncDataSet() {
        dataSync.setData("color", "red");
        assertEquals(dataSync.getData("color"), "red");

        // To capture the argument in the onMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Arrays.asList("color", "red"));
        // To check if sync data change listener is invoked once
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);

        // Set the same data as before
        dataSync.setData("color", "red");
        assertEquals(dataSync.getData("color"), "red");

        // To check if connector is not invoked
        verify(connector, times(1)).onMessage(any(EcologyMessage.class));

        // To check if sync data change listener is not invoked
        verify(syncDataChangeListener, never()).onDataUpdate("color", "red", "red");
    }
}
