package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 21/7/2016.
 */
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

        // Connector 1 gets connected
        receiver1.onConnectorConnected();
        // Connector 2 gets connected
        receiver2.onConnectorConnected();

        // Ecology must be connected only once even if there are multiple connectors
        verify(receiver, times(1)).onConnectorConnected();
    }
}