package sg.edu.smu.ecology;

import android.app.Activity;
import android.content.Context;

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
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventBroadcasterTest {

    @Mock
    private EventReceiver eventReceiver1, eventReceiver2;
    @Mock
    EventBroadcaster.Connector connector;
    @Mock
    Ecology ecology;
    @Mock
    Context context;
    @Mock
    Activity activityContext1, activityContext2;
    @Mock
    ActivityLifecycleTracker activityLifecycleTracker;
    private EventBroadcaster eventBroadcaster;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        eventBroadcaster = new EventBroadcaster(connector, context, ecology);
    }

    @After
    public void tearDown() throws Exception {
        eventBroadcaster = null;
    }

    // Check if the message comes to the right event receiver
    @Test
    public void testMessageReception() {
        eventBroadcaster.subscribe("test1", eventReceiver1);
        eventBroadcaster.subscribe("test2", eventReceiver2);

        // Test data
        List<Object> data = Arrays.<Object>asList(11, "test1");

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data.subList(0, data.size() - 1));
        PowerMockito.when(message.fetchArgument()).thenReturn("test1");

        eventBroadcaster.onRoomMessage(message);

        // Test data
        List<Object> data2 = Arrays.<Object>asList(21, "test2");

        EcologyMessage message2 = mock(EcologyMessage.class);
        PowerMockito.when(message2.getArguments()).thenReturn(data2.subList(0, data.size() - 1));
        PowerMockito.when(message2.fetchArgument()).thenReturn("test2");

        eventBroadcaster.onRoomMessage(message2);

        // To verify that the right event receiver is being called the message is received
        verify(eventReceiver1).handleEvent("test1", data.subList(0, data.size() - 1));
        verify(eventReceiver2, never()).handleEvent("test1", data.subList(0, data.size() - 1));

        // To verify that the right event receiver is being called the message is received
        verify(eventReceiver2).handleEvent("test2", data2.subList(0, data.size() - 1));
        verify(eventReceiver1, never()).handleEvent("test2", data2.subList(0, data.size() - 1));
    }

    // Check if the message reaches the correct room with correct data
    @Test
    public void testPublishNoArgs() {
        // Add an event receiver for the event "test".
        eventBroadcaster.subscribe("test", eventReceiver1);

        eventBroadcaster.publish("test");

        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // To check if right value is passed
        assertEquals(messageArgument.getArguments(), Collections.<Object>singletonList("test"));
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent("test", Collections.emptyList());
    }

    // Check if the message reaches the correct room with correct data
    @Test
    public void testPublishList() {
        // Add an event receiver for the event "test".
        eventBroadcaster.subscribe("test", eventReceiver1);

        // Test data
        List<Object> data = Arrays.<Object>asList(1, 23);
        eventBroadcaster.publish("test", data);

        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "test"));
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent("test", data);
    }

    // Check if the message reaches the correct room with correct data
    @Test
    public void testPublishWithArgs() {
        // Add an event receiver for the event "test".
        eventBroadcaster.subscribe("test", eventReceiver1);

        eventBroadcaster.publishWithArgs("test", 1, 4, "hello");

        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 4, "hello", "test"));
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent(
                "test", Arrays.<Object>asList(1, 4, "hello"));

        eventBroadcaster.publishWithArgs("test", 0);

        // To capture the argument in the onEventBroadcasterMessage method
        verify(connector, times(2)).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument2;
        messageArgument2 = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument2.getArguments(), Arrays.asList(0, "test"));
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent("test", Collections.<Object>singletonList(0));
    }

    // Check if message goes to an unsubscribed event receiver
    @Test
    public void testUnsubscribe() {
        // Subscribe to "test" events.
        eventBroadcaster.subscribe("test", eventReceiver1);
        eventBroadcaster.subscribe("test", eventReceiver2);

        // Publish a first event.
        List<Object> data1 = Collections.<Object>singletonList(1);
        eventBroadcaster.publish("test", data1);

        // Unsubscribe to "test" events.
        eventBroadcaster.unsubscribe("test", eventReceiver1);

        // Publish a second event.
        List<Object> data2 = Collections.<Object>singletonList(2);
        eventBroadcaster.publish("test", data2);

        // Verify that only the first event has been received by receiver1...
        verify(eventReceiver1, times(1)).handleEvent("test", data1);
        verify(eventReceiver1, never()).handleEvent("test", data2);
        // ...and that receiver 2 received both.
        verify(eventReceiver2, times(1)).handleEvent("test", data1);
        verify(eventReceiver2, times(1)).handleEvent("test", data2);
    }

    // Check if a local event published(direct call eg: ecology connected message) is received by the event receivers
    @Test
    public void testOnPublishLocalEvents() {
        // Subscribe to "ecology:connected" events.
        eventBroadcaster.subscribe("ecology:connected", eventReceiver1);

        // Event broadcaster publishes a local event
        eventBroadcaster.publishLocalEvent("ecology:connected",
                Collections.<Object>singletonList("test arg"));

        // To verify if the right event receiver receives the message
        verify(eventReceiver1, times(1)).handleEvent("ecology:connected",
                Collections.<Object>singletonList("test arg"));
        verify(eventReceiver2, never()).handleEvent("ecology:connected",
                Collections.<Object>singletonList("test arg"));
    }

    // To verify that the receiver cannot modify the received data - UnsupportedOperationException is thrown
    @Test
    public void testEventReceiverDataModification() {
        eventBroadcaster.subscribe("test", new EventReceiver() {
            @Override
            public void handleEvent(String eventType, List<Object> eventData) {
                // Make sure that every modifying methods of the eventData's list fails with an
                // exception.

                try {
                    eventData.add(2);
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.add(1, 5);
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.addAll(Arrays.asList(1, 3));
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.addAll(1, Arrays.asList(4, 1));
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.clear();
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.set(1, 3);
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.remove(1);
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.remove("a string");
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }

                try {
                    eventData.removeAll(Arrays.asList(5, new Object()));
                    fail("Expected Unsupported exception to be thrown");
                } catch (Exception e) {
                    assertEquals(e.getClass(), UnsupportedOperationException.class);
                }
            }
        });

        // Test data
        List<Object> data = Arrays.<Object>asList(1, 4, "a string", "test");

        EcologyMessage message = mock(EcologyMessage.class);
        PowerMockito.when(message.getArguments()).thenReturn(data);

        // Send the message to the eventBroadcaster (last argument is the event type).
        eventBroadcaster.onRoomMessage(message);
    }

    // This test checks if an activity in the background receives the subscribed event if it has not
    // subscribed it as a background event
    @Test
    public void testBackgroundActivityWithNoBackgroundEvent() {
        PowerMockito.when(ecology.getActivityLifecycleTracker()).
                thenReturn(activityLifecycleTracker);

        // The current foreground activity is not the event broadcaster context activity. Hence only
        // the background events should be received
        PowerMockito.when(activityLifecycleTracker.getCurrentForegroundActivity()).
                thenReturn(activityContext2);

        // Event broadcaster with an activity context
        EventBroadcaster eventBroadcaster1 = new EventBroadcaster(connector, activityContext1,
                ecology);

        // Add an event receiver for the event "bgTest1". This is not a background event
        eventBroadcaster1.subscribe("bgTest1", eventReceiver1, false);

        // Add an event receiver for the event "bgTest2". This is also not a background event
        eventBroadcaster1.subscribe("bgTest2", eventReceiver2);

        // Test data
        List<Object> data = Arrays.<Object>asList(1, 23);

        eventBroadcaster1.publish("bgTest1", data);
        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest1"));

        eventBroadcaster1.publish("bgTest2", data);
        // To capture the argument in the onEventBroadcasterMessage method
        messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(2)).onEventBroadcasterMessage(messageCaptor.capture());
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest2"));

        // Verify that the event was not received by the local receivers.
        verify(eventReceiver1, never()).handleEvent("bgTest1", data);
        verify(eventReceiver2, never()).handleEvent("bgTest2", data);
    }

    // This test checks if an activity in the background receives the subscribed event if it has
    // subscribed it as a background event
    @Test
    public void testBackgroundActivityWithBackgroundEvent() {
        PowerMockito.when(ecology.getActivityLifecycleTracker()).
                thenReturn(activityLifecycleTracker);

        // The current foreground activity is not the event broadcaster context activity. Hence only
        // the background events should be received
        PowerMockito.when(activityLifecycleTracker.getCurrentForegroundActivity()).
                thenReturn(activityContext2);

        // Event broadcaster with an activity context
        EventBroadcaster eventBroadcaster1 = new EventBroadcaster(connector, activityContext1,
                ecology);

        // Add an event receiver for the event "bgTest1". This is not a background event
        eventBroadcaster1.subscribe("bgTest1", eventReceiver1, false);

        // Add an event receiver for the event "bgTest2". This is a background event
        eventBroadcaster1.subscribe("bgTest2", eventReceiver2, true);

        // Test data
        List<Object> data = Arrays.<Object>asList(1, 23);

        eventBroadcaster1.publish("bgTest1", data);
        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest1"));

        eventBroadcaster1.publish("bgTest2", data);
        // To capture the argument in the onEventBroadcasterMessage method
        messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(2)).onEventBroadcasterMessage(messageCaptor.capture());
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest2"));

        // Verify that only bgTest2 was received since it's a background event
        verify(eventReceiver1, never()).handleEvent("bgTest1", data);
        verify(eventReceiver2, times(1)).handleEvent("bgTest2", data);
    }

    // This test checks if an activity in the foreground receives the subscribed event if it has not
    // subscribed it as a background event
    @Test
    public void testForegroundActivityWithNoBackgroundEvent() {
        PowerMockito.when(ecology.getActivityLifecycleTracker()).
                thenReturn(activityLifecycleTracker);

        // The current foreground activity is same as the event broadcaster context activity.
        // Hence the the event should be received
        PowerMockito.when(activityLifecycleTracker.getCurrentForegroundActivity()).
                thenReturn(activityContext1);

        // Event broadcaster with an activity context
        EventBroadcaster eventBroadcaster1 = new EventBroadcaster(connector, activityContext1,
                ecology);

        // Add an event receiver for the event "bgTest1". This is not a background event
        eventBroadcaster1.subscribe("bgTest1", eventReceiver1, false);

        // Add an event receiver for the event "bgTest2". This is also not a background event
        eventBroadcaster1.subscribe("bgTest2", eventReceiver2);

        // Test data
        List<Object> data = Arrays.<Object>asList(1, 23);

        eventBroadcaster1.publish("bgTest1", data);
        // To capture the argument in the onEventBroadcasterMessage method
        ArgumentCaptor<EcologyMessage> messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(1)).onEventBroadcasterMessage(messageCaptor.capture());
        // Create a local mock ecology message
        EcologyMessage messageArgument;
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest1"));

        eventBroadcaster1.publish("bgTest2", data);
        // To capture the argument in the onEventBroadcasterMessage method
        messageCaptor = ArgumentCaptor.forClass(EcologyMessage.class);
        verify(connector, times(2)).onEventBroadcasterMessage(messageCaptor.capture());
        messageArgument = messageCaptor.getValue();

        // Verify that the right data reaches the room.
        assertEquals(messageArgument.getArguments(), Arrays.asList(1, 23, "bgTest2"));

        // Verify that the event was received by the local receivers.
        verify(eventReceiver1, times(1)).handleEvent("bgTest1", data);
        verify(eventReceiver2, times(1)).handleEvent("bgTest2", data);
    }
}