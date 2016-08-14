package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by tnnguyen on 28/4/16.
 */
public class SocketConnectionStarter extends Thread {
    private static final String TAG = SocketConnectionStarter.class.getSimpleName();

    private Handler handler;
    private InetAddress address;
    private SocketReadWriter socketCreator;
    private boolean connectedToServer = false;

    public SocketConnectionStarter(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        Socket socket = new Socket();
        try {
            socket.setReuseAddress(true);
            socket.bind(null);
            while(!connectedToServer) {
                try {
                    Log.d(TAG, "connection attempt");
                    socket.connect(new InetSocketAddress(address.getHostAddress(), Settings.SERVER_PORT),
                            Settings.TIME_OUT);
                    socketCreator = new SocketReadWriter(socket, handler);
                    new Thread(socketCreator).start();
                    Log.i(TAG, "socketCreator " + socketCreator);
                    connectedToServer = true;
                } catch (ConnectException e) {
                    Log.i(TAG,"Error while connecting. " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                } catch (Exception e){
                    //Log.i(TAG,"Exception "+e.getMessage());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            try {
                Log.i(TAG, "socket close");
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
