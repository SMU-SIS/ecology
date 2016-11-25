package sg.edu.smu.ecology;

/**
 * Created by tnnguyen on 28/4/16.
 */
class Settings {
    static final int MESSAGE_READ = 0x400 + 1;
    static final int MY_HANDLE = 0x400 + 2;
    static final int SOCKET_CLOSE = 0x400 + 3;
    static final int SOCKET_CLIENT = 0x400 + 4;
    static final int SOCKET_SERVER = 0x400 + 5;
    static final int TIME_OUT = 5000;
    static int SERVER_PORT = 6868;
    static final String DEVICE_DISCONNECTED = "device:disconnected";
    static final String DEVICE_CONNECTED = "device:connected";
}
