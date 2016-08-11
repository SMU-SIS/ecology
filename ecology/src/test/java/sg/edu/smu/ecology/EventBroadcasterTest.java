package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
@RunWith(MockitoJUnitRunner.class)
public class EventBroadcasterTest {

    @Mock
    private Room room;
    @Mock
    private EventReceiver eventReceiver1, eventReceiver2;

    private EventBroadcaster eventBroadcaster;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        eventBroadcaster = new EventBroadcaster(room);
    }

    @After
    public void tearDown() throws Exception {
        eventBroadcaster = null;
    }

    // Check if the message comes to the right event receiver
    @Test
    public void testMessageReception(){
        eventBroadcaster.subscribe("test1", eventReceiver1);
        eventBroadcaster.subscribe("test2", eventReceiver2);

        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test1");

        eventBroadcaster.onRoomMessage(data);

        // Test data
        Vector<Object> data2 = new Vector<>();
        data2.add(1);
        data2.add("test2");

        eventBroadcaster.onRoomMessage(data2);

        // To verify that the right event receiver is being called the message is received
        verify(eventReceiver1).handleEvent("test1", data.subList(0, data.size() - 1));
        verify(eventReceiver2, never()).handleEvent("test1", data.subList(0, data.size() - 1));

        // To verify that the right event receiver is being called the message is received
        verify(eventReceiver2).handleEvent("test2", data.subList(0, data.size() - 1));
        verify(eventReceiver1, never()).handleEvent("test2", data.subList(0, data.size() - 1));
    }

    // Check if the message reaches the correct room with correct data
    @Test
    public void testPublish(){
        // Add an event receiver for the event "test".
        eventBroadcaster.subscribe("test", eventReceiver1);

        // Test data
        List<Object> data = new ArrayList<>();
        data.add(1);
        data.add(23);
        eventBroadcaster.publish("test", data);

        // Publish method adds the event type at the end
        List<Object> roomData = new ArrayList<>(data);
        roomData.add("test");

        // Verify that the right data reaches the room.
        verify(room).onEventBroadcasterMessage(roomData);
        // Verify that the event has also been received by the local receiver.
        verify(eventReceiver1, times(1)).handleEvent("test", data);
    }

    // Check if message goes to an unsubscribed event receiver
    @Test
    public void testUnsubscribe(){
        // Subscribe to "test" events.
        eventBroadcaster.subscribe("test", eventReceiver1);
        eventBroadcaster.subscribe("test", eventReceiver2);

        // Publish a first event.
        List<Object> data1 = new ArrayList<>();
        data1.add(1);
        eventBroadcaster.publish("test", data1);

        // Unsubscribe to "test" events.
        eventBroadcaster.unsubscribe("test", eventReceiver1);

        // Publish a second event.
        List<Object> data2 = new ArrayList<>();
        data2.add(2);
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
    public void testOnPublishLocalEvents(){
        // Subscribe to "ecology:connected" events.
        eventBroadcaster.subscribe("ecology:connected", eventReceiver1);

        // Event broadcaster publishes a local event
        eventBroadcaster.publishLocalEvent("ecology:connected", new ArrayList<Object>());

        // To verify if the right event receiver receives the message
        verify(eventReceiver1, times(1)).handleEvent("ecology:connected", new ArrayList<Object>());
        verify(eventReceiver2, never()).handleEvent("ecology:connected", new ArrayList<Object>());
    }

    @Test
    public void testEventReceiverDataModification(){
        eventBroadcaster.subscribe("test", new EventReceiver() {
            @Override
            public void handleEvent(String eventType, List<Object> eventData) {
                // First event receiver modifies the received data
                try {
                    eventData.add(2);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        eventBroadcaster.subscribe("test", eventReceiver2);

        // Test data
        Vector<Object> originalData = new Vector<>();
        originalData.add(1);
        originalData.add(3);
        originalData.add("test");

        Vector<Object> modifiedData = new Vector<>(originalData);
        // Event receiver tried to add 2 - but eventually it won't be able to add
        modifiedData.add(2);

        // Event broadcaster receives the message from the room
        eventBroadcaster.onRoomMessage(originalData);

        // To verify that the original data reaches the second event receiver
        verify(eventReceiver2, times(1)).handleEvent("test", originalData.subList(0, originalData.size() - 1));

        // To verify that the modified data didn't reach the second event receiver
        verify(eventReceiver2, never()).handleEvent("test", modifiedData.subList(0, modifiedData.size() - 1));
    }

}