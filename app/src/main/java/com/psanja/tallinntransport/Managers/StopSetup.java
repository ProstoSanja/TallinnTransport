package com.psanja.tallinntransport.Managers;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.psanja.tallinntransport.DATAclasses.StopIDs;
import com.psanja.tallinntransport.R;

import org.json.JSONArray;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StopSetup {

    private Map<String, StopIDs> newstoplist = new HashMap<>();
    private OnResultListener resultListener;
    private Context context;
    private RequestQueue queue;

    public StopSetup(OnResultListener resultListener, Context context, RequestQueue queue) {
        this.resultListener = resultListener;
        this.context = context;
        this.queue = queue;
        DownloadStopsTLL();
    }

    public interface OnResultListener {
        void onSuccess(Map<String, StopIDs> list);
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
        JsonArrayRequest elronRequest = new JsonArrayRequest(Request.Method.GET, "https://api.ridango.com/v2/64/intercity/originstops", null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                String stop = response.getJSONObject(i).getString("stop_name").toLowerCase();
                                StopIDs tryget = newstoplist.get(stop);
                                if (tryget == null) {
                                    tryget = new StopIDs();
                                }
                                tryget.setElronID(response.getJSONObject(i).getString("stop_area_id"));
                                newstoplist.put(stop, tryget); //todo: potentially redundant line
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
                StopIDs newid = new StopIDs();
                newid.setTltIDs(currentSiriIDs.toArray(new String[0]));
                newstoplist.put(currentName, newid);
            }
            currentName = data[5].toLowerCase();
            StopIDs tryget = newstoplist.get(currentName);
            currentSiriIDs = new ArrayList<>();
            if (tryget != null) {
                currentSiriIDs.addAll(Arrays.asList(tryget.getTltIDs()));
            }
        }
        currentSiriIDs.add(data[1]);
    }

}
