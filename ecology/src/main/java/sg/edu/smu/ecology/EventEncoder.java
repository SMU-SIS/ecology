package sg.edu.smu.ecology;

import android.os.Bundle;

/**
 * Created by Quentin ROY on 18/6/16.
 *
 * Encode and decode {@link Event}.
 */
class EventEncoder {

    /**
     * Parse an array of bytes (usually coming from {@link #pack(Event)} into an event).
     *
     * @param eventData the array of bytes to parse
     * @return the parsed event
     */
    public static Event unpack(byte[] eventData){
        // TODO
        throw new UnsupportedOperationException("Event encoding is not implemented yet.");
    }

    /**
     * Pack an event into an array of byte that can be decoded by {@link #unpack(byte[])}.
     *
     * @return an array of byte
     */
    public static byte[] pack(Event event){
        // TODO
        throw new UnsupportedOperationException("Event decoding is not implemented yet.");
    }
}
