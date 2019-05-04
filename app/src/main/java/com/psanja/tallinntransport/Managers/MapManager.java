package com.psanja.tallinntransport.Managers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.psanja.tallinntransport.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class MapManager implements OnMapReadyCallback {

    private final Handler mHandler = new Handler();
    private Vehicle[] availablevehicles = new Vehicle[15000];
    private SupportMapFragment mapfragment;
    private GoogleMap googleMap;
    private Context context;
    private RequestQueue queue;
    private boolean running;
    private StatusManager statusManager;

    private CameraUpdate requestedplace;

    public MapManager(Context context, RequestQueue queue, StatusManager statusManager, SupportMapFragment mapfragment) {
        this.context = context;
        this.queue = queue;
        this.statusManager = statusManager;
        this.mapfragment = mapfragment;
        start();
    }

    public void setMap(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public Boolean SetCamera(Integer id) {
        try {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(availablevehicles[id].marker.getPosition(), 15.5f));
            availablevehicles[id].marker.showInfoWindow();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean SetCamera(LatLng loc, Float zoom) {
        if (zoom == null)
            zoom = 15.5f;
        try {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, zoom));
            return true;
        } catch (Exception e) {
            requestedplace = CameraUpdateFactory.newLatLngZoom(loc, zoom);
            return false;
        }
    }


    public void start() {
        if (!running) {
            mHandler.post(mHandlerUpdate);
            running = true;
        }
    }

    public void stop() {
        if (running) {
            mHandler.removeCallbacks(mHandlerUpdate);
            running = false;
        }
    }

    private final Runnable mHandlerUpdate = new Runnable() {
        public void run() {
            if (googleMap != null) {
                Log.w("DEBUG", "MAP UPDATE");
                if (requestedplace != null) {
                    googleMap.moveCamera(requestedplace);
                    requestedplace = null;
                }
                DownloadTLT();
                DownloadElron();
                mHandler.postDelayed(mHandlerUpdate, 10000);
            } else {
                Log.w("DEBUG", "MAP RETRY");
                mapfragment.getMapAsync(MapManager.this);
                mHandler.postDelayed(mHandlerUpdate, 1000);
            }
        }
    };


    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style));
        //googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        googleMap.setTrafficEnabled(true);
        googleMap.setMyLocationEnabled(true);
        googleMap.getUiSettings().setRotateGesturesEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setTiltGesturesEnabled(false);
        this.googleMap = googleMap;
        this.SetCamera(new LatLng(59.437060, 24.753406), 13f);
    }

    private void DownloadTLT() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://transport.tallinn.ee/gps.txt",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.isEmpty()) {
                            statusManager.report(StatusManager.Status.MAP_BUS_ERROR);
                        } else {
                            statusManager.report(StatusManager.Status.MAP_BUS_OK);
                            for (String unparsedvehicle : response.split("\n")) {
                                setVehicle(new Vehicle(unparsedvehicle));
                            }
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError e) {
                        statusManager.report(StatusManager.Status.MAP_BUS_ERROR);
                    }
                });
        queue.add(stringRequest);
    }

    private void DownloadElron() {
        JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/map",null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray trains = response.getJSONArray("data");
                            for (int i=0; i < trains.length(); i++) {
                                setVehicle(new Vehicle(trains.getJSONObject(i)));
                            }
                            statusManager.report(StatusManager.Status.MAP_TRAIN_OK);
                        } catch (Exception e) {
                            statusManager.report(StatusManager.Status.MAP_TRAIN_ERROR);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        statusManager.report(StatusManager.Status.MAP_TRAIN_ERROR);
                    }
            });
        queue.add(elronRequest);
    }

    private void setVehicle(Vehicle vehicle) {
        if (Objects.equals(vehicle.number, "0")) {
            return;
        }
        if (availablevehicles[vehicle.id] == null) {
            vehicle.createMarker();
            availablevehicles[vehicle.id] = vehicle;
        } else if (vehicle != availablevehicles[vehicle.id]) {
            availablevehicles[vehicle.id].updateMarker(vehicle.latitude, vehicle.longitude, vehicle.rotation);
        }
    }



    public class Vehicle {

        String number;
        Integer type;
        Integer id;
        Double latitude;
        Double longitude;
        Integer rotation;
        Marker marker;
        String title;
        String info;

        Vehicle(String unparsed) {
            String[] parsed = unparsed.split(",");
            this.id = Integer.valueOf(parsed[6]);
            this.type = Integer.valueOf(parsed[0]);
            this.number = parsed[1];
            this.longitude = Double.valueOf(parsed[2]) / 1000000;
            this.latitude = Double.valueOf(parsed[3]) / 1000000;
            this.rotation = Integer.valueOf(parsed[5]);
        }

        Vehicle(JSONObject unparsed) {
            try {
                this.id = 10000 + unparsed.getInt("reis");
                this.type = 4;
                this.number = unparsed.getString("reis");
                this.longitude = unparsed.getDouble("longitude");
                this.latitude = unparsed.getDouble("latitude");
                try {
                    this.rotation = unparsed.getInt("rongi_suund");
                } catch (Exception e) {
                    this.rotation = 0;
                }
                this.title = unparsed.getString("liin");
                this.info = context.getResources().getString(R.string.arrives_cont) + unparsed.getString("reisi_lopp_aeg");// + "\nSpeed: " + unparsed.getString("kiirus")+"km/h";
            } catch (Exception e) {
                e.printStackTrace();
                statusManager.report(StatusManager.Status.MAP_TRAIN_ERROR);
            }

        }

        void createMarker() {
            Integer iconbase = 0;
            switch (type) {
                case 1:
                    //trolley
                    iconbase = R.drawable.ic_map_blue;
                    break;
                case 2:
                    //bus
                    iconbase = R.drawable.ic_map_green;
                    break;
                case 3:
                    //tram
                    iconbase = R.drawable.ic_map_orange;
                    break;
                case 4:
                    //train
                    iconbase = R.drawable.ic_map_red;
                    break;
            }


            //marker = googleMap.addGroundOverlay(new GroundOverlayOptions()
                    //.image(generateIcon(iconbase, number))
                    //.position(new LatLng(latitude, longitude), 80f));


            marker = googleMap.addMarker(new MarkerOptions()
                    .icon(generateIcon(iconbase, number))
                    .position(new LatLng(latitude, longitude))
                    .flat(true)
                    .anchor(0.5f, 0.1f)
                    .rotation(rotation));
            if (title!=null)
                marker.setTitle(title);
            if (info!=null)
                marker.setSnippet(info);

        }

        void updateMarker(Double latitude, Double longtitude, Integer rotation) {
            marker.setPosition(new LatLng(latitude, longtitude));
            marker.setRotation(rotation);
        }

        private BitmapDescriptor generateIcon(int vectorResId, String label) {
            Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
            vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
            Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.draw(canvas);

            Paint paint = new Paint();

            paint.setColor(Color.WHITE);
            paint.setFakeBoldText(true);
            if (label.length() <= 1) {
                paint.setTextSize(24);
                canvas.drawText(label, vectorDrawable.getIntrinsicWidth() / 2 - 6, vectorDrawable.getIntrinsicHeight()/4*3, paint);
            } else {
                paint.setTextSize(18);
                canvas.drawText(label, vectorDrawable.getIntrinsicWidth() / 2 - 10, vectorDrawable.getIntrinsicHeight()/4*3, paint);

            }

            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }

    }

}
