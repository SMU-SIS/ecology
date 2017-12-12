package sg.edu.smu.ecology.connector.bluetooth;

import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sg.edu.smu.ecology.EcologyMessage;
import sg.edu.smu.ecology.Settings;


/**
 * This class helps the device to act as a server to establish bluetooth connections with other
 * client devices in the ecology.
 *
 * @author Anuroop PATTENA VANIYAR
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

    /**
     * Return the list of {@link BluetoothSocketReadWriter} threads
     *
     * @param targetType specifies the target type
     * @param targets    the target device ids for the message to be sent
     * @return the list of server client connection threads
     */
    @Override
    public Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList(
            Integer targetType, List<String> targets) {
        if (targetType == EcologyMessage.TARGET_TYPE_SPECIFIC) {
            return getSpecificBluetoothReadWriterList(targets);
        } else {
            return getBluetoothSocketReadWriterList();
        }
    }

    /**
     * When a client device gets connected
     *
     * @param msg the message received containing the details
     */
    @Override
    public void onDeviceConnected(Message msg) {
        Log.d(TAG, "A client device has been connected");
        Object obj = msg.obj;

        updateClientsList((BluetoothSocketReadWriter) obj, msg.arg1);
    }

    /**
     * When a receiver message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    @Override
    public void onReceiverMessage(Message msg, EcologyMessage messageData) {
        int targetType = messageData.getTargetType();
        Log.i(TAG, "targetType " + targetType);

        switch (targetType) {
            case EcologyMessage.TARGET_TYPE_SERVER:
                super.onReceiverMessage(msg, messageData);
                break;

            case EcologyMessage.TARGET_TYPE_BROADCAST:
                forwardMessage((byte[]) msg.obj, msg.arg1);
                super.onReceiverMessage(msg, messageData);
                break;

            case EcologyMessage.TARGET_TYPE_SPECIFIC:
                List<String> clientTargets = new ArrayList<>();
                for (String target : messageData.getTargets()) {
                    if (target.equals(getDeviceId())) {
                        super.onReceiverMessage(msg, messageData);
                    } else {
                        clientTargets.add(target);
                    }
                }
                if (clientTargets.size() > 0) {
                    messageData.setTargets(clientTargets);
                    sendMessage(messageData);
                }
                break;

            default:
                break;
        }
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

        // Update the connected devices list
        getDeviceIdsList().remove(msg.arg1);

        handleClientDisconnection(msg.arg1);
    }

    private Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList() {
        return clientConnectionThreadsList.values();
    }

    /**
     * When a connector message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    @Override
    public void onConnectorMessage(Message msg, EcologyMessage messageData) {
        String eventTypeReceived = (String) messageData.fetchArgument();
        String deviceIdReceived = (String) messageData.fetchArgument();

        if (eventTypeReceived.equals(Settings.DEVICE_ID_EXCHANGE)) {
            getDeviceIdsList().put(msg.arg1, deviceIdReceived);
            getReceiver().onDeviceConnected(deviceIdReceived);
        }
    }

    /**
     * When the bluetooth is turned off.
     */
    @Override
    public void onBluetoothOff() {
        handleDisconnection();

        // All the connected client devices will get disconnected
        for (String deviceId : getDeviceIdsList().values()) {
            getReceiver().onDeviceDisconnected(deviceId);
        }

        // Clear the connected device ids list
        getDeviceIdsList().clear();
    }

    @Override
    public void onBluetoothOutOfRange() {
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

    /**
     * Disconnect from the ecology
     */
    @Override
    public void disconnect() {
        super.disconnect();

        handleDisconnection();
    }

    /**
     * Handle the disconnection from ecology
     */
    private void handleDisconnection() {
        if (bluetoothServerAcceptThread != null && !bluetoothServerAcceptThread.isInterrupted()) {
            bluetoothServerAcceptThread.interrupt();
        }
        bluetoothServerAcceptThread = null;
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
                EcologyMessage msg = new EcologyMessage(Arrays.<Object>asList(false, deviceId,
                        Settings.DEVICE_ID_EXCHANGE));
                msg.setSource(getDeviceId());
                msg.setTargetType(EcologyMessage.TARGET_TYPE_SPECIFIC);
                sendConnectorMessage(msg, Collections.singletonList(clientConnectionThread));
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
                                     List<BluetoothSocketReadWriter> clientConnectionThread) {
        EcologyMessage msg = new EcologyMessage(message);
        msg.setSource(getDeviceId());
        msg.setTargetType(EcologyMessage.TARGET_TYPE_SPECIFIC);
        sendConnectorMessage(msg, clientConnectionThread);
    }

    /**
     * Get the Bluetooth read writers thread list of a given list of target devices.
     *
     * @param targets the targets for the message to be sent
     * @return the list of Bluetooth read writers thread list of th targets
     */
    private List<BluetoothSocketReadWriter> getSpecificBluetoothReadWriterList(List<String> targets) {
        List<BluetoothSocketReadWriter> bluetoothSocketReadWriterList = new ArrayList<>();

        Set<Integer> keySet = getDeviceIdsList().keySet();

        for (Integer key : keySet) {
            for (String target : targets) {
                if (getDeviceIdsList().get(key).equals(target)) {
                    bluetoothSocketReadWriterList.add(clientConnectionThreadsList.get(key));
                }
            }
        }
        return bluetoothSocketReadWriterList;
    }
}
