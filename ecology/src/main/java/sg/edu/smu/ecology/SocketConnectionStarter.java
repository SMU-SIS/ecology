package sg.edu.smu.ecology;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by tnnguyen on 28/4/16.
 */
public class SocketConnectionStarter extends Thread {
    private static final String TAG = SocketConnectionStarter.class.getSimpleName();

    private Handler handler;
    private InetAddress address;
    private SocketReadWriter socketReadWriter;
    private Socket socket = null;

    public SocketConnectionStarter(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    @Override
    public void run() {
        // To record the status of the connection
        boolean connectedToServer = false;

        // Try connecting till the connection is setup
        while (!connectedToServer && !isInterrupted()) {
            try {
                socket = new Socket();
                socket.setReuseAddress(true);
                socket.bind(null);

                Log.d(TAG, "connection attempt");

                socket.connect(new InetSocketAddress(address.getHostAddress(), Settings.SERVER_PORT),
                        Settings.TIME_OUT);
                socketReadWriter = new SocketReadWriter(socket, handler);
                new Thread(socketReadWriter).start();

                // When connected, set this to true
                connectedToServer = true;
            } catch (ConnectException e) {
                Log.i(TAG, e.getMessage());
                Log.d(TAG, "Waiting for 1sec before new connection attempt");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // restore interrupted status
                    interrupt();
                }
            } catch (SocketTimeoutException e) {
                Log.i(TAG, "Connection: " + e.getMessage() + ".");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    // restore interrupted status
                    interrupt();
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
            }
        }
        Log.d(TAG, "done");
    }
}
