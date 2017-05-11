package sg.edu.smu.ecology;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anurooppv on 5/1/2017.
 */

/**
 * This class is used to save the message data to be sent to other connected devices in the ecology
 */
public class EcologyMessage {
    private static final String TAG = EcologyMessage.class.getSimpleName();
    /**
     * The values that target type can have
     */
    public static final int TARGET_TYPE_SERVER = 0;
    public static final int TARGET_TYPE_SPECIFIC = 1;
    public static final int TARGET_TYPE_BROADCAST = 2;
    /**
     * The content of the message
     */
    private List<Object> arguments = new ArrayList<>();

    /**
     * The source device id
     */
    private String source;

    /**
     * The list of target devices
     */
    private List<String> targets = new ArrayList<>();

    /**
     * To indicate the target type
     */
    private Integer targetType;

    public EcologyMessage(List<Object> data) {
        addArguments(data);
    }

    /**
     * Copy constructor
     */
    public EcologyMessage(EcologyMessage ecologyMessage) {
        this(ecologyMessage.getArguments());
    }

    /**
     * Add a data to the message data
     *
     * @param data the data to be added
     */
    public void addArgument(Object data) {
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
    public Object fetchArgument() {
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

    /**
     * Get the source(device id) of the message
     *
     * @return the id of the source device of the message
     */
    public String getSource() {
        if (source != null) {
            return source;
        } else {
            return null;
        }
    }

    /**
     * Set the source of the message
     *
     * @param source the id of the source device
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Get the list of targets for the message
     *
     * @return the list of target devices
     */
    public List<String> getTargets() {
        return targets;
    }

    /**
     * Set the target devices the message needs to be sent
     *
     * @param targets the list of target devices
     */
    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    /**
     * Get the target type of the message
     *
     * @return the target type of the message
     */
    public Integer getTargetType() {
        if (targetType != null)
            return targetType;
        else {
            return null;
        }
    }

    /**
     * Set the target type for the message
     *
     * @param targetType the target type for this message
     */
    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }
}
