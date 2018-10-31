package com.cshriakhil.vicinity;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.ServerValue;

// POJO that represents the LocData of a user
public class LocData {

    String name;
    double latitude;
    double longitude;
    Object timestamp; // this object is a Map<String, String> when being pushed, and a Long when being read

    @Override
    public String toString() {
        return "name: " + name + ", lat: " + latitude + ", lng: " + longitude;
    }

    public LocData() {}

    public LocData(String name, double latitude, double longitude) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = ServerValue.TIMESTAMP;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Object getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Object timestamp) {
        this.timestamp = timestamp;
    }

    @Exclude
    public long getTimestampLong() {
        return (Long) timestamp;
    }

    @Exclude
    public String getTimestampString() {
        return timestamp.toString();
    }
}
