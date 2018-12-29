package com.psanja.tallinntransport;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

class MapManager {

    private final Handler mHandler = new Handler();
    private Vehicle[] availablevehicles = new Vehicle[15000];
    private GoogleMap googleMap;
    private Context context;
    private RequestQueue queue;
    private boolean running;
    private StatusManager statusManager;

    MapManager(Context context, GoogleMap googleMap, StatusManager statusManager, RequestQueue queue) {
        this.context = context;
        this.googleMap = googleMap;
        this.statusManager = statusManager;
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
    }

    private final Runnable mHandlerUpdate = new Runnable() {
        public void run() {
            Log.w("DEBUG", "MAP UPDATE");

            StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://transport.tallinn.ee/gps.txt",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            if (response.isEmpty()) {
                                statusManager.reportMap(false);
                            } else {
                                statusManager.reportMap(true);
                                for (String unparsedvehicle : response.split(System.getProperty("line.separator"))) {
                                    //VehicleManager.ShortVehicle vehicle = vehicleManager.setVehicle(unparsedvehicle);
                                    setVehicle(new Vehicle(unparsedvehicle));
                                }
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError e) {
                            statusManager.reportMap(false);
                        }
                    });
            queue.add(stringRequest);
            DownloadElron();
            mHandler.postDelayed(mHandlerUpdate, 10000);
        }
    };

    private void DownloadElron() {
        JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/map",null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        statusManager.reportTrain(true);
                        try {
                            JSONArray trains = response.getJSONArray("data");
                            for (int i=0; i < trains.length(); i++) {
                                setVehicle(new Vehicle(trains.getJSONObject(i)));

                            }
                        } catch (Exception e) {
                            statusManager.reportTrain(false);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        statusManager.reportTrain(false);
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
                this.rotation = unparsed.getInt("rongi_suund");
                this.title = unparsed.getString("liin");
                this.info = "Arrives: " + unparsed.getString("reisi_lopp_aeg");// + "\nSpeed: " + unparsed.getString("kiirus")+"km/h";
            } catch (Exception e) {
                e.printStackTrace();
                statusManager.reportTrain(false);
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
