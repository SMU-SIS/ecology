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
    // To record the status of the connection
    private boolean connectedToServer = false;

    public SocketConnectionStarter(Handler handler, InetAddress groupOwnerAddress) {
        this.handler = handler;
        this.address = groupOwnerAddress;
    }

    public void setConnectedToServer(boolean connectedToServer) {
        this.connectedToServer = connectedToServer;
    }

    @Override
    public void interrupt() {
        super.interrupt();
        if (socketReadWriter != null) {
            socketReadWriter.onInterrupt();
        }
    }

    @Override
    public void run() {
        // Try connecting till the connection is setup
        while (!isInterrupted()) {
            try {
                // If not connected to server, try to connect
                if (!connectedToServer) {
                    Log.d(TAG, "connection attempt");

                    socket = new Socket();
                    socket.setReuseAddress(true);
                    socket.bind(null);

                    socket.connect(new InetSocketAddress(address.getHostAddress(), Settings.SERVER_PORT),
                            Settings.TIME_OUT);
                    socketReadWriter = new SocketReadWriter(socket, handler);
                    new Thread(socketReadWriter).start();

                    // When connected, set this to true
                    connectedToServer = true;
                }
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
