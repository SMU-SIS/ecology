package sg.edu.smu.ecology;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
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
    private final int BUFFER_SIZE = 1024;

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

        IoBuffer ioBuffer = IoBuffer.allocate(BUFFER_SIZE);

        //Log.i(TAG, "eventType "+eventType);

        //Add eventtype at the end.
        data.addElement(eventType);

        DataEncoder dataEncoder = new DataEncoder();

        //Send event via WiFiP2P
        if (socketCreator != null) {

            MessageData messageData = new MessageData();
            messageData.setAddress("/event");

            for(int i = 0; i<data.size(); i++){
                messageData.addArgument(data.get(i));
            }

            try {
                dataEncoder.encodeMessage(messageData, ioBuffer);
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }

            int length = ioBuffer.position();
            byte [] eventData = ioBuffer.array();
            byte [] eventDataToSend = Arrays.copyOfRange(eventData, 0 , length);

            //Write length of the data first
            socketCreator.writeInt(length);

            //Write the byte data
            socketCreator.writeData(eventDataToSend);
            ioBuffer.clear();
        }

        //Add device id at the end for dependent devices.
        if (messageapi) {
            //Log.i(TAG, "Message api");
            String deviceId = ecology.getAndroid_id();
            //If messageData api add device ID at the end
            data.addElement(deviceId);

            MessageData messageData1 = new MessageData();
            messageData1.setAddress("/event");

            //Log.i(TAG, "Data " + data);
            for (int i = 0; i < data.size(); i++) {
                messageData1.addArgument(data.get(i));
            }

            try {
                dataEncoder.encodeMessage(messageData1, ioBuffer);
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }
        }

        int length = ioBuffer.position();
        byte[] forwardRequestData = ioBuffer.array();
        byte [] forwardRequestDataToSend = Arrays.copyOfRange(forwardRequestData, 0, length);
        //Log.i(TAG, "data " + Arrays.toString(forwardRequestData));

        if (eventType.equals("launch")) {
            MESSAGE_PATH = START_ACTIVITY_PATH_1;
        } else {
            MESSAGE_PATH = MESSAGE_PATH_EVENT;
        }

        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, forwardRequestDataToSend).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(TAG, "MessageData Sent " + eventType);

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send messageData
                                Log.i(TAG, "MessageData Failed");
                            }
                        }
                    }

            );
        } else {
            // Unable to retrieve node with transcription capability
            Log.i(TAG, "Message not sent - Node Id is null ");
        }

        ioBuffer.clear();

    };

    public void forward(Collection<Object> args, Boolean forwardCoreRequired){

        Vector<Object> data = new Vector<>(args);
        //Log.i(TAG, "data " + data);

        MessageData messageData = new MessageData();
        messageData.setAddress("/event");
        DataEncoder dataEncoder = new DataEncoder();

        IoBuffer ioBuffer = IoBuffer.allocate(BUFFER_SIZE);

        for(int i = 0; i<data.size(); i++){
            messageData.addArgument(data.get(i));
        }

        try {
            dataEncoder.encodeMessage(messageData, ioBuffer);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }

        int length = ioBuffer.position();
        byte[] forwardRequestData = ioBuffer.array();
        byte [] forwardRequestDataToSend = Arrays.copyOfRange(forwardRequestData, 0, length);
        //Log.i(TAG, "data " + Arrays.toString(forwardRequestData));

        //Forward to dependent devices - keep device id.
        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, forwardRequestDataToSend).setResultCallback(
                    new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.i(TAG, "MessageData Sent " + eventType);

                            if (!sendMessageResult.getStatus().isSuccess()) {
                                // Failed to send messageData
                                Log.i(TAG, "MessageData Failed");
                            }
                        }
                    }

            );
        } else {
            // Unable to retrieve node with transcription capability
            Log.i(TAG, "MessageData not sent - Node Id is null ");
        }

        ioBuffer.clear();

        //If forward to a core device is required
        if (socketCreator != null && forwardCoreRequired) {
            Log.i(TAG, "forward");
            //Remove device id.
            data.removeElementAt(data.size() - 1);

            MessageData messageData1 = new MessageData();
            messageData1.setAddress("/event");

            //Log.i(TAG, "data " + data);
            for(int i = 0; i<data.size(); i++){
                messageData1.addArgument(data.get(i));
            }

            try {
                dataEncoder.encodeMessage(messageData1, ioBuffer);
            } catch (CharacterCodingException e) {
                e.printStackTrace();
            }

            int lengthData = ioBuffer.position();
            byte [] eventData = ioBuffer.array();
            byte [] eventDataToSend = Arrays.copyOfRange(eventData, 0, lengthData);

            //Write length of the data first
            socketCreator.writeInt(lengthData);

            //Write the byte data
            socketCreator.writeData(eventDataToSend);
            ioBuffer.clear();
        }

    }

    public String[] getEventTypes() {
        return eventType;
    }

}
