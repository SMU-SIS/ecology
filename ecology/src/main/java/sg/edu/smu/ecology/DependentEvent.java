package sg.edu.smu.ecology;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by anurooppv on 11/6/2016.
 */
public class DependentEvent implements Serializable {

    private Event event;
    private String deviceID;

    public DependentEvent(){

    }

    protected DependentEvent(Parcel in) {
        event = in.readParcelable(Event.class.getClassLoader());
        deviceID = in.readString();
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

}
