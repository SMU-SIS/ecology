package sg.edu.smu.ecology;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Created by anurooppv on 21/7/2016.
 */
public class EventBroadcasterTest {

    private EventBroadcaster eventBroadcaster;

    @Before
    public void setUp() throws Exception {
        eventBroadcaster = new EventBroadcaster(new Room("roomAB", new Ecology(new Connector() {
            @Override
            public void sendMessage(List<Object> message) {

            }

            @Override
            public void addReceiver(Receiver receiver) {

            }

            @Override
            public void connect(Context activity) {

            }

            @Override
            public void disconnect() {

            }

            @Override
            public boolean isConnected() {
                return false;
            }
        })));
    }

    @Test
    public void testOnRoomMessage() throws Exception {

    }

    @Test
    public void testSubscribe() throws Exception {
        String event1 = "event1";
        String event2 = "event2";

        EventReceiver eventReceiver1 = new EventReceiver() {
            @Override
            public void handleEvent(String eventType, List<Object> eventData) {

            }
        };

        EventReceiver eventReceiver2 = new EventReceiver() {
            @Override
            public void handleEvent(String eventType, List<Object> eventData) {

            }
        };

        eventBroadcaster.subscribe(event1, eventReceiver1);
    }

    @Test
    public void testUnsubscribe() throws Exception {

    }

    @Test
    public void testPublish() throws Exception {

    }
}