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

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

class StopsManager {

    private Context context;
    private RequestQueue queue;
    private StopsManagerReport stopsManagerReport;

    private Map<String, String[]> stoplist = new HashMap<>();

    StopsManager(Context context, RequestQueue queue, StopsManagerReport stopsManagerReport) {
        this.context = context;
        this.queue = queue;
        this.stopsManagerReport = stopsManagerReport;
    }

    public final static int SETUP_OK = 1, ALL_OK = 2, REQUEST_ERROR = 3, SETUP_ERROR = 4;

    public interface StopsManagerReport {
        void onReport(Integer status);
    }

    public void TryLoadStops() {
        try {
            FileInputStream fs = context.openFileInput("stops");
            ObjectInputStream ss = new ObjectInputStream(fs);
            stoplist = (Map<String, String[]>) ss.readObject();
            ss.close();
            stopsManagerReport.onReport(SETUP_OK);
        } catch (IOException | ClassNotFoundException e) {
            DownloadStops();
        }
    }

    private void DownloadStops() {
        new StopSetup(new StopSetup.OnResultListener() {
            @Override
            public void onSuccess(Map<String, String[]> list) {
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
        String[] siriids = stoplist.get(name.toLowerCase());
        if (siriids != null) {
            final Stop stop = new Stop(name, limit, context);
            departuresAdapter.add(stop);
            if (Arrays.asList(siriids).contains("-1")) {
                Boolean isElronOnly = siriids.length == 1;
                //TODO: potentially clean -1
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
                if (!isElronOnly) {
                    stop.sources++;
                    queue.add(elronRequest);
                } else  {
                    queue.add(elronRequest);
                    return;
                }
            }

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
        } else {
            departuresAdapter.add(new Stop(name, context.getResources().getString(R.string.error_unsupported)));
        }
    }

    //TODO: implement uppercase
    ArrayList<String> GetStops() {
        ArrayList<String> response = new ArrayList<>(stoplist.keySet());
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
        return null;
    }
}
