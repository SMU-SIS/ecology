package sg.edu.smu.ecology.connector.wifip2p;

import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by tnnguyen on 28/4/16.
 */

/**
 * This thread runs when a server device tries to establish a connection with a client device
 */
class ServerSocketConnectionStarter extends Thread {
    private static final String TAG = ServerSocketConnectionStarter.class.getSimpleName();
    private final int THREAD_COUNT = 10;
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(
            THREAD_COUNT, THREAD_COUNT, 10, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>());
    // It waits for incoming connections from client devices
    private ServerSocket socket = null;
    // To handle the messages
    private Handler handler;
    // To manage an established connection
    private SocketReadWriter socketReadWriter;

    ServerSocketConnectionStarter(Handler handler) throws IOException {
        try {
            socket = new ServerSocket(Wifip2pConnector.SERVER_PORT);
            this.handler = handler;
        } catch (IOException e) {
            e.printStackTrace();
            pool.shutdownNow();
            throw e;
        }
    }

    /**
     * When a disconnection occurs
     */
    @Override
    public void interrupt() {
        super.interrupt();
        if (socketReadWriter != null) {
            socketReadWriter.onInterrupt();
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ioe) {
        }
    }

    /**
     * Keep listening to incoming connections requests from clients till a disconnection request is
     * received
     */
    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                Log.d(TAG, "connection attempt");
                socketReadWriter = new SocketReadWriter(socket.accept(), handler);
                pool.execute(socketReadWriter);
            } catch (IOException e) {
                try {
                    if (socket != null && !socket.isClosed())
                        socket.close();
                } catch (IOException ioe) {
                }
                e.printStackTrace();
                pool.shutdownNow();
                break;
            }
        }
        Log.d(TAG, "done");
    }
}
