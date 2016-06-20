package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.SerializationUtils;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class EventBroadcaster {

    private final static String TAG = EventBroadcaster.class.getSimpleName();

    public SocketCreator socketCreator;
    private GoogleApiClient googleApiClient;
    private String nodeId = null;
    private Event event;
    private DependentEvent dependentEvent;
    private int index = 0;
    private Ecology ecology;
    private static String MESSAGE_PATH = " ";
    private static final String MESSAGE_PATH_EVENT = "/mobile_news_feed_controller";
    private static final String START_ACTIVITY_PATH_1 = "/start_mobile_activity";
    private boolean messageapi = false;
    private byte[] forwardrequestData;
    private byte[] eventData;

    private String[] eventType = new String[10];

    public EventBroadcaster(Ecology ecology){
        this.ecology = ecology;
        event = new Event();
    }

    public void subscribe(String eventType, Ecology.EventReceiver eventReceiver){
        this.eventType[index++] = eventType;
        ecology.setEventReceiver(eventReceiver);
        ecology.setEvent(event);
    };

    public void unsubscribe(String unsubEventType, Ecology.EventReceiver eventReceiver){
        for (String anEventType : eventType) {
            if (unsubEventType.equals(anEventType)) {
                unsubEventType = null;
            }
        }
    };

    public void setDependentEvent(DependentEvent dependentEvent) {
        this.dependentEvent = dependentEvent;
    }

    public void setMessageapi(boolean messageapi) {
        this.messageapi = messageapi;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    public void setSocketCreator(SocketCreator socketCreator){
        this.socketCreator = socketCreator;
    }

    public void publish(final String eventType, Bundle data) {

        event.setType(eventType);
        event.setData(data);

        eventData = SerializationUtils.serialize(event);

        if(messageapi) {
            Log.i(TAG, "Message api");
            dependentEvent.setEvent(event);
            forwardrequestData = SerializationUtils.serialize(dependentEvent);
        }

            if (socketCreator != null)
                socketCreator.write(eventData);


            if (eventType.equals("launch")) {
                MESSAGE_PATH = START_ACTIVITY_PATH_1;
            } else {
                MESSAGE_PATH = MESSAGE_PATH_EVENT;
            }

            if (nodeId != null) {
                Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                        MESSAGE_PATH, forwardrequestData).setResultCallback(
                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                Log.i(TAG, "Message Sent " + eventType);

                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    // Failed to send message
                                    Log.i(TAG, "Message Failed");
                                }
                            }
                        }

                );
            } else {
                // Unable to retrieve node with transcription capability
                Log.i(TAG, "Message not sent - Node Id is null ");
            }


    };

    public void forward(DependentEvent dependentEvent, Event event, Boolean forwardRequired){

        forwardrequestData = SerializationUtils.serialize(dependentEvent);
        eventData = SerializationUtils.serialize(event);

        if (socketCreator != null && forwardRequired) {
            Log.i(TAG, "forward");
            socketCreator.write(eventData);
        }

        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, forwardrequestData).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(TAG, "Message Sent " + eventType);

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send message
                                Log.i(TAG, "Message Failed");
                            }
                        }
                    }

            );
        } else {
            // Unable to retrieve node with transcription capability
            Log.i(TAG, "Message not sent - Node Id is null ");
        }
    }

    public String[] getEventTypes() {
        return eventType;
    }

}
