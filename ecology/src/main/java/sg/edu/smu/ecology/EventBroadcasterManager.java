/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 */


package sg.edu.smu.ecology;

import android.content.Context;
import android.os.Handler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */

/**
 * This class is responsible for the creation of event broadcasters and also manage their associated
 * contexts.
 *
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
class EventBroadcasterManager {
    private static final String TAG = EventBroadcasterManager.class.getSimpleName();
    /**
     * Used to create event broadcaster instances
     */
    private final EventBroadcasterFactory eventBroadcasterFactory;
    /**
     * Map to store event broadcasters and it's associated contexts
     */
    private Map<Context, EventBroadcaster> eventBroadcastersMap = new HashMap<>();
    private Map<Context, Handler> handlersMap = new HashMap<>();
    private Room room;

    EventBroadcasterManager(Room room) {
        this(room, new EventBroadcasterFactory());
    }

    /**
     * Special constructor used for testing
     *
     * @param room                    the room that created this instance
     * @param eventBroadcasterFactory to create event broadcaster instances
     */
    EventBroadcasterManager(Room room, EventBroadcasterFactory eventBroadcasterFactory) {
        this.room = room;
        this.eventBroadcasterFactory = eventBroadcasterFactory;
    }

    void addEventBroadcaster(Context context, EventBroadcaster eventBroadcaster) {
        getEventBroadcastersMap().put(context, eventBroadcaster);
        addHandler(context, new Handler(context.getMainLooper()));
    }

    EventBroadcaster getEventBroadcaster(Context context) {
        if (getEventBroadcastersMap().get(context) == null) {
            EventBroadcaster eventBroadcaster = eventBroadcasterFactory.createEventBroadcaster(
                    new EventBroadcaster.Connector() {
                        @Override
                        public void onEventBroadcasterMessage(EcologyMessage message) {
                            sendMessage(message);
                        }
                    },
                    context, room.getEcology()
            );
            addEventBroadcaster(context, eventBroadcaster);
        }
        return eventBroadcastersMap.get(context);
    }

    /**
     * Get the ecology looper handler
     *
     * @return the ecology looper handler
     */
    private Handler getEcologyLooperHandler() {
        return room.getEcology().getHandler();
    }

    /**
     * Add a handler for a particular context
     *
     * @param context the context
     */
    void addHandler(Context context, Handler handler) {
        handlersMap.put(context, handler);
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
    private void sendMessage(final EcologyMessage msg) {
        getEcologyLooperHandler().post(new Runnable() {
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
                    entry.getValue().onRoomMessage(new EcologyMessage(msg));
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

    static class EventBroadcasterFactory {
        EventBroadcaster createEventBroadcaster(EventBroadcaster.Connector connector,
                                                Context context, Ecology ecology) {
            return new EventBroadcaster(connector, context, ecology);
        }
    }
}