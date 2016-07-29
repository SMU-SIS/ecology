package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Vector;
import java.util.ArrayList;
import java.util.List;

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
        // Test data
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add(23);
        eventBroadcaster.publish("test", data);

        // Publish method adds the event type at the end
        Vector<Object> roomData = new Vector<>(data);
        roomData.add("test");

        // To verify if the right data reaches the right room
        verify(room).onEventBroadcasterMessage(roomData);
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
}