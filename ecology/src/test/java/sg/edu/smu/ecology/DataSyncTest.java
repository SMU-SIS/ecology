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

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
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
    // invoked correctly
    @Test
    public void testSetData(){
        dataSync.setData("color", "red");

        verify(connector, times(1)).onMessage(Arrays.<Object>asList("color", "red"));
        verify(syncDataChangeListener, times(1)).onDataUpdate("color", "red", null);
    }

    // To verify that when the device receives a sync data change from another device in the
    // ecology, sync data change listener is invoked correctly
    @Test
    public void testOnMessage(){
        dataSync.onMessage(Arrays.<Object>asList("Color", "black"));

        verify(syncDataChangeListener, times(1)).onDataUpdate("Color", "black", null);
    }
}
