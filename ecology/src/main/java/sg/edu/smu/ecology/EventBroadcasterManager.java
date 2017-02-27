package sg.edu.smu.ecology;

import android.content.Context;
import android.os.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anurooppv on 24/2/2017.
 */

class EventBroadcasterManager {
    private static final String TAG = EventBroadcasterManager.class.getSimpleName();
    private Map<Context, EventBroadcaster> eventBroadcastersMap = new HashMap<>();
    private Map<Context, Handler> handlersMap = new HashMap<>();
    private Handler ecologyLooperHandler;
    private Room room;

    EventBroadcasterManager(Room room) {
        this.room = room;
        ecologyLooperHandler = room.getEcology().getHandler();
    }

    void addEventBroadcaster(Context context, EventBroadcaster eventBroadcaster) {
        eventBroadcastersMap.put(context, eventBroadcaster);
        addHandler(context);
    }

    EventBroadcaster getEventBroadcaster(Context context) {
        return eventBroadcastersMap.get(context);
    }

    /**
     * Add a handler for a particular context
     *
     * @param context the context
     */
    private void addHandler(Context context) {
        handlersMap.put(context, new Handler(context.getMainLooper()));
    }

    /**
     * Get the handler associated with the context
     *
     * @param context the context
     * @return the handler associated with the context
     */
    private Handler getHandler(Context context) {
        return handlersMap.get(context);
    }

    /**
     * Send a message from context looper to ecology looper
     *
     * @param msg the message to be sent
     */
    void sendMessage(final EcologyMessage msg) {
        ecologyLooperHandler.post(new Runnable() {
            @Override
            public void run() {
                room.onEventBroadcasterMessage(msg);
            }
        });
    }

    /**
     * Pass the message from ecology looper to context looper
     *
     * @param msg the message received
     */
    void forwardMessage(final EcologyMessage msg) {
        for (final Map.Entry<Context, EventBroadcaster> entry :
                getEventBroadcastersMap().entrySet()) {
            post(new Runnable() {
                @Override
                public void run() {
                    entry.getValue().onRoomMessage(msg);
                }
            }, entry.getKey());
        }
    }

    /**
     * Post a local event
     *
     * @param eventType the event type
     * @param data      the event data
     */
    void postLocalEvent(final String eventType, final List<Object> data) {
        for (final Map.Entry<Context, EventBroadcaster> entry :
                getEventBroadcastersMap().entrySet()) {
            post(new Runnable() {
                @Override
                public void run() {
                    entry.getValue().publishLocalEvent(eventType, data);
                }
            }, entry.getKey());
        }
    }

    /**
     * Post a runnable to the message queue. It wil be run in a thread attached to the handler
     *
     * @param task    the task to be added to the queue
     * @param context the context
     */
    private void post(Runnable task, Context context) {
        getHandler(context).post(task);
    }

    private Map<Context, EventBroadcaster> getEventBroadcastersMap() {
        return eventBroadcastersMap;
    }
}
