package sg.edu.smu.ecology;

import android.app.ListFragment;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anurooppv on 14/7/2016.
 */
public class PeerList extends ListFragment implements WifiP2pManager.PeerListListener {

    private final static String TAG = PeerList.class.getSimpleName();

    WiFiDevicesAdapter listAdapter = null;
    private List<Peer> devices = new ArrayList<Peer>();

    public interface MemberClickListener {
        public void setupRoom(Peer member);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.peer_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listAdapter = new WiFiDevicesAdapter(this.getActivity(),
                android.R.layout.simple_list_item_2, android.R.id.text1,new ArrayList<Peer>());
        setListAdapter(listAdapter);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        devices.clear();
        for(WifiP2pDevice peer : peers.getDeviceList())
        {
            Peer member = new Peer();
            member.setDevice(peer);
            member.setName(peer.deviceName);
            member.setRegistrationType(peer.primaryDeviceType);
            devices.add(member);
        }
        ((WiFiDevicesAdapter) getListAdapter()).notifyDataSetChanged();

        if (devices.size() == 0) {
            Log.d(TAG, "No devices found");
            return;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ((MemberClickListener) getActivity()).setupRoom((Peer) l.getItemAtPosition(position));
        ((TextView) v.findViewById(android.R.id.text2)).setText("Connecting");
    }

    /**
     * Array adapter for list fragment that maintains Member list.
     */
    public class WiFiDevicesAdapter extends ArrayAdapter<Peer> {
        private List<Peer> items;
        public WiFiDevicesAdapter(Context context, int resource,
                                  int textViewResourceId, List<Peer> items) {
            super(context, resource, textViewResourceId, items);
            this.items = items;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(android.R.layout.simple_list_item_2, null);
            }
            Peer service = items.get(position);
            if (service != null) {
                TextView nameText = (TextView) v
                        .findViewById(android.R.id.text1);
                if (nameText != null) {
                    nameText.setText(service.getDevice().deviceName + " - " + service.getName());
                }
                TextView statusText = (TextView) v
                        .findViewById(android.R.id.text2);
                statusText.setText(getMemberStatus(service.getDevice().status));
            }
            return v;
        }
    }

    /*
        Returns current status of member
     */
    public static String getMemberStatus(int statusCode) {
        switch (statusCode) {
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}
