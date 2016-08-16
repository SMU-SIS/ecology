package sg.edu.smu.ecology;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by anurooppv on 22/7/2016.
 */
public class MsgApiConnector implements Connector, GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener {

    private final static String TAG = MsgApiConnector.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private String nodeId = null;
    private static final String CAPABILITY_NAME = "mobile_news_feed_controller";
    private static final String MESSAGE_PATH_EVENT = "/mobile_news_feed_controller";
    private static final String START_ACTIVITY_PATH_1 = "/start_mobile_activity";
    private Connector.Receiver receiver;

    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;

    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;

    @Override
    public void sendMessage(List<Object> message) {
        // Retrieve eventType
        final String eventType = (String) message.get(message.size() - 3);

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
            MESSAGE_PATH = START_ACTIVITY_PATH_1;
        } else {
            MESSAGE_PATH = MESSAGE_PATH_EVENT;
        }

        if (nodeId != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
                    MESSAGE_PATH, messageDataToSend).setResultCallback(
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

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    private void setupMessageApiConnection() {

        // Handle the results of the capability checker thread.
        final Handler handler = new Handler();

        // Manually retrieving the results of reachable nodes with the message_wearable capability
        new Thread("CapCheck") {
            @Override
            public void run() {
                final CapabilityApi.GetCapabilityResult result =
                        Wearable.CapabilityApi.getCapability(
                                googleApiClient, CAPABILITY_NAME,
                                CapabilityApi.FILTER_REACHABLE).await();

                // Handle the results through the handlers to get out of this thread.
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            nodeId = updateMessageCapability(result.getCapability());
                            Log.i(TAG, "nodeId " + nodeId);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }.start();

        //Registering the listener and retrieving the results of reachable nodes with the message_wearable capability
        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        try {
                            nodeId = updateMessageCapability(capabilityInfo);
                            Log.i(TAG, "nodeId " + nodeId);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                googleApiClient,
                capabilityListener,
                CAPABILITY_NAME);

    }

    //Determining the best node to use
    private String updateMessageCapability(CapabilityInfo capabilityInfo) throws IOException {
        Set<Node> connectedNodes = capabilityInfo.getNodes();

        nodeId = pickBestNodeId(connectedNodes);
        Log.i(TAG, "nodeId " + nodeId);

        if (nodeId != null) {
            onConnectorConnected = true;
            receiver.onConnectorConnected();
        }

        return nodeId;
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        Log.i(TAG, "pickBestNodeId " + bestNodeId);
        return bestNodeId;
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
