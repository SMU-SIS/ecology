package sg.edu.smu.ecology;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.apache.mina.core.buffer.IoBuffer;

import java.net.InetAddress;
import java.nio.charset.CharacterCodingException;
import java.util.Arrays;
import java.util.List;

import sg.edu.smu.ecology.encoding.DataDecoder;
import sg.edu.smu.ecology.encoding.DataEncoder;
import sg.edu.smu.ecology.encoding.MessageData;

/**
 * Created by anurooppv on 22/7/2016.
 */
public class Wifip2pConnector implements Connector, WifiP2pManager.ConnectionInfoListener, Handler.Callback {

    private final static String TAG = Wifip2pConnector.class.getSimpleName();

    // To listen to certain events of wifi direct
    private final IntentFilter intentFilter = new IntentFilter();
    private SocketReadWriter socketReadWriter;
    private Handler handler = new Handler(this);
    private Connector.Receiver receiver;
    private BroadcastManager broadcastManager = null;
    private SocketConnectionStarter socketConnectionStarter;

    // Buffer size to be allocated to the IoBuffer - message byte array size is different from this
    private static final int BUFFER_SIZE = 1024;

    // Registers if the connector is connected.
    private Boolean onConnectorConnected = false;

    /**
     * Used to save the application context
     */
    private Context applicationContext;

    // The thread responsible for initializing the connection.
    private Thread connectionStarter = null;

    // To store the group owner's address
    private InetAddress groupOwnerAddress;

    public Wifip2pConnector() {
        filterIntent();
    }

    private void filterIntent() {
        // add the intents that the broadcast receiver should check for
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    @Override
    public void sendMessage(List<Object> message) {
        IoBuffer ioBuffer = IoBuffer.allocate(BUFFER_SIZE);

        if (socketReadWriter != null) {
            DataEncoder dataEncoder = new DataEncoder();
            MessageData messageData = new MessageData();

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
            byte[] eventData = ioBuffer.array();

            // Actual message data is retrieved
            byte[] eventDataToSend = Arrays.copyOfRange(eventData, 0, length);

            // Write length of the data first
            socketReadWriter.writeInt(length);

            // Write the byte data
            socketReadWriter.writeData(eventDataToSend);
            ioBuffer.clear();
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
    public void connect(Context context) {
        applicationContext = context;

        // To register to the WiFiP2P framework
        WifiP2pManager manager = (WifiP2pManager) applicationContext.getSystemService(Context.WIFI_P2P_SERVICE);
        WifiP2pManager.Channel channel = manager.initialize(applicationContext, applicationContext.getMainLooper(), null);

        // To notify about various events occurring with respect to the WiFiP2P connection
        broadcastManager = new BroadcastManager(manager, channel, this);
        // Register the broadcast receiver with the intent values to be matched
        applicationContext.registerReceiver(broadcastManager, intentFilter);
    }

    /**
     * Disconnect from the ecology.
     */
    @Override
    public void disconnect() {
        onConnectorConnected = false;

        applicationContext.unregisterReceiver(broadcastManager);

        if (connectionStarter != null && connectionStarter.isAlive() && !connectionStarter.isInterrupted()) {
            connectionStarter.interrupt();
        }
    }

    // Called when the wifip2p connection is lost.  
    void onWifiP2pConnectionDisconnected() {
        receiver.onConnectorDisconnected();
    }

    @Override
    public boolean isConnected() {
        return onConnectorConnected;
    }

    // The requested connection info is available
    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable");
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as server");
            try {
                connectionStarter = new ServerSocketConnectionStarter(this.getHandler());
                connectionStarter.start();
            } catch (Exception e) {
                Log.d(TAG, "Failed to create a server thread - " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Connected as peer");
            groupOwnerAddress = p2pInfo.groupOwnerAddress;

            socketConnectionStarter = new SocketConnectionStarter(this.getHandler(), groupOwnerAddress);

            connectionStarter = socketConnectionStarter;
            connectionStarter.start();
        }
    }

    private Handler getHandler() {
        return handler;
    }

    // Get Wi-Fi P2p configuration for setting up a connection
    public WifiP2pConfig getWifiConfig(Peer peer) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = peer.getDevice().deviceAddress;
        config.wps.setup = WpsInfo.PBC;
        return config;
    }

    // Scan for the available peers around
    public void scanPeers(WifiP2pManager manager, WifiP2pManager.Channel channel) {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "discover peers success");
            }

            @Override
            public void onFailure(int reason) {

            }
        });
    }

    // Handle message received from wifi p2p
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Settings.MESSAGE_READ:
                Log.d(TAG, " MESSAGE_READ");
                byte[] readBuf = (byte[]) msg.obj;

                DataDecoder dataDecoder = new DataDecoder();

                MessageData messageData = dataDecoder.convertMessage(readBuf, readBuf.length);

                List<Object> data;
                data = messageData.getArguments();
                Log.i(TAG, "data " + data);
                String eventTypeReceived = null;

                try {
                    eventTypeReceived = (String) data.get(data.size() - 2);
                } catch (ArrayIndexOutOfBoundsException e) {
                    e.printStackTrace();
                }

                Log.i(TAG, " eventType " + eventTypeReceived);

                receiver.onMessage(data);
                break;

            case Settings.MY_HANDLE:
                Log.d(TAG, " MY HANDLE");
                Object obj = msg.obj;
                setSocketReadWriter((SocketReadWriter) obj);

                onConnectorConnected = true;

                receiver.onConnectorConnected();
                break;

            case Settings.SOCKET_CLOSE:
                Log.d(TAG, "Socket Close");
                onConnectorConnected = false;

                receiver.onConnectorDisconnected();

                // For only client - start looking for server connections
                if (groupOwnerAddress != null) {
                    socketConnectionStarter.setConnectedToServer(false);
                }
                break;
        }
        return true;
    }

    private void setSocketReadWriter(SocketReadWriter socketReadWriter) {
        this.socketReadWriter = socketReadWriter;
    }
}
