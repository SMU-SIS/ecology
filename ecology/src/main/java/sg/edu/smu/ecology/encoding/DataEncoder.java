/*
 * Copyright (C) 2017, Singapore Management University.
 * All rights reserved.
 *
 * This code is licensed under the MIT license.
 * See file LICENSE (or LICENSE.html) for more information.
 *
 *
 * Modified from https://github.com/odbol/OSCLib.
 *
 * Copyright (c) 2008 Martin Wood-Mitrovski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package sg.edu.smu.ecology.encoding;

import org.apache.mina.core.buffer.IoBuffer;

import java.math.BigInteger;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;

/**
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
 */
public class DataEncoder {

    private static CharsetEncoder charsetEncoder = Charset.forName("UTF-8").newEncoder();

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
            // Characters are encoded as integers.
            buffer.putInt((int) (char) arg);
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
        buffer.putString(s, charsetEncoder);
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
