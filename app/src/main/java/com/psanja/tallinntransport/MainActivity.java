package com.psanja.tallinntransport;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import androidx.annotation.NonNull;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
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


public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener, MapManager.OnMapStatusListener {


    private InputMethodManager imm;
    private RequestQueue queue;
    private final Handler mHandler = new Handler();

    private StopsManager stopsManager;
    private SwipeRefreshLayout refresh;
    private AutoCompleteTextView search;
    private DeparturesAdapter mainAdapter;
    private SparseArray<ArrayList<Stop>> dataBackup = new SparseArray<>();

    private FrameLayout mapview;
    private BottomNavigationView navigation;
    private FusedLocationProviderClient mFusedLocationClient;
    private MapManager mapManager;

    private int currentsate = R.id.navigation_timetable;
    private TextView errorlocation, errorbig, errorbus, errortrain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupAll();
        } else {
            Intent intent = new Intent(this, SetupActivity.class);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            switch (resultCode) {
                case 0:
                    finish();
                    break;
                default:
                    setupAll();
                    break;
            }
        }
    }

    private void setupAll() {
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        refresh = findViewById(R.id.refresh);
        refresh.setColorScheme(R.color.buslight, R.color.tramlight, R.color.trolleylight);
        setRefresh(true);
        refresh.setOnRefreshListener(this);


        queue = Volley.newRequestQueue(this);
        stopsManager = new StopsManager(getApplicationContext(), queue, new StopsManager.StopsManagerReport() {
            @Override
            public void onReport(Integer status) {
                switch (status) {
                    case StopsManager.SETUP_OK:
                        getNearestStops();
                        search.setAdapter(new ArrayAdapter<> (getApplicationContext(), android.R.layout.select_dialog_item, stopsManager.GetStops()));
                        break;
                    case StopsManager.SETUP_ERROR:
                        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle(getResources().getString(R.string.error_name));
                        alertDialog.setCancelable(false);
                        alertDialog.setMessage(getResources().getString(R.string.error_setup));
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.error_retry),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        stopsManager.TryLoadStops();
                                    }
                                });
                        alertDialog.show();
                        break;
                    case StopsManager.ALL_OK:
                        errorbig.setVisibility(View.GONE);
                        break;
                    case StopsManager.REQUEST_ERROR:
                        errorbig.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        RecyclerView recyclerView = findViewById(R.id.recycler_holder);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mainAdapter = new DeparturesAdapter(MainActivity.this);
        recyclerView.setAdapter(mainAdapter);

        errorlocation = findViewById(R.id.error_location);
        errorbig = findViewById(R.id.error_big);
        errorbus = findViewById(R.id.error_bus);
        errortrain = findViewById(R.id.error_train);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        search = findViewById(R.id.search_name);
        search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SearchStops(((TextView)view).getText().toString().trim());
                toggleKeyboard(false);
            }
        });
        search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE && v.getText() != null) {
                    SearchStops(v.getText().toString());
                }
                return false;
            }
        });

        stopsManager.TryLoadStops();

        mapview = findViewById(R.id.mapholder);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @SuppressLint("MissingPermission")
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getApplicationContext(), R.raw.map_style));
                //googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                googleMap.setTrafficEnabled(true);
                googleMap.setMyLocationEnabled(true);
                googleMap.getUiSettings().setRotateGesturesEnabled(false);
                googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.getUiSettings().setCompassEnabled(false);
                googleMap.getUiSettings().setTiltGesturesEnabled(false);
                mapManager = new MapManager(getApplicationContext(), googleMap, queue, MainActivity.this);
                mapManager.start();
            }
        });


        navigation = findViewById(R.id.navigation);
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
                    toggleKeyboard(true);
                } else {
                    toggleKeyboard(false);
                    search.clearFocus();
                    if (state == R.id.navigation_map) {
                        mapview.setVisibility(View.VISIBLE);
                    }
                }

                currentsate = state;
                return true;
            }
        });
    }

    public Boolean openMapAt(Integer id) {
        if (mapManager.SetCamera(id)) {
            navigation.setSelectedItemId(R.id.navigation_map);
            return true;
        }
        return false;
    }

    @Override
    public void onMapStatus(Integer status) {
        switch (status) {
            case MapManager.BUS_OK:
                mainAdapter.isLive = true;
                errorbus.setVisibility(View.GONE);
                break;
            case MapManager.BUS_ERROR:
                mainAdapter.isLive = false;
                errorbus.setVisibility(View.VISIBLE);
                break;
            case MapManager.TRAIN_OK:
                errortrain.setVisibility(View.GONE);
                break;
            case MapManager.TRAIN_ERROR:
                errortrain.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapManager != null) {
            mapManager.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapManager != null) {
            mapManager.stop();
        }
    }

    private void toggleKeyboard(Boolean state) {
        if (state) {
            imm.showSoftInput(search, InputMethodManager.SHOW_IMPLICIT);
        } else {
            imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
    }

    @Override
    public void onRefresh() {
        if (currentsate == R.id.navigation_timetable) {
            getNearestStops();
        } else if (currentsate == R.id.navigation_search) {
            search.onEditorAction(EditorInfo.IME_ACTION_DONE);
        } else {
            setRefresh(false);
        }
    }

    private void setRefresh(boolean type) {
        refresh.setRefreshing(type);
    }

    @SuppressLint("MissingPermission")
    private void getNearestStops() {
        mHandler.postDelayed(mLocationFailed, 5000);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null && mapManager != null) {
                    mHandler.removeCallbacks(mLocationFailed);
                    errorlocation.setVisibility(View.GONE);
                    mapManager.SetCamera(new LatLng(location.getLatitude(), location.getLongitude()));
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
                            onMapStatus(MapManager.BUS_ERROR);
                            onMapStatus(MapManager.TRAIN_ERROR);
                            setRefresh(false);
                        }
                    });
                    queue.add(jsObjRequest);
                }
            }
        });
    }

    private final Runnable mLocationFailed = new Runnable() {
        public void run() {
            setRefresh(false);
            errorlocation.setVisibility(View.VISIBLE);
            mHandler.postDelayed(mLocationFailed, 5000);
        }
    };

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
            if (stops.length() == 0 ) {
                stopsManager.NoStopsFound(mainAdapter);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            stopsManager.NoStopsFound(mainAdapter);
        }
        setRefresh(false);
    }


    private void SearchStops(String stop) {
        mainAdapter.clear();
        stopsManager.get(stop, 100, mainAdapter);
        setRefresh(false);
    }

}
