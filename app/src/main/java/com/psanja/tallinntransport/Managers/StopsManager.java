package com.psanja.tallinntransport.Managers;

import android.content.Context;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.psanja.tallinntransport.Adapters.DeparturesAdapter;
import com.psanja.tallinntransport.DATAclasses.Stop;
import com.psanja.tallinntransport.DATAclasses.StopIDs;
import com.psanja.tallinntransport.R;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class StopsManager {

    private Context context;
    private RequestQueue queue;
    private StatusManager statusManager;

    private Map<String, StopIDs> stoplist = new HashMap<>();

    public StopsManager(Context context, RequestQueue queue, StatusManager statusManager) {
        this.context = context;
        this.queue = queue;
        this.statusManager = statusManager;
    }

    public void TryLoadStops() {
        try {
            FileInputStream fs = context.openFileInput("stops");
            ObjectInputStream ss = new ObjectInputStream(fs);
            stoplist = (Map<String, StopIDs>) ss.readObject();
            ss.close();
            statusManager.report(StatusManager.Status.SETUP_OK);
        } catch (IOException | ClassNotFoundException e) {
            DownloadStops();
        }
    }

    private void DownloadStops() {
        new StopSetup(new StopSetup.OnResultListener() {
            @Override
            public void onSuccess(Map<String, StopIDs> list) {
                stoplist = list;
                statusManager.report(StatusManager.Status.SETUP_OK);
            }

            @Override
            public void onError(String error) {
                statusManager.report(StatusManager.Status.SETUP_ERROR);
            }
        }, context, queue);
    }

    public void get(final String name, Integer limit, final DeparturesAdapter departuresAdapter) {
        try {
            StopIDs stopids = stoplist.get(name.toLowerCase());
            if (stopids.providers() > 0) {
                final Stop stop = new Stop(queue, name, limit, context);
                stop.sources = stopids.providers();
                departuresAdapter.add(stop);
                if (stopids.isElron()) {
                    getElron(name, stop, departuresAdapter);
                }
                if (stopids.isTlt()) {
                    getTLT(stopids.getTltIDs(), stop, departuresAdapter);
                }
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            departuresAdapter.add(new Stop(name, context.getResources().getString(R.string.error_unsupported)));
            DownloadStops();
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
                        statusManager.report(StatusManager.Status.DEPARTURES_OK);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        statusManager.report(StatusManager.Status.DEPARTURES_ERROR);
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
                            statusManager.report(StatusManager.Status.DEPARTURES_OK);
                        } catch (Exception e) {
                            statusManager.report(StatusManager.Status.DEPARTURES_ERROR);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        statusManager.report(StatusManager.Status.DEPARTURES_ERROR);
                    }
                });
        queue.add(elronRequest);
    }

    public String getElronIDs(String name) {
        try {
            return stoplist.get(name).getElronID();
        } catch (Exception ignored) {
            return null;
        }
    }

    public void NoStopsFound(DeparturesAdapter departuresAdapter) {
        departuresAdapter.add(new Stop(context.getResources().getString(R.string.error_nostops), ""));
    }

    public ArrayList<String> GetStops(boolean elronorall) {
        ArrayList<String> response = new ArrayList<>();
        for (String item : stoplist.keySet()) {
            if (item != null) {
                if (elronorall) {
                    StopIDs ids = stoplist.get(item);
                    if (ids.isElron()) {
                        response.add(toUpperCase(item));
                    }
                } else {
                    response.add(toUpperCase(item));
                }
            }
        }
        Collections.sort(response, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int delta = o1.length() - o2.length();
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
