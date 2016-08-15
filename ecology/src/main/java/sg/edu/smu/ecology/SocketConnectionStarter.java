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
    private Socket socket = null;

    public SocketConnectionStarter(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        // To record the status of the connection
        boolean connectedToServer = false;
        int maxNumberOfRconnections = 20;
        int currentNumberOfReconnections = 0;

        // Try connecting till the connection is setup
        while (!connectedToServer) {
            try {
                socket = new Socket();
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
                currentNumberOfReconnections++;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

                // Stop reconnecting when the number of attempts reaches max number of reconnections
                if(currentNumberOfReconnections == maxNumberOfRconnections){
                    currentNumberOfReconnections = 0;
                    Log.i(TAG, "Reconnections stopped");
                    connectedToServer = true;
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    if (socket != null && !socket.isClosed()) {
                        Log.i(TAG, "Socket close ");
                        socket.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
