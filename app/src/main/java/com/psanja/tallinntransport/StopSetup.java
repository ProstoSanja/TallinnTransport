package com.psanja.tallinntransport;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StopSetup {

    private Map<String, String[]> newstoplist = new HashMap<>();
    private OnResultListener resultListener;
    private Context context;
    private RequestQueue queue;

    StopSetup(OnResultListener resultListener, Context context, RequestQueue queue) {
        this.resultListener = resultListener;
        this.context = context;
        this.queue = queue;
        DownloadStopsTLL();
    }

    public interface OnResultListener {
        void onSuccess(Map<String, String[]> list);
        void onError(String error);
    }

    private void DownloadStopsTLL() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://transport.tallinn.ee/data/stops.txt",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            String preprocess = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            String[] rawstops = preprocess.split("\n");
                            for (String rawstop : rawstops) {
                                addBus(rawstop.split(";"));
                            }
                            DownloadStopsELR();
                        } catch (UnsupportedEncodingException e) {
                            resultListener.onError(context.getResources().getString(R.string.error_tlt_down));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        resultListener.onError(context.getResources().getString(R.string.error_tlt_parse));
                    }
                });
        queue.add(stringRequest);
    }

    private void DownloadStopsELR() {
        JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/stops", null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray stops = response.getJSONArray("data");
                            for (int i = 0; i < stops.length(); i++) {
                                String stop = stops.getJSONObject(i).getString("peatus").toLowerCase();
                                String[] tryget = newstoplist.get(stop);
                                if (tryget != null) {
                                    List<String> newids = new ArrayList<>(Arrays.asList(tryget));
                                    newids.add("-1");
                                    newstoplist.put(stop, newids.toArray(new String[0]));
                                } else {
                                    newstoplist.put(stop, new String[]{"-1"});
                                }
                            }
                            writeStops();
                        } catch (Exception e) {
                            resultListener.onError(context.getResources().getString(R.string.error_elron_parse));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        resultListener.onError(context.getResources().getString(R.string.error_elron_down));
                    }
                });
        queue.add(elronRequest);
    }

    private void writeStops() {
        resultListener.onSuccess(newstoplist);
        try {
            FileOutputStream f = context.openFileOutput("stops", Context.MODE_PRIVATE);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(newstoplist);
            s.close();
        } catch (Exception e) {
            resultListener.onError(context.getResources().getString(R.string.error_save));
        }
    }

    //this script is actually pretty reasonable, considering how much shit they have in thier source file
    private String currentName;
    private List<String> currentSiriIDs;

    private void addBus(String[] data) {
        if (data.length > 5) {
            if (currentName != null) {
                newstoplist.put(currentName, currentSiriIDs.toArray(new String[0]));
            }
            currentName = data[5].toLowerCase();
            String[] tryget = newstoplist.get(currentName);
            currentSiriIDs = new ArrayList<>();
            if (tryget != null) {
                currentSiriIDs.addAll(Arrays.asList(tryget));
            }
        }
        currentSiriIDs.add(data[1]);
    }

}
