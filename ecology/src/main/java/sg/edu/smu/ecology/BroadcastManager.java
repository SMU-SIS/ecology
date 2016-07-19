package sg.edu.smu.ecology;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

/**
 * Broadcast receiver that receives intent broadcast
 */
public class BroadcastManager extends BroadcastReceiver {

    private static final String TAG = BroadcastManager.class.getSimpleName();


    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private Ecology ecology;
    private Activity activity;

    public BroadcastManager(WifiP2pManager manager, WifiP2pManager.Channel channel,
                            Ecology ecology, Activity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.ecology = ecology;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "Wifi status is enabled");
            } else {
                Log.d(TAG, "Wifi status is disabled");
            }
        }
        else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                Log.i(TAG, "requestpeers");
                manager.requestPeers(channel, (WifiP2pManager.PeerListListener) activity.getFragmentManager().
                        findFragmentByTag("peerList"));
            }
        }
        else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                NetworkInfo networkInfo = (NetworkInfo) intent
                        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    Log.d(TAG, "Connected to peer network.");
                    manager.requestConnectionInfo(channel, ecology);
                }
            }
        }
        else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION
                .equals(action)) {
            WifiP2pDevice device = (WifiP2pDevice) intent
                    .getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "Device status -" + device.status);
        }
    }

}
