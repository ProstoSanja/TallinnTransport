package com.psanja.tallinntransport;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.psanja.tallinntransport.DATAclasses.Stop;
import com.psanja.tallinntransport.DATAclasses.StopIDs;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StopsManager {

    private Context context;
    private RequestQueue queue;
    private StopsManagerReport stopsManagerReport;

    private Map<String, StopIDs> stoplist = new HashMap<>();

    StopsManager(Context context, RequestQueue queue, StopsManagerReport stopsManagerReport) {
        this.context = context;
        this.queue = queue;
        this.stopsManagerReport = stopsManagerReport;
    }

    public final static int SETUP_OK = 1, ALL_OK = 2, REQUEST_ERROR = 3, SETUP_ERROR = 4;

    public interface StopsManagerReport {
        void onReport(Integer status);
    }

    void TryLoadStops() {
        try {
            FileInputStream fs = context.openFileInput("stops");
            ObjectInputStream ss = new ObjectInputStream(fs);
            stoplist = (Map<String, StopIDs>) ss.readObject();
            ss.close();
            stopsManagerReport.onReport(SETUP_OK);
        } catch (IOException | ClassNotFoundException e) {
            DownloadStops();
        }
    }

    private void DownloadStops() {
        new StopSetup(new StopSetup.OnResultListener() {
            @Override
            public void onSuccess(Map<String, StopIDs> list) {
                stoplist = list;
                stopsManagerReport.onReport(SETUP_OK);
            }

            @Override
            public void onError(String error) {
                stopsManagerReport.onReport(SETUP_ERROR);
            }
        }, context, queue);
    }

    void get(final String name, Integer limit, final DeparturesAdapter departuresAdapter) {
        try {
            StopIDs siriids = stoplist.get(name.toLowerCase());
            if (siriids.providers() > 0) {
                final Stop stop = new Stop(queue, name, limit, context);
                stop.sources = siriids.providers();
                departuresAdapter.add(stop);
                if (siriids.isElron()) {
                    getElron(name, stop, departuresAdapter);
                }
                if (siriids.isTlt()) {
                    getTLT(siriids.getTltIDs(), stop, departuresAdapter);
                }
            } else {
                departuresAdapter.add(new Stop(name, context.getResources().getString(R.string.error_unsupported)));
            }
        } catch (Exception e) {
            departuresAdapter.add(new Stop(name, context.getResources().getString(R.string.error_unsupported)));
        }
    }

    private void getTLT(String[] siriids, final Stop stop, final DeparturesAdapter departuresAdapter) {

        String url = "https://transport.tallinn.ee/siri-stop-departures.php?stopid=" + TextUtils.join(",", siriids);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        stop.addData(response);
                        departuresAdapter.notifyDataSetChanged();
                        stopsManagerReport.onReport(ALL_OK);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        stopsManagerReport.onReport(REQUEST_ERROR);
                    }
                });
        queue.add(stringRequest);
    }

    private void getElron(final String name, final Stop stop, final DeparturesAdapter departuresAdapter) {
        JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/stop?stop="+name, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            stop.addData(response.getJSONArray("data"));
                            departuresAdapter.notifyDataSetChanged();
                            stopsManagerReport.onReport(ALL_OK);
                        } catch (Exception e) {
                            stopsManagerReport.onReport(REQUEST_ERROR);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        stopsManagerReport.onReport(REQUEST_ERROR);
                    }
                });
        queue.add(elronRequest);
    }

    void NoStopsFound(DeparturesAdapter departuresAdapter) {
        departuresAdapter.add(new Stop(context.getResources().getString(R.string.error_nostops), ""));
    }

    ArrayList<String> GetStops() {
        ArrayList<String> response = new ArrayList<>();
        for (String item : stoplist.keySet()) {
            if (item != null) {
                response.add(toUpperCase(item));
            }
        }
        Collections.sort(response, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer delta = o1.length() - o2.length();
                if (delta != 0) {
                    return delta;
                }
                return o1.compareTo(o2);
            }
        });
        return response;
    }

    private String toUpperCase(String value) {
        if (value != null) {
            if (value.length() > 1) {
                return value.substring(0, 1).toUpperCase() + value.substring(1);
            } else if (value.length() == 1) {
                return value.toUpperCase();
            }
        }
        return "";
    }
}
