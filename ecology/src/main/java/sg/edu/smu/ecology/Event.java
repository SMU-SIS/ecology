package sg.edu.smu.ecology;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Event implements Serializable {

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

}
