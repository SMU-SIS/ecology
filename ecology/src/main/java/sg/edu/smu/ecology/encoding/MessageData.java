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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Anuroop PATTENA VANIYAR
 * @author Quentin ROY
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

    /**
     * Returns an estimation of the number of bytes of this message encoding. This estimation will
     * always be greater than the actual encoded size so it is safe to use it to allocate the
     * encoding byte buffer.
     *
     * @return the estimation.
     */
    public int getMaximumByteSize() {
        return dataSize + getStringSize(getTypeTags(), false);
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

    public void addArgument(Map<?, ?> map) {
        typeTags += '{';
        // Add the couples key / values.
        for(Map.Entry<?, ?> entry: map.entrySet()){
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
        int stringSize = getStringSize(param, true);
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
        typeTags += 'c';
        arguments.add(c);
        dataSize += 4;
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

    private int getStringSize(String str, boolean unicode) {
        // Add 1 because a string must be zero terminated
        int len = unicode ? str.length() * 2 + 1 : str.length() + 1;
        // logger.debug("GetStringSize : " + str + " : " + len);
        int mod = len % 4;
        // logger.debug("MOD " + mod);
        int pad = (mod > 0) ? 4 - mod : 0;
        // logger.debug("PAD " + pad);
        return len + pad;
    }
}
