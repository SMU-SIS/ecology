package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;

import java.math.BigInteger;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;

/**
 * Created by anurooppv on 29/6/2016.
 */
public class DataEncoder {

    private static CharsetEncoder cse = Charset.forName("UTF-8").newEncoder();

    //Write datamessage into iobuffer.
    public void encodeMessage(MessageData messageData, IoBuffer buffer)
            throws CharacterCodingException {

        ArrayList<Object> args = messageData.getArguments();

        String typeTags = messageData.getTypeTags();

        write(typeTags, buffer);

        for (Object arg : args) {
            write(arg, buffer);
        }
    }

    /**
     * Write the arguments as bytes into the buffer
     *
     * @param arg    the argument
     * @param buffer the IOBuffer to store the bytes
     * @throws CharacterCodingException
     */
    private void write(Object arg, IoBuffer buffer)
            throws CharacterCodingException {
        if (arg == null) {
            return;
        }

        if (arg instanceof byte[]) {
            byte[] bytes = (byte[]) arg;
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            padBuffer(bytes.length, buffer);
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

        if (arg instanceof Character) {
            Character c = (Character) arg;
            if(c < (char) 128){
                // ASCII characters are encoded as an integer.
                buffer.putInt(c);
            } else {
                // Unicode characters are encoded as a string.
                write(c.toString(), buffer);
            }
            return;
        }
    }

    /**
     * Write the string data bytes into the buffer
     *
     * @param s      the string to be encoded
     * @param buffer the IOBuffer to store the bytes
     * @throws CharacterCodingException
     */
    private void write(String s, IoBuffer buffer)
            throws CharacterCodingException {
        int initialBufferLength = buffer.position();
        buffer.putString(s, cse);
        buffer.put((byte) 0);
        padBuffer((buffer.position() - initialBufferLength), buffer);
    }

    /**
     * Add required paddings of 0 in the buffer
     *
     * @param bytesLength bytes length of the string
     * @param buffer      the IOBuffer to store the bytes
     */
    private void padBuffer(int bytesLength, IoBuffer buffer) {
        int mod = bytesLength % 4;
        if (mod > 0) {
            byte[] padding = new byte[4 - mod];
            buffer.put(padding);
        }
    }

}
