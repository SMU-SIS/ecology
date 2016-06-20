package sg.edu.smu.ecology;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by anurooppv on 11/6/2016.
 */
public class DependentEvent implements Parcelable {

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

    public static final Creator<DependentEvent> CREATOR = new Creator<DependentEvent>() {
        @Override
        public DependentEvent createFromParcel(Parcel in) {
            return new DependentEvent(in);
        }

        @Override
        public DependentEvent[] newArray(int size) {
            return new DependentEvent[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(event, flags);
        dest.writeString(deviceID);
    }

    public <T> DependentEvent createFromParcel(Parcel parcel) {
        return new DependentEvent(parcel);
    }
}
