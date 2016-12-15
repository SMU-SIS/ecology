package sg.edu.smu.ecology;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anurooppv on 29/6/2016.
 */
public class DataDecoder {

    private static final String NO_ARGUMENT_TYPES = "";

    private static class Input {

        private final byte[] bytes;
        private final int bytesLength;
        private int streamPosition;

        Input(final byte[] bytes, final int bytesLength) {

            this.bytes = bytes;
            this.bytesLength = bytesLength;
            this.streamPosition = 0;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getBytesLength() {
            return bytesLength;
        }

        public int getAndIncreaseStreamPositionByOne() {
            return streamPosition++;
        }

        public void addToStreamPosition(int toAdd) {
            streamPosition += toAdd;
        }

        public int getStreamPosition() {
            return streamPosition;
        }
    }

    /** Used to decode message addresses and string parameters. */
    private Charset charset;

    /**
     * Creates a helper object for converting from a byte array
     */
    public DataDecoder() {
        this.charset = Charset.defaultCharset();
    }

    /**
     * Returns the character set used to decode message addresses
     * and string parameters.
     * @return the character-encoding-set used by this converter
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the character set used to decode message addresses
     * and string parameters.
     * @param charset the desired character-encoding-set to be used by this converter
     */
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /**
     * Converts the byte array to a simple message.
     * Assumes that the byte array is a message.
     * @return a message containing the data specified in the byte stream
     */
    public MessageData convertMessage(byte[] bytes, int bytesLength) {
        final Input rawInput = new Input(bytes, bytesLength);
        final MessageData messageData = new MessageData();

        final CharSequence types = readTypes(rawInput);
        for (int ti = 0; ti < types.length(); ++ti) {
            if ('[' == types.charAt(ti)) {
                // we're looking at an array -- read it in
                messageData.addArgument(readArray(rawInput, types, ++ti));
                // then increment i to the end of the array
                while (types.charAt(ti) != ']') {
                    ti++;
                }
            } else {
                messageData.addArgument(readArgument(rawInput, types.charAt(ti)));
            }
        }
        return messageData;
    }

    /**
     * Reads a string from the byte stream.
     * @return the next string in the byte stream
     */
    private String readString(final Input rawInput) {
        final int strLen = lengthOfCurrentString(rawInput);
        final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen, charset);
        rawInput.addToStreamPosition(strLen);
        moveToFourByteBoundry(rawInput);
        return res;
    }

    /**
     * Reads a binary blob from the byte stream.
     * @return the next blob in the byte stream
     */
    private byte[] readBlob(final Input rawInput) {
        final int blobLen = readInteger(rawInput);
        final byte[] res = new byte[blobLen];
        System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), res, 0, blobLen);
        rawInput.addToStreamPosition(blobLen);
        moveToFourByteBoundry(rawInput);
        return res;
    }

    /**
     * Reads the types of the arguments from the byte stream.
     * @return a char array with the types of the arguments,
     *   or <code>null</code>, in case of no arguments
     */
    private CharSequence readTypes(final Input rawInput) {
        final String typesStr;

        // The next byte should be a ',', but some legacy code may omit it
        // in case of no arguments, refering to "OSC Messages" in:
        // http://opensoundcontrol.org/spec-1_0
        if (rawInput.getBytes().length <= rawInput.getStreamPosition()) {
            typesStr = NO_ARGUMENT_TYPES;
        } else if (rawInput.getBytes()[rawInput.getStreamPosition()] != ',') {
            // XXX should we not rather fail-fast -> throw exception?
            typesStr = NO_ARGUMENT_TYPES;
        } else {
            rawInput.getAndIncreaseStreamPositionByOne();
            typesStr = readString(rawInput);
        }

        return typesStr;
    }

    /**
     * Reads an object of the type specified by the type char.
     * @param type type of the argument to read
     * @return a Java representation of the argument
     */
    private Object readArgument(final Input rawInput, final char type) {
        switch (type) {
            case 'u' :
                return readUnsignedInteger(rawInput);
            case 'i' :
                return readInteger(rawInput);
            case 'h' :
                return readLong(rawInput);
            case 'f' :
                return readFloat(rawInput);
            case 'd' :
                return readDouble(rawInput);
            case 's' :
                return readString(rawInput);
            case 'b' :
                return readBlob(rawInput);
            case 'c' :
                return readChar(rawInput);
            case 'C' :
                return readUnicodeChar(rawInput);
            case 'N' :
                return null;
            case 'T' :
                return Boolean.TRUE;
            case 'F' :
                return Boolean.FALSE;
            default:
                // XXX Maybe we should let the user choose what to do in this
                //   case (we encountered an unknown argument type in an
                //   incomming message):
                //   just ignore (return null), or throw an exception?
//				throw new UnsupportedOperationException(
//						"Invalid or not yet supported OSC type: '" + type + "'");
                return null;
        }
    }

    /**
     * Reads a unicode char from the byte stream.
     * @return a {@link Character}
     */
    private Character readUnicodeChar(final Input rawInput) {
        return readString(rawInput).charAt(0);
    }

    /**
     * Reads a char from the byte stream.
     * @return a {@link Character}
     */
    private Character readChar(final Input rawInput) {
        return (char) (int) readInteger(rawInput);
    }

    private BigInteger readBigInteger(final Input rawInput, final int numBytes) {
        final byte[] myBytes = new byte[numBytes];
        System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), myBytes, 0, numBytes);
        rawInput.addToStreamPosition(numBytes);
        return  new BigInteger(myBytes);
    }

    /**
     * Reads a double from the byte stream.
     * @return a 64bit precision floating point value
     */
    private Object readDouble(final Input rawInput) {
        final BigInteger doubleBits = readBigInteger(rawInput, 8);
        return Double.longBitsToDouble(doubleBits.longValue());
    }

    /**
     * Reads a float from the byte stream.
     * @return a 32bit precision floating point value
     */
    private Float readFloat(final Input rawInput) {
        final BigInteger floatBits = readBigInteger(rawInput, 4);
        return Float.intBitsToFloat(floatBits.intValue());
    }

    /**
     * Reads a double precision integer (64 bit integer) from the byte stream.
     * @return double precision integer (64 bit)
     */
    private Long readLong(final Input rawInput) {
        final BigInteger longintBytes = readBigInteger(rawInput, 8);
        return longintBytes.longValue();
    }

    /**
     * Reads an Integer (32 bit integer) from the byte stream.
     * @return an {@link Integer}
     */
    private Integer readInteger(final Input rawInput) {
        final BigInteger intBits = readBigInteger(rawInput, 4);
        return intBits.intValue();
    }

    /**
     * Reads an unsigned integer (32 bit) from the byte stream.
     * This code is copied from {@see http://darksleep.com/player/JavaAndUnsignedTypes.html},
     * which is licensed under the Public Domain.
     * @return single precision, unsigned integer (32 bit) wrapped in a 64 bit integer (long)
     */
    private Long readUnsignedInteger(final Input rawInput) {

        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int fourthByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return ((long) (firstByte << 24
                | secondByte << 16
                | thirdByte << 8
                | fourthByte))
                & 0xFFFFFFFFL;
    }


    /* Reads an array from the byte stream.
     * @param types
     * @param pos at which position to start reading
     * @return the array that was read
     */
    private List<Object> readArray(final Input rawInput, final CharSequence types, int pos) {
        int arrayLen = 0;
        while (types.charAt(pos + arrayLen) != ']') {
            arrayLen++;
        }
        final List<Object> array = new ArrayList<Object>(arrayLen);
        for (int ai = 0; ai < arrayLen; ai++) {
            array.add(readArgument(rawInput, types.charAt(pos + ai)));
        }
        return array;
    }

    //Get the length of the string currently in the byte stream.
    private int lengthOfCurrentString(final Input rawInput) {
        int len = 0;
        while (rawInput.getBytes()[rawInput.getStreamPosition() + len] != 0) {
            len++;
        }
        return len;
    }

    //Move to the next byte with an index in the byte array which is dividable by four.
    private void moveToFourByteBoundry(final Input rawInput) {
        // If i am already at a 4 byte boundry, I need to move to the next one
        final int mod = rawInput.getStreamPosition() % 4;
        rawInput.addToStreamPosition(4 - mod);
    }
}