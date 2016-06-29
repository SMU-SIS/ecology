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

    private String[] eventType = new String[10];

    public EventBroadcaster(Ecology ecology){
        this.ecology = ecology;
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

    public void publish(final String eventType, Collection<Object> args) {

        Vector<Object> data = new Vector<>(args);
        Log.i(TAG, "eventType "+eventType);
        //Add eventtype at the end.
        data.addElement(eventType);
        OSCMessage oscMessage = new OSCMessage("/event", data);

        //Send eve
        if (socketCreator != null) {
            byte [] eventData = oscMessage.getByteArray();
            socketCreator.write(eventData);
        }

        //Add device id at the end for dependent devices.
        if(messageapi) {
            Log.i(TAG, "Message api");
            String deviceId = ecology.getAndroid_id();
            //If message api add device ID at the end
            data.addElement(deviceId);
        }

        oscMessage = new OSCMessage("/event", data);
        byte[] forwardRequestData = oscMessage.getByteArray();

        if (eventType.equals("launch")) {
            MESSAGE_PATH = START_ACTIVITY_PATH_1;
        } else {
            MESSAGE_PATH = MESSAGE_PATH_EVENT;
        }

        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, forwardRequestData).setResultCallback(
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

    public void forward(Collection<Object> args, Boolean forwardCoreRequired){

        Vector<Object> data = new Vector<>(args);
        Log.i(TAG, "data "+data);
        OSCMessage oscMessage = new OSCMessage("/event", data);
        byte[] forwardRequestData = oscMessage.getByteArray();

        //If forward to a core device is required
        if (socketCreator != null && forwardCoreRequired) {
            Log.i(TAG, "forward");
            //Remove device id.
            data.removeElementAt(data.size() - 1);
            Log.i(TAG, "data " + data);
            oscMessage = new OSCMessage("/event", data);
            byte[] eventData = oscMessage.getByteArray();
            socketCreator.write(eventData);
        }

        //Forward to dependent devices - keep device id.
        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, forwardRequestData).setResultCallback(
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
