package sg.edu.smu.ecology;

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

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private Connector connector1;
    @Mock
    private Connector connector2;
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
        ecologyConnection.addCoreConnector(connector1);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector1).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector2).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        ecologyConnection.setReceiver(receiver);

        // Mock isConnected method for connector1
        PowerMockito.when(connector1.isConnected()).thenReturn(true);
        // Connector 1 gets connected
        receiver1.onConnectorConnected();

        // Mock isConnected method for connector2
        PowerMockito.when(connector2.isConnected()).thenReturn(true);
        // Connector 2 gets connected
        receiver2.onConnectorConnected();

        // Ecology must be connected only once even if there are multiple connectors
        verify(receiver, times(1)).onConnectorConnected();
    }

    // To test isConnected method
    @Test
    public void testIsConnected(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector1).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector2).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        ecologyConnection.setReceiver(receiver);

        // Mock isConnected method for connector1
        PowerMockito.when(connector1.isConnected()).thenReturn(true);
        // Connector 1 gets connected
        receiver1.onConnectorConnected();

        // To check that when only one connector among two is connected, isConnected returns false
        assertEquals(false, ecologyConnection.isConnected());

        // Mock isConnected method for connector2
        PowerMockito.when(connector2.isConnected()).thenReturn(true);
        // Connector 2 gets connected
        receiver2.onConnectorConnected();

        // To check that when all the connectors are connected, isConnected returns true
        assertEquals(true, ecologyConnection.isConnected());
    }

    // To test ecology connect method
    @Test
    public void testConnectMethod(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);

        Context context = mock(Context.class);

        String androidId = "android_id";
        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId);

        ecologyConnection.connect(context);

        // To verify that all the added connectors' connect method is called once
        verify(connector1, times(1)).connect(context);
        verify(connector2, times(1)).connect(context);
    }

    // To test ecology disconnect method
    @Test
    public void testDisconnect(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);

        ecologyConnection.disconnect();

        // To verify that all the added connectors' disconnect method is called once
        verify(connector1, times(1)).disconnect();
        verify(connector2, times(1)).disconnect();
    }

    @Test
    public void testSendMessage(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);

        Context context = mock(Context.class);

        String androidId = "android_id";
        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId);

        ecologyConnection.connect(context);

        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");

        ecologyConnection.sendMessage(data);

        verify(connector1, times(1)).sendMessage(data);

        // Adds android id at the end of the message while sending it using a dependent connector
        data.add(androidId);
        verify(connector2, times(1)).sendMessage(data);
    }

    // To verify that ecology connection receiver receives the message when a core connector receives
    // a message. Also if there are no added dependent connectors, there won't be any forwarding of
    // the received message
    @Test
    public void testOnMessageCore(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector1).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        Context context = mock(Context.class);

        String androidId = "android_id";
        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId);

        ecologyConnection.connect(context);

        ecologyConnection.setReceiver(receiver);

        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");

        // Core connector receiver receives the message
        receiver1.onMessage(data);

        verify(receiver, times(1)).onMessage(data);

        // Android id will be added before forwarding the message
        data.add(androidId);
        // To verify that there is no forwarding of message
        verify(connector2, never()).sendMessage(data);
    }

    // To verify that ecology connection receiver receives the message when a core connector receives
    // a message. Also there will be forwarding of the received message as the ecology has
    // dependent connectors
    @Test
    public void testOnMessageCoreWithDependents(){
        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector1).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);

        Context context = mock(Context.class);

        String androidId = "android_id";
        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId);

        ecologyConnection.connect(context);

        ecologyConnection.setReceiver(receiver);

        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");

        // Core connector receiver receives the message
        receiver1.onMessage(data);

        verify(receiver, times(1)).onMessage(data);

        // Android id will be added before forwarding the message
        data.add(androidId);
        // To verify that there is forwarding of messages to dependent connectors
        verify(connector2, times(1)).sendMessage(data);
    }

    // To verify that ecology connection receiver receives the message when a dependent connector
    // receives a message from another device. No forwarding of message will happen if there are
    // no added core connectors
    @Test
    public void testOnMessageDependent(){
        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector2).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        Context context = mock(Context.class);

        String androidId1 = "android_id1";
        String androidId2 = "android_id2";

        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId1);

        ecologyConnection.connect(context);

        ecologyConnection.setReceiver(receiver);

        // Test data - contains a different android id
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");
        data.add(androidId2);

        // Dependent connector receiver receives the message
        receiver2.onMessage(data);

        verify(receiver, times(1)).onMessage(data.subList(0, data.size() - 1));
        verify(connector1, never()).sendMessage(data.subList(0, data.size() - 1));
    }

    // To verify that ecology connection receiver doesn't receive the message when a dependent
    // connector receives it's own message
    @Test
    public void testOnDependentMessageSameDevice(){
        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector2).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        Context context = mock(Context.class);

        String androidId = "android_id";
        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId);

        ecologyConnection.connect(context);

        ecologyConnection.setReceiver(receiver);

        // Test data - contains the same android id
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");
        data.add(androidId);

        // Dependent connector receiver receives the message
        receiver2.onMessage(data);

        verify(receiver, times(0)).onMessage(data.subList(0, data.size() - 1));
        verify(connector1, never()).sendMessage(data.subList(0, data.size() - 1));
    }

    // To verify that ecology connection receiver receives the message when a dependent connector
    // receives a message from another device and the message will be forwarded to the core
    // connectors
    @Test
    public void testOnMessageDependentWithCoreConnectors(){
        // Add a dependent connector
        ecologyConnection.addDependentConnector(connector2);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(connector2).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        // Add a core connector
        ecologyConnection.addCoreConnector(connector1);

        Context context = mock(Context.class);

        String androidId1 = "android_id1";
        String androidId2 = "android_id2";

        // Mock android id
        when(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID)).thenReturn(androidId1);

        ecologyConnection.connect(context);

        ecologyConnection.setReceiver(receiver);

        // Test data - contains a different android id
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test");
        data.add(androidId2);

        // Dependent connector receiver receives the message
        receiver2.onMessage(data);

        // Android id will be removed before passing to ecology connection receiver
        // To verify that receiver receives the message
        verify(receiver, times(1)).onMessage(data.subList(0, data.size() - 1));
        // To verify that the received message is forwarded to core connector
        verify(connector1, times(1)).sendMessage(data.subList(0, data.size() - 1));
    }
}