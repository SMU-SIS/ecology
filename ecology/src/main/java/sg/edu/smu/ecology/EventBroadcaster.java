package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import java.util.Collection;
import java.util.Vector;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class EventBroadcaster {

    private final static String TAG = EventBroadcaster.class.getSimpleName();

    public SocketCreator socketCreator;
    private GoogleApiClient googleApiClient;
    private String nodeId = null;
    private int index = 0;
    private Ecology ecology;
    private static String MESSAGE_PATH = " ";
    private static final String MESSAGE_PATH_EVENT = "/mobile_news_feed_controller";
    private static final String START_ACTIVITY_PATH_1 = "/start_mobile_activity";
    private boolean messageapi = false;
    private String deviceID;

    private String[] eventType = new String[10];

    public EventBroadcaster(Ecology ecology) {
        this.ecology = ecology;
    }

    public void subscribe(String eventType, Ecology.EventReceiver eventReceiver) {
        this.eventType[index++] = eventType;
        ecology.setEventReceiver(eventReceiver);
    }

    ;

    public void unsubscribe(String unsubEventType, Ecology.EventReceiver eventReceiver) {
        for (String anEventType : eventType) {
            if (unsubEventType.equals(anEventType)) {
                unsubEventType = null;
            }
        }
    }

    ;

    public void setMessageapi(boolean messageapi) {
        this.messageapi = messageapi;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public void setGoogleApiClient(GoogleApiClient googleApiClient) {
        this.googleApiClient = googleApiClient;
    }

    public void setSocketCreator(SocketCreator socketCreator) {
        this.socketCreator = socketCreator;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public void publish(final String eventType, Collection<Object> args) {

        Vector<Object> data = new Vector<>(args);
        data.addElement(eventType);

        JavaToByteArrayConverter javaToByteArrayConverter = new JavaToByteArrayConverter();

        // For message api, add the deviceID at the end of the data.
        if (messageapi) {
            Log.i(TAG, "Message api");
            data.addElement(deviceID);
            Log.i(TAG, "DeviceId " + deviceID);
        }
        // Write the type of the data.
        javaToByteArrayConverter.writeTypes(data);
        // Write every arguments.
        for(Object o: data){
            javaToByteArrayConverter.write(o);
        }
        // Transform the data into a byte array.
        byte[] eventData = javaToByteArrayConverter.toByteArray();
        Log.i(TAG, "eventdata " + eventData);

        if (socketCreator != null) {
            // Send the data with wifi direct.
            socketCreator.write(eventData);
        }


        if (eventType.equals("launch")) {
            MESSAGE_PATH = START_ACTIVITY_PATH_1;
        } else {
            MESSAGE_PATH = MESSAGE_PATH_EVENT;
        }

        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, eventData).setResultCallback(
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

    public void forward(Collection<Object> args1, Collection<Object> args2, Boolean forwardRequired) {

        JavaToByteArrayConverter javaToByteArrayConverter1 = new JavaToByteArrayConverter();

        javaToByteArrayConverter1.writeTypes(args1);
        byte[] forwardrequestData = javaToByteArrayConverter1.toByteArray();

        javaToByteArrayConverter1.writeTypes(args2);
        byte[] eventData = javaToByteArrayConverter1.toByteArray();

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
