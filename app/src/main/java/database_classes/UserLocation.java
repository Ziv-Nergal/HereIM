package database_classes;

import com.google.android.gms.maps.model.LatLng;

public class UserLocation {

    private String address;
    private double lat;
    private double lng;

    public UserLocation() { }

    public String getAddress() { return address; }

    public double getLat() { return lat; }

    public double getLng() { return lng; }

    public LatLng getLatLng() {
        return new LatLng(lat, lng);
    }

    public void setAddress(String address) { this.address = address; }

    public void setLat(double lat) { this.lat = lat; }

    public void setLng(double lng) { this.lng = lng; }
}
