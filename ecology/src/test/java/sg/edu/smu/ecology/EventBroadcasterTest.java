package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Vector;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created by anurooppv on 26/7/2016.
 */
public class EventBroadcasterTest {

    @Mock
    private Room roomA;
    @Mock
    private EventReceiver eventReceiver1, eventReceiver2;

    private EventBroadcaster eventBroadcaster;


    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        eventBroadcaster = null;
    }

    // Check if the message comes to the right event receiver
    @Test
    public void testMessageReception(){

        eventBroadcaster = new EventBroadcaster(roomA);

        eventBroadcaster.subscribe("test1", eventReceiver1);
        eventBroadcaster.subscribe("test2", eventReceiver2);

        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test1");

        eventBroadcaster.onRoomMessage(data);

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

    // Check if the message is reaching the correct room
    @Test
    public void testPublish(){

        eventBroadcaster = new EventBroadcaster(roomA);

        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add(23);
        eventBroadcaster.publish("test", data);

        // Publish method adds the event type at the end
        Vector<Object> roomData = new Vector<>();
        roomData.add(1);
        roomData.add(23);
        roomData.add("test");
        // To verify if the right data reaches the right room
        verify(roomA).onEventBroadcasterMessage(roomData);
    }

    // Check if message goes to an unsubscribed event receiver
    @Test
    public void testUnsubscribe(){
        eventBroadcaster = new EventBroadcaster(roomA);

        eventBroadcaster.subscribe("test1", eventReceiver1);

        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("test1");

        eventBroadcaster.unsubscribe("test1", eventReceiver1);
        eventBroadcaster.onRoomMessage(data);
        // To verify if event receiver is called or not
        verify(eventReceiver1, never()).handleEvent("test1", data.subList(0, data.size() - 1));
    }
}