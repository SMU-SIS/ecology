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

import org.apache.mina.core.buffer.IoBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by anurooppv on 1/6/2016.
 */
public class Ecology implements GoogleApiClient.ConnectionCallbacks, MessageApi.MessageListener,WifiP2pManager.ConnectionInfoListener,
        Handler.Callback {

    private final static String TAG = Ecology.class.getSimpleName();

    private GoogleApiClient googleApiClient;
    private String nodeId = null;
    private EventReceiver eventReceiver;
    private EventBroadcaster eventBroadcaster;
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

    public Handler getHandler() {
        return handler;
    }

    public IntentFilter getIntentFilter(){
        wifiDirect= true;
        return intentFilter;
    }

    public String getAndroid_id() {
        return android_id;
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

        eventBroadcaster.setGoogleApiClient(googleApiClient);
        eventBroadcaster.setNodeId(nodeId);

        if( nodeId != null){
            messageapi = true;
            android_id = android.provider.Settings.Secure.getString(activity.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
            Log.i(TAG, "my android id " + android_id);

            eventBroadcaster.setMessageapi(true);
        }

        if(messageapi && !wifiDirect) {
            Vector<Object> launchData = new Vector<>();
            eventReceiver.handleEvent("ecology:connected", launchData);
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

        void handleEvent(String eventType, Collection<Object> args);

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
    // Handle message received from the message API.
    public void onMessageReceived(MessageEvent messageEvent) {

        if(messageapi) {

            DataDecoder dataDecoder = new DataDecoder();
            Log.i(TAG, "messagedata " + Arrays.toString(messageEvent.getData()));
            DataMessage dataMessage = dataDecoder.convertMessage(messageEvent.getData(), messageEvent.getData().length);

            List<Object> data;
            data = dataMessage.getArguments();
            Log.i(TAG, "Data "+data);

            String deviceID = (String) data.get(data.size() - 1);
            String eventTypeReceived = (String) data.get(data.size() - 2);

            Log.i(TAG, "received android id " + deviceID);
            Log.i(TAG, "Received "+eventTypeReceived);

            eventType = eventBroadcaster.getEventTypes();

            for (String anEventType : eventType) {

                if (anEventType != null && eventTypeReceived.equals(anEventType)) {

                    if (!(deviceID.equals(android_id))) {
                        eventReceiver.handleEvent(eventTypeReceived, data);
                    }

                    if (wifiDirect) {
                        eventBroadcaster.forward(data, true);
                    }
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
        Log.d(TAG, "onConnectionInfoAvailable");
        Thread handler = null;
        if (p2pInfo.isGroupOwner) {
            Log.d(TAG, "Connected as group owner");
            try {
                handler = new OwnerSocketHandler(this.getHandler());
                handler.start();
            } catch (Exception e) {
                Log.d(TAG,
                        "Failed to create a server thread - " + e.getMessage());
            }
        } else {
            Log.d(TAG, "Connected as peer");
            handler = new MemberSocketHandler(
                    (this).getHandler(),p2pInfo.groupOwnerAddress);
            handler.start();
        }
    }

    @Override
    // Handle message received from wifi direct.
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Settings.MESSAGE_READ:
                Log.d(TAG, " MESSAGE_READ");
                byte[] readBuf = (byte[]) msg.obj;

                DataDecoder dataDecoder = new DataDecoder();

                DataMessage dataMessage = dataDecoder.convertMessage(readBuf, readBuf.length);

                List<Object> data;
                data = dataMessage.getArguments();

                String eventTypeReceived = (String) data.get(data.size() - 1);
                Log.i(TAG, " eventType " + eventTypeReceived);

                eventType = eventBroadcaster.getEventTypes();

                for (String anEventType : eventType) {

                    if (anEventType != null && eventTypeReceived.equals(anEventType)) {
                        eventReceiver.handleEvent(eventTypeReceived, data);
                        
                        if(messageapi) {
                            data.add(android_id);
                            Log.i(TAG, "data "+data);
                            eventBroadcaster.forward(data, false);
                        }
                    }
                }
                break;
            case Settings.MY_HANDLE:
                Log.d(TAG, " MY HANDLE");
                Object obj = msg.obj;
                setEventBroadcaster((SocketCreator) obj);

                if(wifiDirect) {
                    Vector<Object> launchData = new Vector<>();
                    eventReceiver.handleEvent("ecology:connected", launchData);
                }
        }
        return true;
    }

}
