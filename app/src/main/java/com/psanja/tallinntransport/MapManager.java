package com.psanja.tallinntransport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

class MapManager {

    private final Handler mHandler = new Handler();
    private Vehicle[] availablevehicles = new Vehicle[10000];
    private GoogleMap googleMap;
    private Context context;
    private RequestQueue queue;
    private boolean running;
    private TextView error;

    MapManager(Context context, GoogleMap googleMap, TextView error, RequestQueue queue) {
        this.context = context;
        this.googleMap = googleMap;
        this.error = error;
        this.queue = queue;
    }

    void start() {
        if (!running) {
            mHandler.post(mHandlerUpdate);
            running = true;
        }
    }

    void stop() {
        if (running) {
            mHandler.removeCallbacks(mHandlerUpdate);
            running = false;
        }
        error.setVisibility(View.GONE);
    }

    private final Runnable mHandlerUpdate = new Runnable() {
        public void run() {
            String url = "https://transport.tallinn.ee/gps.txt?";

            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (response.isEmpty()) {
                                error.setVisibility(View.VISIBLE);
                            } else {
                                error.setVisibility(View.GONE);
                                for (String unparsedvehicle : response.split(System.getProperty("line.separator"))) {
                                    //VehicleManager.ShortVehicle vehicle = vehicleManager.setVehicle(unparsedvehicle);
                                    setVehicle(unparsedvehicle);
                                }
                            }
                        }
                    }, null);
            queue.add(stringRequest);
            mHandler.postDelayed(mHandlerUpdate, 10000);
        }
    };

    private void setVehicle(String unparsed) {
        Vehicle newvehicle = new Vehicle(unparsed);
        if (Objects.equals(newvehicle.number, "0")) {
            return;
        }
        if (availablevehicles[newvehicle.id] == null) {
            newvehicle.createMarker();
            availablevehicles[newvehicle.id] = newvehicle;
        } else if (newvehicle != availablevehicles[newvehicle.id]) {
            availablevehicles[newvehicle.id].updateMarker(newvehicle.latitude, newvehicle.longtitude, newvehicle.rotation);
        }
    }


    public class Vehicle {

        String number;
        Integer type;
        Integer id;
        Double latitude;
        Double longtitude;
        Integer rotation;
        Marker marker;

        Vehicle(String unparsed) {
            String[] parsed = unparsed.split(",");
            this.id = Integer.valueOf(parsed[6]);
            this.type = Integer.valueOf(parsed[0]);
            this.number = parsed[1];
            this.longtitude = Double.valueOf(parsed[2]) / 1000000;
            this.latitude = Double.valueOf(parsed[3]) / 1000000;
            this.rotation = Integer.valueOf(parsed[5]);
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
            }

            marker = googleMap.addMarker(new MarkerOptions()
                    .icon(generateIcon(iconbase, number))
                    .position(new LatLng(latitude, longtitude))
                    .flat(true)
                    .anchor(0.5f, 0.1f)
                    .rotation(rotation)
                    .title(number));

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
