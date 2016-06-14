package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class EventBroadcaster {

    public SocketCreator socketCreator;
    private final String TAG = "EVENT BROADCASTER";
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

    private String[] eventType = new String[10];

    public EventBroadcaster(Ecology ecology){
        this.ecology = ecology;
        event = new Event();
    }

    public void subscribe(String eventType, Ecology.EventReceiver eventReceiver){
        this.eventType[index++] = eventType;
        ecology.setEventReceiver(eventReceiver);
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

        byte[] eventData = pack((Parcelable) event);

        if(messageapi) {
            Log.i(TAG, "Message api");
            dependentEvent.setEvent(event);
            forwardrequestData = pack((Parcelable) dependentEvent);
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

        forwardrequestData = pack((Parcelable) dependentEvent);
        byte[] eventData = pack((Parcelable) event);

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
        Log.i(TAG, "eventType" + eventType);
        Log.i(TAG, "index" + index);
        return eventType;
    }

    public static byte[] pack(Parcelable parcelable) {
        Parcel parcel = Parcel.obtain();
        parcelable.writeToParcel(parcel, 0);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes;
    }

}
