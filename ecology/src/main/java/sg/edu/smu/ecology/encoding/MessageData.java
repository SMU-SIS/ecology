package sg.edu.smu.ecology.encoding;

import java.math.BigInteger;
import java.util.ArrayList;

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
    protected String typeTags;
    private int datasize = 0;

    public MessageData() {
        arguments = new ArrayList<Object>();
        typeTags = ",";
    }

    public String getTypeTags() {
        return typeTags;
    }

    public void addArgument(Object argument) {
        if (argument instanceof String) {
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
        } else {
            throw new UnsupportedDataTypeException("Unsupported argument type: " + argument);
        }
    }

    public void addArgument(Object[] array, String objectTypes) {
        typeTags += '[' + objectTypes + ']';
        arguments.add(array);
        // TODO : Compute datasize for array contents
    }

    public ArrayList<Object> getArguments() {
        return arguments;
    }

    public void addArgument(String param) {
        typeTags += 's';
        arguments.add(param);
        int stringSize = getStringSize(param);
        datasize += stringSize;
    }

    public void addArgument(Float f) {
        typeTags += 'f';
        arguments.add(f);
        datasize += 4;
    }

    public void addArgument(Double d) {
        typeTags += 'd';
        arguments.add(d);
        datasize += 8;
    }

    public void addArgument(Integer i) {
        typeTags += 'i';
        arguments.add(i);
        datasize += 4;
    }

    public void addArgument(BigInteger b) {
        typeTags += 'h';
        arguments.add(b);
        datasize += 8;
    }

    public void addArgument(Character c) {
        if(c < (char) 128){
            // ASCII characters are encoded as an integer.
            typeTags += 'c';
            arguments.add(c);
            datasize += 4;
        } else {
            // Unicode characters are encoded as a string.
            typeTags += 'C';
            arguments.add(c);
            datasize += getStringSize(c.toString());
        }
    }

    public void addArgument(Boolean b) {
        typeTags += b ? 'T' : 'F';
        arguments.add(b);
    }

    public void addArgument(byte[] bytes) {
        typeTags += 'b';
        arguments.add(bytes);
        int dataLength = bytes.length;
        // An int32 size count,
        datasize += 4;
        // followed by that many 8-bit bytes of arbitrary binary data,
        datasize += dataLength;
        // followed by 0-3 additional zero bytes to make the total number of
        // bits a multiple of 32.
        int mod = (dataLength % 4);
        datasize += (mod > 0) ? 4 - mod : 0;
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

    public void clearArguments(){
        arguments.clear();
    }


}
