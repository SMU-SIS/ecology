package sg.edu.smu.ecology;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.mina.core.buffer.IoBuffer;

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import sg.edu.smu.ecology.encoding.DataDecoder;
import sg.edu.smu.ecology.encoding.DataEncoder;
import sg.edu.smu.ecology.encoding.MessageData;

/**
 * Created by anurooppv on 22/7/2016.
 */
public class MsgApiConnector implements Connector, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {

    private final static String TAG = MsgApiConnector.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    // To store the node Id of all the connected devices
    private List<String> nodeId = new Vector<>();
    private static final String MESSAGE_PATH_EVENT = "/ecology_message";
    private static final String START_ACTIVITY_PATH = "/start_mobile_activity";
    private Connector.Receiver receiver;

    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;

    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;

    @Override
    public void sendMessage(List<Object> message) {
        // Retrieve eventType
        final String eventType = (String) message.get(message.size() - 2);

        DataEncoder dataEncoder = new DataEncoder();
        IoBuffer ioBuffer = IoBuffer.allocate(BUFFER_SIZE);

        MessageData messageData = new MessageData();

        Log.i(TAG, "Data " + message);
        for (int i = 0; i < message.size(); i++) {
            messageData.addArgument(message.get(i));
        }

        try {
            dataEncoder.encodeMessage(messageData, ioBuffer);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }

        // To store the length of the message
        int length = ioBuffer.position();

        // Contains the whole IoBuffer
        byte[] messageByteData = ioBuffer.array();

        // Actual message data is retrieved
        byte[] messageDataToSend = Arrays.copyOfRange(messageByteData, 0, length);
        Log.i(TAG, "data " + Arrays.toString(messageDataToSend));

        String MESSAGE_PATH = " ";
        if (eventType.equals("launch")) {
            MESSAGE_PATH = START_ACTIVITY_PATH;
        } else {
            MESSAGE_PATH = MESSAGE_PATH_EVENT;
        }

        if (nodeId.size() > 0) {
            for (String nodeIdValue : nodeId) {
                Log.i(TAG, "node "+nodeIdValue);
                Wearable.MessageApi.sendMessage(googleApiClient, nodeIdValue,
                        MESSAGE_PATH, messageDataToSend).setResultCallback(
                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                Log.i(TAG, "MessageData Sent " + eventType);

                                if (!sendMessageResult.getStatus().isSuccess()) {
                                    // Failed to send messageData
                                    Log.i(TAG, "MessageData Failed");
                                }
                            }
                        }

                );
            }
        } else {
            // Unable to retrieve node with transcription capability
            Log.i(TAG, "Message not sent - Node Id is null ");
        }

        ioBuffer.clear();
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Connect to the ecology.
     */
    @Override
    public void connect(Context context) {

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        googleApiClient.connect();

        // Add listener to receive messages
        Wearable.MessageApi.addListener(googleApiClient, this);

    }

    /**
     * Disconnect from the ecology.
     */
    @Override
    public void disconnect() {
        onConnectorConnected = false;
        googleApiClient.disconnect();
    }

    @Override
    public boolean isConnected() {
        return onConnectorConnected;
    }

    private void setupMessageApiConnection() {

        // Handle the results of the capability checker thread.
        final Handler handler = new Handler();

        // Retrieve the list of nodes connected to the device.
        new Thread("CapCheck") {
            @Override
            public void run() {
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( googleApiClient ).await();

                // Handle the results through the handlers to get out of this thread.
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for(Node node : nodes.getNodes()) {
                            nodeId.add(node.getId());
                            Log.i(TAG, "Node Id "+node.getId());
                        }

                        if(nodeId.size() > 0){
                            onConnectorConnected = true;
                            receiver.onConnectorConnected();
                        }
                    }
                });
            }
        }.start();

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected");
        setupMessageApiConnection();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        DataDecoder dataDecoder = new DataDecoder();
        MessageData messageData = dataDecoder.convertMessage(messageEvent.getData(), messageEvent.getData().length);

        List<Object> data;
        data = messageData.getArguments();
        Log.i(TAG, "Data received" + data);

        receiver.onMessage(data);
    }

}
