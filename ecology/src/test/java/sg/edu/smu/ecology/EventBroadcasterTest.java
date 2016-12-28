package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
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
    private EventBroadcaster eventBroadcaster;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        eventBroadcaster = new EventBroadcaster(connector);
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
        eventBroadcaster.onRoomMessage(data);

        // Test data
        List<Object> data2 = Arrays.<Object>asList(21, "test2");
        eventBroadcaster.onRoomMessage(data2);

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

        // Verify that the right data reaches the room.
        verify(connector).onEventBroadcasterMessage(Collections.singletonList((Object) "test"));
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

        // Publish method adds the event type at the end
        List<Object> roomData = new ArrayList<>(data);
        roomData.add("test");

        // Verify that the right data reaches the room.
        verify(connector).onEventBroadcasterMessage(roomData);
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent("test", data);
    }

    // Check if the message reaches the correct room with correct data
    @Test
    public void testPublishWithArgs() {
        // Add an event receiver for the event "test".
        eventBroadcaster.subscribe("test", eventReceiver1);

        eventBroadcaster.publishWithArgs("test", 1, 4, "hello");

        // Verify that the right data reaches the room.
        verify(connector).onEventBroadcasterMessage(Arrays.<Object>asList(1, 4, "hello", "test"));
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent(
                "test", Arrays.<Object>asList(1, 4, "hello"));

        eventBroadcaster.publishWithArgs("test", 0);

        // Verify that the right data reaches the room.
        verify(connector).onEventBroadcasterMessage(Arrays.<Object>asList(0, "test"));
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

        // Send the message to the eventBroadcaster (last argument is the event type).
        eventBroadcaster.onRoomMessage(Arrays.<Object>asList(1, 4, "a string", "test"));
    }

}