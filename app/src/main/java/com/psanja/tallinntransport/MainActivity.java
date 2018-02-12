package com.psanja.tallinntransport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.psanja.tallinntransport.DATAclasses.Stop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    RequestQueue queue;

    StopsManager stopsManager;
    private SwipeRefreshLayout refresh;
    private DeparturesAdapter mainAdapter, searchAdapter;

    private FrameLayout mapview;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private MapManager mapManager;

    private LinearLayout search;
    private EditText searchname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            finish();
            return;
        }

        queue = Volley.newRequestQueue(this);

        refresh = findViewById(R.id.refresh);
        refresh.setColorScheme(R.color.buslight, R.color.tramlight, R.color.trolleylight);
        setRefresh(true);
        refresh.setOnRefreshListener(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_holder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainAdapter = new DeparturesAdapter(new ArrayList<Stop>(), getApplicationContext());
        recyclerView.setAdapter(mainAdapter);

        RecyclerView searchView = findViewById(R.id.search_holder);
        searchView.setLayoutManager(new LinearLayoutManager(this));
        searchAdapter = new DeparturesAdapter(new ArrayList<Stop>(), getApplicationContext());
        searchView.setAdapter(searchAdapter);

        stopsManager = new StopsManager(queue);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapview = findViewById(R.id.mapholder);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                googleMap.setTrafficEnabled(true);
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setTiltGesturesEnabled(false);
                mapManager = new MapManager(getApplicationContext(), googleMap, queue);
            }
        });

        search = findViewById(R.id.search);
        searchname = findViewById(R.id.search_name);
        searchname.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && v.getText() != null) {
                    SearchStops(v.getText().toString());
                }
                return false;
            }
        });

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_timetable:
                        mapview.setVisibility(View.GONE);
                        search.setVisibility(View.GONE);
                        setRefresh(false);
                        refresh.setVisibility(View.VISIBLE);
                        mapManager.stop();
                        return true;
                    case R.id.navigation_map:
                        refresh.setVisibility(View.GONE);
                        search.setVisibility(View.GONE);
                        mapview.setVisibility(View.VISIBLE);
                        mapManager.start();
                        return true;
                    case R.id.navigation_search:
                        refresh.setVisibility(View.GONE);
                        mapview.setVisibility(View.GONE);
                        search.setVisibility(View.VISIBLE);
                        mapManager.stop();
                        return true;
                }
                return false;
            }
        });

        LoadStops();
    }

    @Override
    public void onRefresh() {
        setRefresh(true);
        mainAdapter.clear();
        getNearestStops();
    }

    public void setRefresh(boolean type) {
        refresh.setRefreshing(type);
    }

    private void LoadStops() {
        String url ="https://transport.tallinn.ee/data/stops.txt";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    ProcessStops(response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });
        queue.add(stringRequest);

    }

    private void ProcessStops(String response) {
        try {
            String preprocess = new String(response.getBytes("ISO-8859-1"), "UTF-8");
            String[] rawstops = preprocess.split("\n");
            for (String rawstop: rawstops) {
                stopsManager.add(rawstop.split(";"));
            }
            getNearestStops();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("MissingPermission")
    private void getNearestStops() {
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15.5f));
                    String url ="https://transit.land/api/v1/stops?lat=" + location.getLatitude() + "&lon=" + location.getLongitude() + "&r=200&sort_key=name";
                    JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    processNearestStops(response);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                        }
                    });
                    queue.add(jsObjRequest);
                }
            }
        });
    }

    private void processNearestStops(JSONObject response) {
        try {
            String name = "";

            JSONArray stops = response.getJSONArray("stops");
            for (int i=0; i < stops.length(); i++) {
                JSONObject stop = stops.getJSONObject(i);
                String newname = stop.getString("name");
                if (!Objects.equals(newname, name)) {
                    name = newname;
                    stopsManager.get(name, 15, mainAdapter);
                }
            }
            setRefresh(false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private void SearchStops(String stop) {
        searchAdapter.clear();
        stopsManager.get(stop, 100, searchAdapter);
    }

}
