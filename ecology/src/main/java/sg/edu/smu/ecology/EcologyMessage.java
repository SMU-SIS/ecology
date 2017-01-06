package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anurooppv on 5/1/2017.
 */

/**
 * This class is used to save the message data to sent to other connected devices in the ecology
 */
public class EcologyMessage {
    private static final String TAG = EcologyMessage.class.getSimpleName();
    /**
     * The content of the message
     */
    private List<Object> arguments = new ArrayList<>();

    public EcologyMessage(List<Object> data) {
        addArguments(data);
    }

    /**
     * Add a data to the message data
     *
     * @param data the data to be added
     */
    void addArgument(Object data) {
        arguments.add(data);
    }

    /**
     * Add a list of data to the message data
     *
     * @param messageData the list of data to be added
     */
    void addArguments(List<Object> messageData) {
        arguments.addAll(messageData);
    }

    /**
     * Fetch the last value in the message data
     *
     * @return the last value in the message data
     */
    Object fetchArgument() {
        return arguments.remove(arguments.size() - 1);
    }

    /**
     * Get the whole message data content
     *
     * @return the message data
     */
    public List<Object> getArguments() {
        return arguments;
    }
}
