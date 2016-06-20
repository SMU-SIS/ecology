package sg.edu.smu.ecology;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class Ecology implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener,WifiP2pManager.ConnectionInfoListener,
        Handler.Callback {

    private final String TAG = "ECOLOGY - MAGI LIB";
    private GoogleApiClient googleApiClient;
    private String nodeId = null;
    private EventReceiver eventReceiver;
    private EventBroadcaster eventBroadcaster;
    private Event event;
    private DependentEvent dependentEvent;
    private String[] eventType;
    private final IntentFilter intentFilter = new IntentFilter();
    private String android_id;
    private static final String CAPABILITY_NAME = "mobile_news_feed_controller";
    private Context activity;
    private boolean messageapi = false;
    private boolean wifiDirect = false;

    private Handler handler = new Handler(this);

    public Ecology(){
        filterIntent();
    }

    public void setEventReceiver(EventReceiver eventReceiver) {
        this.eventReceiver = eventReceiver;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Handler getHandler() {
        return handler;
    }

    public IntentFilter getIntentFilter(){
        wifiDirect= true;
        return intentFilter;
    }

    public void connectEcology(String ecologyName, Context activity){

        Settings.ECOLOGY_NAME = ecologyName;
        this.activity = activity;

        googleApiClient = new GoogleApiClient.Builder(activity)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();

        googleApiClient.connect();

        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    public void disconnectEcology(){

        getGoogleApiClient().disconnect();
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

    public void createApplicationRoom() {

        Log.i(TAG, "setupmsgdelivery");

        // Manually retrieving the results of reachable nodes with the message_wearable capability
        new Thread("CapCheck") {
            @Override
            public void run() {
                CapabilityApi.GetCapabilityResult result =
                        Wearable.CapabilityApi.getCapability(
                                googleApiClient, CAPABILITY_NAME,
                                CapabilityApi.FILTER_REACHABLE).await();

                try {
                    nodeId = updateMessageCapability(result.getCapability());
                    Log.i(TAG, "nodeId " + nodeId);
                } catch (IOException e) {
                    e.printStackTrace();
                }

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
        //nodeId = "7ffcaa18";

        eventBroadcaster.setGoogleApiClient(googleApiClient);
        eventBroadcaster.setNodeId(nodeId);

        if( nodeId != null){
            messageapi = true;
            android_id = android.provider.Settings.Secure.getString(activity.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            Log.i(TAG, "my android id " +android_id);

            dependentEvent = new DependentEvent();
            dependentEvent.setDeviceID(android_id);
            eventBroadcaster.setDependentEvent(dependentEvent);

            eventBroadcaster.setMessageapi(true);
        }

        if(messageapi && !wifiDirect) {
            Event event = new Event();
            event.setType("ecology:connected");
            eventReceiver.handleEvent(event);
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

    public GoogleApiClient getGoogleApiClient(){
        return googleApiClient;
    }

    public EventBroadcaster getEventBroadcaster(){
        eventBroadcaster = new EventBroadcaster(this);
        return eventBroadcaster;
    }

    public interface EventReceiver {

        void handleEvent(Event event);

    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "Connected");
        createApplicationRoom();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if(messageapi) {
            dependentEvent = (DependentEvent) SerializationUtils.deserialize(messageEvent.getData());
            event = dependentEvent.getEvent();
            String deviceID = dependentEvent.getDeviceID();

            Log.i(TAG, "received android id " + deviceID);
            Log.i(TAG, "Received "+event.getType());

            eventType = eventBroadcaster.getEventTypes();

            for (String anEventType : eventType) {

                if (anEventType != null && event.getType().equals(anEventType)) {

                    if (!(deviceID.equals(android_id))) {
                        eventReceiver.handleEvent(event);
                    }

                    if (wifiDirect)
                        eventBroadcaster.forward(dependentEvent, event, true);
                }
            }
        }
    }

    private void filterIntent()
    {
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    public void setEventBroadcaster(SocketCreator value) {
        eventBroadcaster.setSocketCreator(value);
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Log.d(Settings.TAG, "onConnectionInfoAvailable");
        Thread handler = null;
        if (p2pInfo.isGroupOwner) {
            Log.d(Settings.TAG, "Connected as group owner");
            try {
                handler = new OwnerSocketHandler(this.getHandler());
                handler.start();
            } catch (Exception e) {
                Log.d(Settings.TAG,
                        "Failed to create a server thread - " + e.getMessage());
            }
        } else {
            Log.d(Settings.TAG, "Connected as peer");
            handler = new MemberSocketHandler(
                    (this).getHandler(),p2pInfo.groupOwnerAddress);
            handler.start();
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Settings.MESSAGE_READ:
                Log.d(Settings.TAG, " MESSAGE_READ");
                byte[] readBuf = (byte[]) msg.obj;
                event = (Event)SerializationUtils.deserialize(readBuf);
                Log.i(Settings.TAG, " eventType" +event.getType());
                Log.i(Settings.TAG, " eventData" +event.getData());
                eventType = eventBroadcaster.getEventTypes();

                for (String anEventType : eventType) {

                    if (anEventType != null && event.getType().equals(anEventType)) {
                        eventReceiver.handleEvent(event);

                        if(messageapi) {
                            dependentEvent.setEvent(event);
                            dependentEvent.setDeviceID(android_id);
                            eventBroadcaster.forward(dependentEvent, event, false);
                        }
                    }
                }
                break;
            case Settings.MY_HANDLE:
                Log.d(Settings.TAG, " MY HANDLE");
                Object obj = msg.obj;
                setEventBroadcaster((SocketCreator) obj);

                if(wifiDirect) {
                    Event event = new Event();
                    event.setType("ecology:connected");
                    eventReceiver.handleEvent(event);
                }
        }
        return true;
    }

}
