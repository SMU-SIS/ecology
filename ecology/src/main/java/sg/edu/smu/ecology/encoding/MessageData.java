package sg.edu.smu.ecology.encoding;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by anurooppv on 29/6/2016.
 */
public class MessageData {

    public static class UnsupportedDataTypeException extends RuntimeException {
        public UnsupportedDataTypeException(String s) {
            super(s);
        }
    };

    protected ArrayList<Object> arguments;
    protected String typeTags = "";
    private int dataSize = 0;

    public MessageData() {
        arguments = new ArrayList<>();
    }

    public String getTypeTags() {
        return typeTags;
    }

    public int getByteSize() {
        return dataSize + getStringSize(getTypeTags());
    }

    public void addArgument(Object argument) {
        if(argument == null) {
            addNullArgument();
        } else if (argument instanceof String) {
            addArgument((String) argument);
        } else if (argument instanceof Float) {
            addArgument((Float) argument);
        } else if (argument instanceof Integer) {
            addArgument((Integer) argument);
        } else if (argument instanceof Double) {
            addArgument((Double) argument);
        }else if (argument instanceof BigInteger) {
            addArgument((BigInteger) argument);
        } else if (argument instanceof byte[]) {
            addArgument((byte[]) argument);
        } else if (argument instanceof Boolean) {
            addArgument((Boolean) argument);
        } else if (argument instanceof Character) {
            addArgument((Character) argument);
        } else if (argument instanceof  List) {
            addArgument((List) argument);
        } else if (argument instanceof Map) {
            addArgument((Map) argument);
        } else {
            throw new UnsupportedDataTypeException(
                    "Invalid or not yet supported type: " + argument.getClass().getCanonicalName()
            );
        }
    }

    public void addNullArgument(){
        typeTags += 'N';
    }

    public void addArgument(List list) {
        typeTags += '[';
        for(Object arg: list){
            addArgument(arg);
        }
        typeTags += ']';
    }

    public void addArgument(Map<Object, Object> map) {
        typeTags += '{';
        // Add the couples key / values.
        for(Map.Entry<Object, Object> entry: map.entrySet()){
            addArgument(entry.getKey());
            addArgument(entry.getValue());
        }
        typeTags += '}';
    }

    public ArrayList<Object> getArguments() {
        return arguments;
    }

    public void addArgument(String param) {
        typeTags += 's';
        arguments.add(param);
        int stringSize = getStringSize(param);
        dataSize += stringSize;
    }

    public void addArgument(Float f) {
        typeTags += 'f';
        arguments.add(f);
        dataSize += 4;
    }

    public void addArgument(Double d) {
        typeTags += 'd';
        arguments.add(d);
        dataSize += 8;
    }

    public void addArgument(Integer i) {
        typeTags += 'i';
        arguments.add(i);
        dataSize += 4;
    }

    public void addArgument(BigInteger b) {
        typeTags += 'h';
        arguments.add(b);
        dataSize += 8;
    }

    public void addArgument(Character c) {
        if(c < (char) 128){
            // ASCII characters are encoded as an integer.
            typeTags += 'c';
            arguments.add(c);
            dataSize += 4;
        } else {
            // Unicode characters are encoded as a string.
            typeTags += 'C';
            arguments.add(c);
            dataSize += getStringSize(c.toString());
        }
    }

    public void addArgument(Boolean b) {
        typeTags += b ? 'T' : 'F';
    }

    public void addArgument(byte[] bytes) {
        typeTags += 'b';
        arguments.add(bytes);
        int dataLength = bytes.length;
        // An int32 size count,
        dataSize += 4;
        // followed by that many 8-bit bytes of arbitrary binary data,
        dataSize += dataLength;
        // followed by 0-3 additional zero bytes to make the total number of
        // bits a multiple of 32.
        int mod = (dataLength % 4);
        dataSize += (mod > 0) ? 4 - mod : 0;
    }

    private int getStringSize(String str) {
        // Add 1 because a string must be zero terminated
        int len = str.length() + 1;
        // logger.debug("GetStringSize : " + str + " : " + len);
        int mod = len % 4;
        // logger.debug("MOD " + mod);
        int pad = (mod > 0) ? 4 - mod : 0;
        // logger.debug("PAD " + pad);
        return len + pad;
    }
}
