package sg.edu.smu.ecology;

import org.apache.mina.core.buffer.IoBuffer;

import java.math.BigInteger;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by anurooppv on 29/6/2016.
 */
public class DataEncoder {

    private static CharsetEncoder cse = Charset.forName("UTF-8").newEncoder();

    public DataEncoder() {
    }

    //Write datamessage into iobuffer.
    public void encodeMessage(MessageData messageData, IoBuffer buffer)
            throws CharacterCodingException {
        String addr = messageData.getAddress();

        write(addr, buffer);

        ArrayList<Object> args = messageData.getArguments();

        String typeTags = messageData.getTypeTags();

        write(typeTags, buffer);

        for (Iterator<Object> i = args.iterator(); i.hasNext();) {
            Object arg = (Object) i.next();
            write(arg, buffer);
        }
    }

    private void write(Object arg, IoBuffer buffer)
            throws CharacterCodingException {
        if (arg == null) {
            return;
        }

        if (arg instanceof byte[]) {
            byte[] bytes = (byte[]) arg;
            buffer.put(bytes);
            padBuffer(bytes.length, buffer);
            return;
        }

        if (arg instanceof Object[]) {
            Object[] theArray = (Object[]) arg;
            for (int i = 0; i < theArray.length; ++i) {
                write(theArray[i], buffer);
            }
            return;
        }

        if (arg instanceof Float) {
            buffer.putFloat((Float) arg);
            return;
        }

        if (arg instanceof String) {
            String str = (String) arg;
            write(str, buffer);
            return;
        }

        if (arg instanceof Integer) {
            buffer.putInt((Integer) arg);
            return;
        }

        if (arg instanceof Double) {
            buffer.putDouble((Double) arg);
            return;
        }

        if (arg instanceof BigInteger) {
            buffer.putLong(((BigInteger) arg).longValue());
            return;
        }
    }

    private void write(String s, IoBuffer buffer)
            throws CharacterCodingException {
        buffer.putString(s, cse);
        buffer.put((byte) 0);
        padBuffer(s.length() + 1, buffer);
    }

    private void padBuffer(int itemLength, IoBuffer buffer) {
        int mod = itemLength % 4;
        if (mod > 0) {
            byte[] padding = new byte[4 - mod];
            buffer.put(padding);
        }
    }


}
