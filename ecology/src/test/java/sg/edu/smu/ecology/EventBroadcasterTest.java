package sg.edu.smu.ecology;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by anurooppv on 26/7/2016.
 */
public class EventBroadcasterTest {

    @Mock
    private Room roomA, roomB;
    private EventBroadcaster eventBroadcasterA, eventBroadcasterB;
    private EventReceiver eventReceiver1, eventReceiver2;

    @Before
    public void setUp() throws Exception {
        String nameA = "roomA";
        String nameB = "roomB";

        Ecology ecologyA = Mockito.mock(Ecology.class);
        Ecology ecologyB = Mockito.mock(Ecology.class);

        eventReceiver1 = Mockito.mock(EventReceiver.class);
        eventReceiver2 = Mockito.mock(EventReceiver.class);

        roomA = new Room(nameA, ecologyA);
        roomB = new Room(nameB, ecologyB);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSubscribe() throws Exception {
        eventBroadcasterA = roomA.getEventBroadcaster();
        String eventType = "test";

        eventBroadcasterA.subscribe(eventType, eventReceiver1);

        Vector<EventReceiver> eventReceivers1 = new Vector<>();
        eventReceivers1.add(eventReceiver1);

        Vector<EventReceiver> eventReceivers2 = new Vector<>();
        eventReceivers2.add(eventReceiver1);
        eventReceivers2.add(eventReceiver2);

        // Check if the event receiver is added to the list for the subscribed event type
        assertEquals(eventReceivers1, eventBroadcasterA.getEventReceivers(eventType));
        assertNotEquals(eventReceivers2 ,eventBroadcasterA.getEventReceivers(eventType));
    }

    @Test
    public void testUnsubscribe() throws Exception {
        eventBroadcasterA = roomA.getEventBroadcaster();
        String eventType = "test";

        eventBroadcasterA.subscribe(eventType, eventReceiver1);
        //eventBroadcasterA.subscribe(eventType, eventReceiver2);

        eventBroadcasterA.unsubscribe(eventType, eventReceiver1);

        // Check if the event receiver is removed from the list for the unsubscribed event type
        assertNull(eventBroadcasterA.getEventReceivers(eventType));
    }

    @Test
    public void testPublish() throws Exception {
        eventBroadcasterA = roomA.getEventBroadcaster();
        Vector<Object> data = new Vector<>();
        data.add(1);
        data.add("value");

        eventBroadcasterA.publish("test", data);
        //assertEquals(data, roomA.getMessage());

        // message will have the event type added at the end
        data.add("test");

        // Check to see if the correct message has reached the correct room
        assertEquals(data, roomA.getMessage());
    }


    @Test
    public void testCheckCorrectRoom(){
        eventBroadcasterB = roomA.getEventBroadcaster();

        //assertEquals(roomB, eventBroadcasterB.getRoom());
        assertEquals(roomA, eventBroadcasterB.getRoom());
    }
}