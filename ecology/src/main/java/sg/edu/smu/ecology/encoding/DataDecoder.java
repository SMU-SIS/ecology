/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 *
 *
 * Modified from https://github.com/hoijui/JavaOSC.
 *
 * Copyright (c) 2002-2014, Chandrasekhar Ramakrishnan / Illposed Software
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the {organization} nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package sg.edu.smu.ecology.encoding;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
public class DataDecoder {

    private static final String NO_ARGUMENT_TYPES = "";

    // This class represents an argument and the amount of character it took to code in the types
    // sequence. It is returned by the readArgument methods.
    private static class Argument {
        // The increment in the types sequence caused by the argument.
        public int typeCodeSize;
        // The value of the argument.
        public Object value;

        public Argument(Object value, int typeCodeSize){
            this.typeCodeSize = typeCodeSize;
            this.value = value;
        }
    }

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
    public List<Object> convertMessageArgs(byte[] bytes, int bytesLength) {
        final Input rawInput = new Input(bytes, bytesLength);
        final List<Object> messageData = new ArrayList<>();
        final CharSequence types = readTypes(rawInput);

        for (int ti = 0; ti < types.length();) {
            Argument arg = readOneArgument(rawInput, types, ti);
            messageData.add(arg.value);
            ti += arg.typeCodeSize;
        }
        return messageData;
    }

    public MessageData convertMessage(byte[] bytes, int bytesLength) {
        MessageData data = new MessageData();
        for(Object arg: convertMessageArgs(bytes, bytesLength)){
            data.addArgument(arg);
        }
        return data;
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
        return readString(rawInput);
    }

    /**
     * Read one argument from the byte stream (composite arguments like list are entirely read,
     * including their content).
     * @param rawInput The stream where to read the argument.
     * @param types The types sequence.
     * @param position The current position in the type list.
     * @return The argument.
     */
    private Argument readOneArgument(final Input rawInput, final CharSequence types, int position){
        char type = types.charAt(position);
        switch (type) {
            case 'u' :
                return new Argument(readUnsignedInteger(rawInput), 1);
            case 'i' :
                return new Argument(readInteger(rawInput), 1);
            case 'h' :
                return new Argument(readLong(rawInput), 1);
            case 'f' :
                return new Argument(readFloat(rawInput), 1);
            case 'd' :
                return new Argument(readDouble(rawInput), 1);
            case 's' :
                return new Argument(readString(rawInput), 1);
            case 'b' :
                return new Argument(readBlob(rawInput), 1);
            case 'c' :
                return new Argument(readChar(rawInput), 1);
            case 'N' :
                return new Argument(null, 1);
            case 'T' :
                return new Argument(Boolean.TRUE, 1);
            case 'F' :
                return new Argument(Boolean.FALSE, 1);
            case '[':
                return readListArgument(rawInput, types, position);
            case '{':
                return readMapArgument(rawInput, types, position);
            default:
                // XXX Maybe we should let the user choose what to do in this
                //   case (we encountered an unknown argument type in an
                //   incomming message):
                //   just ignore (return null), or throw an exception?
                throw new UnsupportedOperationException(
                        "Invalid or not yet supported type: '" + type + "'");
        }
    }

    private Argument readMapArgument(final Input rawInput, final CharSequence types, int position){
        // Initialize the map.
        HashMap<Object, Object> map = new HashMap<>();
        // Pass the opening bracket (position should be on the '{' that indicates the beginning
        // of a map).
        int i = position + 1;
        // Iteratively parse each key value pairs.
        while(types.charAt(i) != '}'){
            Argument keyArg = readOneArgument(rawInput, types, i);
            i += keyArg.typeCodeSize;
            Argument valArg = readOneArgument(rawInput, types, i);
            i += valArg.typeCodeSize;
            map.put(keyArg.value, valArg.value);
        }
        // Calculate the position increment, including the opening and closing brackets.
        int typeInc = i - position + 1;
        return new Argument(map, typeInc);
    }

    private Argument readListArgument(final Input rawInput, final CharSequence types, int position){
        // Initialize the array.
        List<Object> list = new ArrayList<>();
        // Pass the opening bracket (position should be on the '[' that indicates the beginning of
        // an array).
        int li = position + 1;
        // Parse the content of the array.
        while(types.charAt(li) != ']'){
            Argument subArg = readOneArgument(rawInput, types, li);
            li += subArg.typeCodeSize;
            list.add(subArg.value);
        }
        // Calculate the position increment, including the opening and closing brackets.
        int typeInc = li - position + 1;
        return new Argument(list, typeInc);
    }

    /**
     * Reads a unicode char from the byte stream.
     * @return a {@link Character}
     */
    private Character readChar(final Input rawInput) {
        // Characters are encoded as integers.
        return (char) readInteger(rawInput).intValue();
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