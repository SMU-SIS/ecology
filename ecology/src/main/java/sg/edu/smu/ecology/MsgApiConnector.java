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

import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import sg.edu.smu.ecology.encoding.MessageDecoder;
import sg.edu.smu.ecology.encoding.MessageEncoder;

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
    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;

    @Override
    public void sendMessage(List<Object> message) {
        // Retrieve eventType
        final String eventType = (String) message.get(message.size() - 2);

        byte[] encodedMessageData = encodeMessage(message);
        Log.i(TAG, "data " + Arrays.toString(encodedMessageData));

        String messagePath = " ";
        if (eventType.equals("launch")) {
            messagePath = START_ACTIVITY_PATH;
        } else {
            messagePath = MESSAGE_PATH_EVENT;
        }

        writeData(encodedMessageData, messagePath);
    }

    /**
     * Encode the message to be sent
     *
     * @param message the message to be encoded
     * @return the encoded message
     */
    private byte[] encodeMessage(List<Object> message) {
        MessageEncoder messageEncoder = new MessageEncoder();

        byte[] encodedMessage = new byte[0];

        try {
            encodedMessage = messageEncoder.encode(message);
        } catch (CharacterCodingException e) {
            e.printStackTrace();
        }

        return encodedMessage;
    }

    /**
     * Write the data to be sent
     *
     * @param encodedMessage the encoded message ready to be sent
     * @param messagePath    the message path of the message
     */
    private void writeData(byte[] encodedMessage, String messagePath) {
        if (nodeId.size() > 0) {
            for (String nodeIdValue : nodeId) {
                Log.i(TAG, "node " + nodeIdValue);
                Wearable.MessageApi.sendMessage(googleApiClient, nodeIdValue,
                        messagePath, encodedMessage).setResultCallback(
                        new ResultCallback<MessageApi.SendMessageResult>() {
                            @Override
                            public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                                Log.i(TAG, "MessageData Sent ");

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
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    /**
     * Connect to the ecology.
     */
    @Override
    public void connect(Context context, String deviceId) {

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
                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();

                // Handle the results through the handlers to get out of this thread.
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (Node node : nodes.getNodes()) {
                            nodeId.add(node.getId());
                            Log.i(TAG, "Node Id " + node.getId());
                        }

                        if (nodeId.size() > 0) {
                            onConnectorConnected = true;
                            receiver.onDeviceConnected(nodeId.get(0));
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
        MessageDecoder messageDecoder = new MessageDecoder();

        List<Object> data;
        data = messageDecoder.decode(messageEvent.getData());
        Log.i(TAG, "Data received" + data);

        receiver.onMessage(data);
    }

}
