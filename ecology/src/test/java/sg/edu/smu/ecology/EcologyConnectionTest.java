package sg.edu.smu.ecology;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by anurooppv on 21/7/2016.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest( {Settings.Secure.class, Log.class} )
public class EcologyConnectionTest {

    @Mock
    private Wifip2pConnector wifip2pConnector;
    @Mock
    private MsgApiConnector msgApiConnector;
    @Mock
    private Connector.Receiver receiver;

    private EcologyConnection ecologyConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecologyConnection = new EcologyConnection();

        // Prepare the mock for static classes
        PowerMockito.mockStatic(Settings.Secure.class);
        PowerMockito.mockStatic(Log.class);
    }

    @After
    public void tearDown() throws Exception {
        ecologyConnection = null;
    }

    // To test "ecology:connected" message sending
    @Test
    public void testEcologyConnected() throws Exception {
        // Add a core connector
        ecologyConnection.addCoreConnector(wifip2pConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(wifip2pConnector).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        // Add a dependent connector
        ecologyConnection.addDependentConnector(msgApiConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(msgApiConnector).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        ecologyConnection.setReceiver(receiver);

        // Mock isConnected method for wifip2pConnector
        PowerMockito.when(wifip2pConnector.isConnected()).thenReturn(true);
        // Connector 1 gets connected
        receiver1.onConnectorConnected();

        // Mock isConnected method for msgApiConnector
        PowerMockito.when(msgApiConnector.isConnected()).thenReturn(true);
        // Connector 2 gets connected
        receiver2.onConnectorConnected();

        // Ecology must be connected only once even if there are multiple connectors
        verify(receiver, times(1)).onConnectorConnected();
    }

    // To test isConnected method
    @Test
    public void testIsConnected(){
        // Add a core connector
        ecologyConnection.addCoreConnector(wifip2pConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(wifip2pConnector).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        // Add a dependent connector
        ecologyConnection.addDependentConnector(msgApiConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(msgApiConnector).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        ecologyConnection.setReceiver(receiver);

        // Mock isConnected method for wifip2pConnector
        PowerMockito.when(wifip2pConnector.isConnected()).thenReturn(true);
        // Connector 1 gets connected
        receiver1.onConnectorConnected();

        // To check that when only one connector among two is connected, isConnected returns false
        assertEquals(false, ecologyConnection.isConnected());

        // Mock isConnected method for msgApiConnector
        PowerMockito.when(msgApiConnector.isConnected()).thenReturn(true);
        // Connector 2 gets connected
        receiver2.onConnectorConnected();

        // To check that when all the connectors are connected, isConnected returns true
        assertEquals(true, ecologyConnection.isConnected());
    }

    // To test ecology connect method
    @Test
    public void testConnectMethod(){
        // Add a core connector
        ecologyConnection.addCoreConnector(wifip2pConnector);

        // Add a dependent connector
        ecologyConnection.addDependentConnector(msgApiConnector);

        Context context = mock(Context.class);

        ContentResolver mockContentResolver = mock(ContentResolver.class);

        // Mock android id
        when(Settings.Secure.getString(mockContentResolver, Settings.Secure.ANDROID_ID)).thenReturn("android_id");

        ecologyConnection.connect(context);

        // To verify that all the added connectors' connect method is called once
        verify(wifip2pConnector, times(1)).connect(context);
        verify(msgApiConnector, times(1)).connect(context);
    }

}