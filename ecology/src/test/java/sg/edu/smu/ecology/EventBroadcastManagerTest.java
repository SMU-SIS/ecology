package sg.edu.smu.ecology;

import android.content.Context;
import android.os.Handler;

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
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
@RunWith(MockitoJUnitRunner.class)
public class EventBroadcastManagerTest {
    private EventBroadcasterManager eventBroadcasterManager;
    @Mock
    private Room room;
    @Mock
    private Context context1, context2;
    @Mock
    private EventBroadcaster eventBroadcaster1, eventBroadcaster2;
    @Mock
    private Handler ecologyLooperHandler;
    @Mock
    private EventBroadcasterManager.EventBroadcasterFactory eventBroadcasterFactory;

    @Before
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        eventBroadcasterManager = new EventBroadcasterManager(room, eventBroadcasterFactory);
    }

    @After
    public void tearDown() throws Exception {
        eventBroadcasterManager = null;
    }

    @Test
    public void testForwardMsgToEventBroadcasters() {
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");
        data.add(1);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        Handler handler1 = mock(Handler.class);
        Handler handler2 = mock(Handler.class);

        // Add event broadcaster and it's associated context
        eventBroadcasterManager.addEventBroadcaster(context1, eventBroadcaster1);
        eventBroadcasterManager.addEventBroadcaster(context2, eventBroadcaster2);

        // Add a handler associated with the context
        eventBroadcasterManager.addHandler(context1, handler1);
        eventBroadcasterManager.addHandler(context2, handler2);

        // Mocking ecology looper Handler to invoke a Runnable
        when(handler1.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });
        when(handler2.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        eventBroadcasterManager.forwardMessage(message);

        // To capture the argument passed to the event broadcasters
        ArgumentCaptor<EcologyMessage> messageCaptor1 =
                ArgumentCaptor.forClass(EcologyMessage.class);
        ArgumentCaptor<EcologyMessage> messageCaptor2 =
                ArgumentCaptor.forClass(EcologyMessage.class);

        // To verify that all the event broadcasters receive the room message
        verify(eventBroadcaster1, times(1)).onRoomMessage(messageCaptor1.capture());
        verify(eventBroadcaster2, times(1)).onRoomMessage(messageCaptor2.capture());

        // Get the argument value
        EcologyMessage message1 = messageCaptor1.getValue();
        EcologyMessage message2 = messageCaptor2.getValue();

        // To verify that the correct message is forwarded to the event broadcasters
        assertEquals(message.getArguments(), message1.getArguments());
        assertEquals(message.getArguments(), message2.getArguments());
    }

    @Test
    public void sendMessageTest() {
        final List<Object> data = new ArrayList<>();
        data.add(10);
        data.add("test");
        data.add(1);

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        // Mock ecology looper handler retrieval
        Ecology ecology = mock(Ecology.class);
        PowerMockito.doReturn(ecology).when(room).getEcology();
        PowerMockito.doReturn(ecologyLooperHandler).when(ecology).getHandler();

        // Mocking ecology looper Handler to invoke a Runnable
        when(ecologyLooperHandler.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        PowerMockito.when(eventBroadcasterFactory.createEventBroadcaster(any(EventBroadcaster.
                Connector.class), any(Context.class), any(Ecology.class))).thenReturn(eventBroadcaster1);
        eventBroadcasterManager.getEventBroadcaster(context1);

        // To capture the argument in the createDataSync method
        ArgumentCaptor<EventBroadcaster.Connector> connectorCaptor =
                ArgumentCaptor.forClass(EventBroadcaster.Connector.class);
        verify(eventBroadcasterFactory).createEventBroadcaster(connectorCaptor.capture(),
                any(Context.class), any(Ecology.class));
        EventBroadcaster.Connector eventBroadcasterConnector = connectorCaptor.getValue();

        // When a message is received from event broadcaster
        eventBroadcasterConnector.onEventBroadcasterMessage(message);

        // To verify that correct data is passed to the room
        verify(room, times(1)).onEventBroadcasterMessage(message);
    }

    @Test
    public void testPostLocalEvent() {
        Handler handler1 = mock(Handler.class);
        Handler handler2 = mock(Handler.class);

        // Add event broadcaster and it's associated context
        eventBroadcasterManager.addEventBroadcaster(context1, eventBroadcaster1);
        eventBroadcasterManager.addEventBroadcaster(context2, eventBroadcaster2);

        // Add a handler associated with the context
        eventBroadcasterManager.addHandler(context1, handler1);
        eventBroadcasterManager.addHandler(context2, handler2);

        // Mocking ecology looper Handler to invoke a Runnable
        when(handler1.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });
        when(handler2.post(any(Runnable.class))).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                invocation.getArgumentAt(0, Runnable.class).run();
                return null;
            }
        });

        eventBroadcasterManager.postLocalEvent("Tap", Arrays.<Object>asList(1, "djsd"));

        verify(eventBroadcaster1, times(1)).publishLocalEvent("Tap", Arrays.<Object>asList(1, "djsd"));
        verify(eventBroadcaster2, times(1)).publishLocalEvent("Tap", Arrays.<Object>asList(1, "djsd"));
    }

    // To verify that right event broadcaster is returned
    @Test
    public void testGetEventBroadcaster() {
        eventBroadcasterManager.addEventBroadcaster(context1, eventBroadcaster1);
        eventBroadcasterManager.addEventBroadcaster(context2, eventBroadcaster2);

        assertEquals(eventBroadcaster1, eventBroadcasterManager.getEventBroadcaster(context1));
        assertNotEquals(eventBroadcaster1, eventBroadcasterManager.getEventBroadcaster(context2));
        assertEquals(eventBroadcaster2, eventBroadcasterManager.getEventBroadcaster(context2));
        assertNotEquals(eventBroadcaster2, eventBroadcasterManager.getEventBroadcaster(context1));
    }
}
