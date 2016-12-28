package sg.edu.smu.ecology;

import android.os.Message;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anurooppv on 25/10/2016.
 */

/**
 * This class helps the device to act as a server to establish bluetooth connections with other
 * client devices in the ecology
 */
public class BluetoothServerConnector extends BluetoothConnector {
    private static final String TAG = BluetoothServerConnector.class.getSimpleName();

    // To save the list of client connection threads
    private Map<Integer, BluetoothSocketReadWriter> clientConnectionThreadsList = new HashMap<>();
    private BluetoothServerAcceptThread bluetoothServerAcceptThread;

    @Override
    public void setupBluetoothConnection() {
        // Start the thread to listen on a BluetoothServerSocket
        if (bluetoothServerAcceptThread == null) {
            try {
                bluetoothServerAcceptThread = new BluetoothServerAcceptThread(getBluetoothAdapter(),
                        getUuidsList(), getHandler());
                bluetoothServerAcceptThread.start();
            } catch (Exception e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
            }
        }
    }

    @Override
    public Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList() {
        return clientConnectionThreadsList.values();
    }

    /**
     * When a client device gets connected
     *
     * @param msg the message received
     */
    @Override
    public void onDeviceConnected(Message msg) {
        Log.d(TAG, "A client device has been connected");
        Object obj = msg.obj;

        updateClientsList((BluetoothSocketReadWriter) obj, msg.arg1);

        // Pass the server device Id to the connected client device.
        sendMessageToClient(Arrays.<Object>asList(getDeviceId(), Settings.DEVICE_ID_EXCHANGE),
                clientConnectionThreadsList.get(msg.arg1));

        // To notify the new client about the already connected client devices in the ecology
        sendConnectedClientsIds(msg.arg1, clientConnectionThreadsList.get(msg.arg1));
    }

    /**
     * When a receiver message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    @Override
    public void onReceiverMessage(Message msg, List<Object> messageData) {
        forwardMessage((byte[]) msg.obj, msg.arg1);
        super.onReceiverMessage(msg, messageData);
    }

    /**
     * When a client device gets disconnected
     *
     * @param msg the message received
     */
    @Override
    public void onDeviceDisconnected(Message msg) {
        Object disconnectedObj = msg.obj;

        // A client device has been disconnected
        getReceiver().onDeviceDisconnected(getDeviceIdsList().get(msg.arg1));

        updateClientsList((BluetoothSocketReadWriter) disconnectedObj, msg.arg1);

        // To notify other connected client devices in the ecology
        sendConnectorMessage(Arrays.<Object>asList(getDeviceIdsList().get(msg.arg1),
                Settings.DEVICE_DISCONNECTED), getBluetoothSocketReadWriterList());

        // Update the connected devices list
        getDeviceIdsList().remove(msg.arg1);

        handleClientDisconnection(msg.arg1);
    }

    /**
     * When a connector message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    @Override
    public void onConnectorMessage(Message msg, List<Object> messageData) {
        String deviceIdReceived = (String) messageData.get(messageData.size() - 3);

        forwardMessage((byte[]) msg.obj, msg.arg1);

        getDeviceIdsList().put(msg.arg1, deviceIdReceived);

        getReceiver().onDeviceConnected(deviceIdReceived);
    }

    /**
     * This method updates the list accordingly when a client gets connected or disconnected.
     *
     * @param bluetoothSocketReadWriter the thread associated with the client
     * @param clientId                  the unique id of the client
     */
    private void updateClientsList(BluetoothSocketReadWriter bluetoothSocketReadWriter,
                                   int clientId) {
        if (clientConnectionThreadsList.get(clientId) != null) {
            clientConnectionThreadsList.remove(clientId);
            Log.i(TAG, "Removed from clients list " + clientConnectionThreadsList.size());
        } else {
            clientConnectionThreadsList.put(clientId, bluetoothSocketReadWriter);
            Log.i(TAG, "Added to clients list " + clientConnectionThreadsList.size());
        }
    }

    @Override
    public void disconnect() {
        super.disconnect();

        if (bluetoothServerAcceptThread != null && !bluetoothServerAcceptThread.isInterrupted()) {
            bluetoothServerAcceptThread.interrupt();
        }
    }

    /**
     * Handle the client disconnection
     *
     * @param clientId the client that got disconnected
     */
    private void handleClientDisconnection(int clientId) {
        bluetoothServerAcceptThread.handleClientDisconnection(clientId);
    }

    /**
     * When a server receives a message from a client, it is forwarded to rest of the connected
     * clients
     *
     * @param dataByteArray the data byte array received
     * @param clientId      the client id of the client from which the message was received
     */
    private void forwardMessage(byte[] dataByteArray, int clientId) {
        // Store the client from which the message was received
        BluetoothSocketReadWriter sender = clientConnectionThreadsList.get(clientId);

        for (BluetoothSocketReadWriter client : clientConnectionThreadsList.values()) {
            if (client != sender) {
                client.writeInt(dataByteArray.length);
                client.writeData(dataByteArray);
                Log.i(TAG, "Message forwarding...");
            }
        }
    }

    /**
     * Send the device Ids of already connected client devices to the newly connected client device
     *
     * @param clientId               the client Id of the new client device
     * @param clientConnectionThread the client thread of the new client device
     */
    private void sendConnectedClientsIds(int clientId,
                                         BluetoothSocketReadWriter clientConnectionThread) {

        String newDeviceId = getDeviceIdsList().get(clientId);

        for (String deviceId : getDeviceIdsList().values()) {
            if (!deviceId.equals(newDeviceId)) {
                sendConnectorMessage(Arrays.<Object>asList(deviceId, Settings.DEVICE_ID_EXCHANGE),
                        Collections.singletonList(clientConnectionThread));
            }
        }
    }

    /**
     * Send a connector message to a particular client device
     *
     * @param message                the message to be sent
     * @param clientConnectionThread the client thread of the destination device
     */
    private void sendMessageToClient(List<Object> message,
                                     BluetoothSocketReadWriter clientConnectionThread) {
        sendConnectorMessage(message, Collections.singletonList(clientConnectionThread));
    }
}