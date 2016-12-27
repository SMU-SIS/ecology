package sg.edu.smu.ecology.connector.wifip2p;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Created by anurooppv on 14/7/2016.
 */
public class Peer {

    private final static String TAG = Peer.class.getSimpleName();

    private WifiP2pDevice device;
    private String name;
    private String registrationType;

    public WifiP2pDevice getDevice(){
        return device;
    }

    public void setDevice (WifiP2pDevice value){
        device = value;
    }

    public String getName(){
        return name;
    }

    public void setName(String value){
        name = value;
    }

    public String getRegistrationType(){
        return registrationType;
    }

    public void setRegistrationType(String value){
        registrationType = value;
    }
}
