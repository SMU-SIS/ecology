package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

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

    public SocketConnectionStarter(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        // To record the status of the connection
        boolean connectedToServer = false;

        // Try connecting till the connection is setup
        while (!connectedToServer) {
            try {
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.bind(null);

                Log.d(TAG, "connection attempt");

                socket.connect(new InetSocketAddress(address.getHostAddress(), Settings.SERVER_PORT),
                        Settings.TIME_OUT);
                socketCreator = new SocketReadWriter(socket, handler);
                new Thread(socketCreator).start();

                // When connected, set this to true
                connectedToServer = true;
            } catch (ConnectException e) {
                Log.i(TAG, "Error while connecting. " + e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
