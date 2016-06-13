package sg.edu.smu.ecology;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by anurooppv on 11/6/2016.
 */
public class ForwardRequest implements Parcelable {

    private Event event;
    private String deviceID;

    public ForwardRequest(){

    }

    protected ForwardRequest(Parcel in) {
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

    public static final Creator<ForwardRequest> CREATOR = new Creator<ForwardRequest>() {
        @Override
        public ForwardRequest createFromParcel(Parcel in) {
            return new ForwardRequest(in);
        }

        @Override
        public ForwardRequest[] newArray(int size) {
            return new ForwardRequest[size];
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

    public <T> ForwardRequest createFromParcel(Parcel parcel) {
        return new ForwardRequest(parcel);
    }
}
