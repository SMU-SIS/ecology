package sg.edu.smu.ecology.connector.bluetooth;

import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import sg.edu.smu.ecology.EcologyMessage;
import sg.edu.smu.ecology.Settings;

/**
 * Created by anurooppv on 25/10/2016.
 */

/**
 * This class helps to establish a client connection to any server device part of the ecology
 * using bluetooth
 */
public class BluetoothClientConnector extends BluetoothConnector {
    private static final String TAG = BluetoothClientConnector.class.getSimpleName();

    // To store the server connection thread
    private BluetoothSocketReadWriter clientToServerSocketReadWriter;

    private BluetoothClientConnectThread bluetoothClientConnectThread;
    // Contains the list of threads trying to establish a connection with all the paired devices
    private List<BluetoothClientConnectThread> clientConnectThreadsList = new ArrayList<>();
    private ClientConnectionListener clientConnectionListener = new ClientConnectionListener() {
        @Override
        public void clientConnectedToServer(BluetoothClientConnectThread
                                                    bluetoothClientConnectThread) {
            Log.i(TAG, "Client connected to server");
            // Set the client connect thread connected to the server
            setBluetoothClientConnectThread(bluetoothClientConnectThread);
            // Interrupt other client connect threads that are no longer required since one was
            // able to establish a connection
            if (clientConnectThreadsList.size() > 0) {
                interruptOtherClientConnectThreads();
            }
        }
    };

    /**
     * This sets up connection requests to all the paired devices
     */
    @Override
    public void setupBluetoothConnection() {
        // Among the paired devices, any device can be the server. So try connecting to each and
        // every paired device until a connection is established
        for (int i = 0; i < getPairedDevicesList().size(); i++) {
            BluetoothClientConnectThread bluetoothClientConnectThread = new
                    BluetoothClientConnectThread(getBluetoothAdapter(),
                    getPairedDevicesList().get(i), getUuidsList(), getHandler(),
                    clientConnectionListener);
            bluetoothClientConnectThread.start();
            // Save the connect threads list
            clientConnectThreadsList.add(bluetoothClientConnectThread);
        }
        Log.i(TAG, "clientConnectThreadsList size " + clientConnectThreadsList.size());
    }

    /**
     * Return the list of {@link BluetoothSocketReadWriter} threads
     *
     * @return the list of client server connection threads
     */
    @Override
    public Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList(
            Integer targetType, List<String> targets) {
        // Since the client can have only one connection(to server)
        return getBluetoothSocketReadWriterList();
    }

    /**
     * When the client device gets connected to a server
     *
     * @param msg the message received
     */
    @Override
    public void onDeviceConnected(Message msg) {
        Log.i(TAG, "Connected as a client to a device");
        Object object = msg.obj;

        clientToServerSocketReadWriter = (BluetoothSocketReadWriter) object;

        EcologyMessage message = new EcologyMessage(Arrays.<Object>asList(getDeviceId(),
                Settings.DEVICE_ID_EXCHANGE));
        message.setSource(getDeviceId());
        message.setTargetType(EcologyMessage.TARGET_TYPE_SERVER);
        // Send the client device Id to the server device
        sendConnectorMessage(message, getBluetoothSocketReadWriterList());

        getReceiver().onConnected();
    }

    private Collection<BluetoothSocketReadWriter> getBluetoothSocketReadWriterList() {
        return Collections.singletonList(clientToServerSocketReadWriter);
    }

    /**
     * When the server device gets disconnected
     *
     * @param msg the message received
     */
    @Override
    public void onDeviceDisconnected(Message msg) {
        handleServerDisconnection();
        getDeviceIdsList().clear();
        clientToServerSocketReadWriter = null;

        getReceiver().onDisconnected();
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
            case EcologyMessage.TARGET_TYPE_BROADCAST:
                super.onReceiverMessage(msg, messageData);
                break;

            case EcologyMessage.TARGET_TYPE_SPECIFIC:
                for (String target : messageData.getTargets()) {
                    if (target.equals(getDeviceId())) {
                        super.onReceiverMessage(msg, messageData);
                    }
                }
                break;

            default:
                break;
        }
    }

    /**
     * When a connector message is received
     *
     * @param msg         the message received
     * @param messageData the decoded data
     */
    @Override
    public void onConnectorMessage(Message msg, EcologyMessage messageData) {
    }

    @Override
    public void disconnect() {
        super.disconnect();
        handleDisconnection();
    }

    /**
     * When the bluetooth is turned off - disconnect the existing connection and reset connected
     * device ids
     */
    @Override
    public void onBluetoothOff() {
        handleDisconnection();
        getDeviceIdsList().clear();
        getReceiver().onDisconnected();
    }

    private void handleDisconnection(){
        if (bluetoothClientConnectThread != null && !bluetoothClientConnectThread.isInterrupted()) {
            bluetoothClientConnectThread.interrupt();
        }

        if (clientConnectThreadsList.size() > 0) {
            for (BluetoothClientConnectThread bluetoothClientConnectThread :
                    clientConnectThreadsList) {
                if (!bluetoothClientConnectThread.isInterrupted()) {
                    bluetoothClientConnectThread.interrupt();
                }
            }
        }
    }
    /**
     * Handle the server disconnection so that it starts looking for new server connection
     */
    private void handleServerDisconnection() {
        bluetoothClientConnectThread.handleServerDisconnection();
    }

    private void setBluetoothClientConnectThread(BluetoothClientConnectThread
                                                         bluetoothClientConnectThread) {
        this.bluetoothClientConnectThread = bluetoothClientConnectThread;
    }

    /**
     * Interrupt other client connect threads which tried to set up a connection with a server
     * device
     */
    private void interruptOtherClientConnectThreads() {
        Log.i(TAG, "interruptOtherClientConnectThreads");
        for (BluetoothClientConnectThread clientConnectThread : clientConnectThreadsList) {
            if (!clientConnectThread.equals(bluetoothClientConnectThread)) {
                if (!clientConnectThread.isInterrupted()) {
                    clientConnectThread.interrupt();
                }
            }
        }
        clientConnectThreadsList.clear();
    }

    /**
     * Interface to listen to client connections to a server
     */
    interface ClientConnectionListener {

        /**
         * This method is called when a client gets connected to the server
         *
         * @param bluetoothClientConnectThread the client connect thread that got connected
         */
        public void clientConnectedToServer(BluetoothClientConnectThread
                                                    bluetoothClientConnectThread);
    }
}
