package com.psanja.tallinntransport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.psanja.tallinntransport.DATAclasses.Stop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {


    InputMethodManager imm;
    RequestQueue queue;

    StopsManager stopsManager;
    private SwipeRefreshLayout refresh;
    private EditText search;
    private DeparturesAdapter mainAdapter;
    private SparseArray<ArrayList<Stop>> dataBackup = new SparseArray<>();

    private FrameLayout mapview;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationClient;
    private MapManager mapManager;

    private int currentsate = R.id.navigation_timetable;
    private StatusManager statusManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO: stop mapmanager on pause/stop
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            finish();
            return;
        }

        queue = Volley.newRequestQueue(this);
        statusManager = new StatusManager(getApplicationContext(), (TextView) findViewById(R.id.maperror));
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        refresh = findViewById(R.id.refresh);
        refresh.setColorScheme(R.color.buslight, R.color.tramlight, R.color.trolleylight);
        setRefresh(true);
        refresh.setOnRefreshListener(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_holder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainAdapter = new DeparturesAdapter(getApplicationContext(), statusManager);
        recyclerView.setAdapter(mainAdapter);
        stopsManager = new StopsManager(getApplicationContext(), queue, statusManager);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mapview = findViewById(R.id.mapholder);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style));
                //googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                googleMap.setTrafficEnabled(true);
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setTiltGesturesEnabled(false);
                mapManager = new MapManager(getApplicationContext(), googleMap, statusManager, queue);
                mapManager.start();
            }
        });

        search = findViewById(R.id.search_name);
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && v.getText() != null) {
                    SearchStops(v.getText().toString());
                }
                return false;
            }
        });

        //TODO: implement search SOMEWHERE
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int state = item.getItemId();

                if (state == currentsate)
                    return false;

                setRefresh(false);
                refresh.setVisibility(View.GONE);
                mapview.setVisibility(View.GONE);
                search.setVisibility(View.GONE);
                if (currentsate != R.id.navigation_map) {
                    dataBackup.put(currentsate, mainAdapter.backup());
                }
                if (state != R.id.navigation_map) {
                    mainAdapter.tryRestore(dataBackup.get(state));
                    refresh.setVisibility(View.VISIBLE);
                }

                if (state == R.id.navigation_search) {
                    search.setVisibility(View.VISIBLE);
                    search.requestFocus();
                    imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    imm.hideSoftInputFromWindow(search.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
                    search.clearFocus();
                    if (state == R.id.navigation_map) {
                        mapview.setVisibility(View.VISIBLE);
                    }
                }

                currentsate = state;
                return true;
            }
        });

        getNearestStops();
    }

    @Override
    public void onRefresh() {
        setRefresh(true);
        if (currentsate == R.id.navigation_timetable) {
            getNearestStops();
        } else if (currentsate == R.id.navigation_search) {
            search.onEditorAction(EditorInfo.IME_ACTION_DONE);
        } else {
            setRefresh(false);
        }
    }

    public void setRefresh(boolean type) {
        refresh.setRefreshing(type);
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
                            statusManager.reportBus(false);
                            setRefresh(false);
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

            mainAdapter.clear();

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
        mainAdapter.clear();
        stopsManager.get(stop, 100, mainAdapter);
        setRefresh(false);
    }

}
