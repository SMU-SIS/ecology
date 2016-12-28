package sg.edu.smu.ecology.connector.wifip2p;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * A BroadcastReceiver that notifies of important wifi p2p events.
 */
class Wifip2pBroadcastManager extends BroadcastReceiver {

    private static final String TAG = Wifip2pBroadcastManager.class.getSimpleName();

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Wifip2pConnector wifip2pConnector;
    private Boolean wifip2pConnected = false;

    /**
     * @param manager WifiP2pManager system service
     * @param channel Wifi p2p channel
     * @param wifip2pConnector the connector associated with the receiver
     */
    public Wifip2pBroadcastManager(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                   Wifip2pConnector wifip2pConnector) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.wifip2pConnector = wifip2pConnector;
    }

    /**
     * This method is called when the BroadcastReceiver is receiving an Intent broadcast related to
     * wifi p2p events
     *
     * @param context the Context in which the receiver is running.
     * @param intent  the Intent being received.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Action " + action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            Log.i(TAG, "state " + state);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "Wifi status is enabled");
            } else {
                Log.d(TAG, "Wifi status is disabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Not required currently as we don't populate the peer list
            /*if (manager != null) {
                Log.i(TAG, "requestpeers");
                manager.requestPeers(channel, (WifiP2pManager.PeerListListener) activity.
                getFragmentManager().findFragmentByTag("peerList"));
            }*/
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                Log.i(TAG, "networkinfo " + networkInfo);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "Connected to peer network.");
                    wifip2pConnected = true;
                    manager.requestConnectionInfo(channel, wifip2pConnector);
                } else {
                    if (wifip2pConnected) {
                        Log.d(TAG, "WiFiP2P connection is lost");
                        wifip2pConnector.onWifiP2pConnectionDisconnected();
                        wifip2pConnected = false;
                    }
                }
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {
            WifiP2pDevice device = (WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "Device status -" + device.status);
        }
    }
}
