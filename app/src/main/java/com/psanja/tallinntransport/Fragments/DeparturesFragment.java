package com.psanja.tallinntransport.Fragments;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.psanja.tallinntransport.Adapters.DeparturesAdapter;
import com.psanja.tallinntransport.Managers.StatusManager;
import com.psanja.tallinntransport.Managers.StopsManager;
import com.psanja.tallinntransport.R;
import com.psanja.tallinntransport.Utils.Keyboard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class DeparturesFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener{

    private int start = 0;

    private FragmentActivity context;
    private final Handler mHandler = new Handler();
    private FusedLocationProviderClient mFusedLocationClient;

    private StopsManager stopsManager;
    private StatusManager statusManager;
    private RequestQueue queue;

    private boolean amIsearch;

    private SwipeRefreshLayout refresh;
    private AutoCompleteTextView search;
    private DeparturesAdapter mainAdapter;

    public DeparturesFragment() {
        // Required empty public constructor
    }

    public void SetupMe(RequestQueue queue, StatusManager statusManager, StopsManager stopsManager, boolean amIsearch) {
        this.queue = queue;
        this.statusManager = statusManager;
        this.stopsManager = stopsManager;
        this.amIsearch = amIsearch;
    }

    public void tryFirstStart() {
        Log.w("DEBUG", String.valueOf(amIsearch) + String.valueOf(start));
        if (start < 1) {
            start+=1;
            return;
        }
        if (amIsearch) {
            search.setAdapter(new ArrayAdapter<>(context, android.R.layout.select_dialog_item, stopsManager.GetStops(false)));
        } else {
            getNearestStops();
        }
    }

    public void setLive(boolean live) {
        try {
            mainAdapter.isLive = live;
        } catch (Exception ignore) {

        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireActivity();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View holder = inflater.inflate(R.layout.fragment_departures, container, false);
        refresh = holder.findViewById(R.id.departures_refresh);
        refresh.setColorScheme(R.color.buslight, R.color.tramlight, R.color.trolleylight);
        refresh.setOnRefreshListener(this);

        RecyclerView recyclerView = holder.findViewById(R.id.departures_adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        mainAdapter = new DeparturesAdapter(context, statusManager);
        recyclerView.setAdapter(mainAdapter);

        if (amIsearch) {
            search = holder.findViewById(R.id.departures_search);
            search.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SearchStops(((TextView) view).getText().toString().trim());
                    Keyboard.hide(context, search);
                }
            });
            search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE && v.getText() != null) {
                        SearchStops(null);
                    }
                    return false;
                }
            });
            search.setVisibility(View.VISIBLE);
        } else {
            setRefresh(true);
        }

        tryFirstStart();
        return holder;
    }

    @Override
    public void onRefresh() {
        if (amIsearch) {
            SearchStops(null);
        } else {
            getNearestStops();
        }
    }

    private void setRefresh(boolean type) {
        refresh.setRefreshing(type);
    }


    @SuppressLint("MissingPermission")
    private void getNearestStops() {
        mHandler.postDelayed(mLocationFailed, 5000);
        mFusedLocationClient.getLastLocation().addOnSuccessListener(context, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {// && mapManager != null) {
                    mHandler.removeCallbacks(mLocationFailed);
                    statusManager.report(StatusManager.Status.LOCATION_OK);
                    statusManager.setmap(new LatLng(location.getLatitude(), location.getLongitude()));
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
                            statusManager.report(StatusManager.Status.DEPARTURES_ERROR);
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
            statusManager.report(StatusManager.Status.LOCATION_ERROR);
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
        if (stop == null) {
            stop = search.getText().toString().trim();
        }
        mainAdapter.clear();
        stopsManager.get(stop, 100, mainAdapter);
        setRefresh(false);
    }

}
