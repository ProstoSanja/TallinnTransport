package com.psanja.tallinntransport.Managers;

import com.google.android.gms.maps.model.LatLng;

public class StatusManager {

    public enum Status {
        MAP_BUS_OK,
        MAP_BUS_ERROR,
        MAP_TRAIN_OK,
        MAP_TRAIN_ERROR,
        SETUP_OK,
        SETUP_ERROR,
        DEPARTURES_ERROR,
        DEPARTURES_OK,
        LOCATION_OK,
        LOCATION_ERROR
    }

    private OnStatusListener onStatusListener;

    public StatusManager(OnStatusListener onStatusListener) {
        this.onStatusListener = onStatusListener;
    }

    public void report(Status status) {
        onStatusListener.onStatus(status);
    }
    public void setmap(LatLng location) {
        onStatusListener.onMapPosition(location);
    }
    public boolean setmap(Integer markerId) {
        return onStatusListener.onMapPosition(markerId);
    }

    public interface OnStatusListener {
        void onStatus(Status status);
        void onMapPosition(LatLng location);
        boolean onMapPosition(Integer markerId);
    }
}
