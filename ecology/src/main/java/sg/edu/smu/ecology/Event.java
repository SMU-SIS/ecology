package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

public class Event implements Parcelable {

    private String type;
    private Bundle data;

    public Event(){}

    protected Event(Parcel in) {
        type = in.readString();
        data = in.readBundle();
    }

    public String getType() {
        return type;
    }

    public void setType(String value) {
        type = value;
    }

    public Bundle getData() {
        return data;
    }

    public void setData(Bundle value) {
        data = value;
    }

    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    // This is where we write the values we want to save to the `Parcel`.
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeBundle(data);
    }
    public <T> Event createFromParcel(Parcel parcel) {
        return new Event(parcel);
    }

}