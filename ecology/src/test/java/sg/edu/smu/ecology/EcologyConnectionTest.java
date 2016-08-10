package sg.edu.smu.ecology;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;

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
    private Ecology ecology;
    @Mock
    private Ecology.RoomFactory roomFactory;

    private EcologyConnection ecologyConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ecologyConnection = new EcologyConnection();
    }

    @Test
    public void testEcologyConnected() throws Exception {
        ecologyConnection.addCoreConnector(wifip2pConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor1 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(wifip2pConnector).setReceiver(receiverCaptor1.capture());
        // Create a local mock receiver
        Connector.Receiver receiver1;
        receiver1 = receiverCaptor1.getValue();

        ecologyConnection.addDependentConnector(msgApiConnector);
        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> receiverCaptor2 = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(msgApiConnector).setReceiver(receiverCaptor2.capture());
        // Create a local mock receiver
        Connector.Receiver receiver2;
        receiver2 = receiverCaptor2.getValue();

        PowerMockito.whenNew(Ecology.class).withArguments(roomFactory, ecologyConnection).thenReturn(ecology);
        ecology = new Ecology(roomFactory, ecologyConnection);

        // To capture the argument in the setReceiver method
        ArgumentCaptor<Connector.Receiver> ecologyreceiverCaptor = ArgumentCaptor.forClass(Connector.Receiver.class);
        verify(ecologyConnection).setReceiver(ecologyreceiverCaptor.capture());
        // Create a local mock receiver
        Connector.Receiver ecologyReceiver;
        ecologyReceiver = ecologyreceiverCaptor.getValue();

        receiver1.onConnectorConnected();
        receiver2.onConnectorConnected();

        verify(ecologyReceiver, times(2)).onConnectorConnected();
    }
}