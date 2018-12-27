package com.psanja.tallinntransport;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.psanja.tallinntransport.DATAclasses.Departure;
import com.psanja.tallinntransport.DATAclasses.Stop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
    private StatusManager statusManager;

    private Map<String, String[]> stoplist = new HashMap<>();

    StopsManager(Context context, RequestQueue queue, StatusManager statusManager) {
        this.context = context;
        this.queue = queue;
        this.statusManager = statusManager;
        LoadStops();
    }

    private void LoadStops() {
        try {
            FileInputStream fs = context.openFileInput("stops");
            ObjectInputStream ss = new ObjectInputStream(fs);
            stoplist = (Map<String, String[]>) ss.readObject();
            ss.close();
            //delay it or dont run at all if not needed
            DownloadStops();
        } catch (Exception e) {
            DownloadStops();
        }
    }

    private void DownloadStops() {
        StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://transport.tallinn.ee/data/stops.txt",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            Map<String, String[]> newstoplist = new HashMap<>();
                            String preprocess = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            String[] rawstops = preprocess.split("\n");
                            for (String rawstop: rawstops) {
                                addBus(rawstop.split(";"), newstoplist);
                            }
                            try {
                                context.deleteFile("stops");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            FileOutputStream f = context.openFileOutput("stops", Context.MODE_PRIVATE);
                            ObjectOutputStream s = new ObjectOutputStream(f);
                            s.writeObject(newstoplist);
                            s.close();
                            stoplist = newstoplist;
                        } catch (Exception e) {
                            statusManager.reportBus(false);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        statusManager.reportBus(false);
                    }
                });
        queue.add(stringRequest);
    }



    //this script is actually pretty reasonable, considering how much shit they have in thier source file
    private String currentName;
    private List<String> currentSiriIDs;

    void addBus(String[] data, Map<String, String[]> list) {
        if (data.length > 5) {
            if (currentName != null) {
                list.put(currentName, currentSiriIDs.toArray(new String[0]));
            }
            currentName = data[5].toLowerCase();
            String[] tryget = list.get(currentName);
            currentSiriIDs = new ArrayList<>();
            if (tryget != null) {
                currentSiriIDs.addAll(Arrays.asList(tryget));
            }
        }
        currentSiriIDs.add(data[1]);
    }

    void get(String name, Integer limit, DeparturesAdapter departuresAdapter) {
        String[] siriids = stoplist.get(name.toLowerCase());
        if (siriids != null) {
            final ResponseCombiner responseCombiner = new ResponseCombiner(siriids.length, name, limit, departuresAdapter);
            for (String stop : siriids) {
                String url = "https://transport.tallinn.ee/siri-stop-departures.php?stopid=" + stop;
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                responseCombiner.process(response);
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        statusManager.reportBus(false);
                    }
                });
                queue.add(stringRequest);
            }
        } else {
            Stop stop = new Stop(name);
            stop.departures.add(new Departure(Departure.NOTFOUND));
            departuresAdapter.add(stop);
        }
    }

    private class ResponseCombiner {

        Integer required;
        Integer position;
        Integer limit;
        Stop stop;
        Boolean error;
        DeparturesAdapter departuresAdapter;

        ResponseCombiner(Integer responses, String name, Integer limit, DeparturesAdapter departuresAdapter) {
            required = responses;
            this.limit = limit;
            stop = new Stop(name);
            this.departuresAdapter = departuresAdapter;
            position = this.departuresAdapter.add(stop);
        }

        void process(String response) {
            required--;
            String[] responses = response.split("\n");
            if (responses.length >= 3) {
                if (!responses[0].contains("ERROR:")) {
                    responses[0] = null;
                    responses[1] = null;
                    for (String departure : responses) {
                        if (departure != null) {
                            stop.departures.add(new Departure(departure));
                        }
                    }
                } else {
                    error = true;
                }
            }

            if (required <= 0) {
                if (error != null) {
                    statusManager.reportBus(false);
                } else {
                    statusManager.reportBus(true);
                    if (stop.departures.size() <= 0) {
                        stop.departures.add(new Departure(Departure.NODEPARTURE));
                    } else {
                        Collections.sort(stop.departures, new Comparator<Departure>() {
                            @Override
                            public int compare(Departure o1, Departure o2) {
                                return o1.arrivingseconds.compareTo(o2.arrivingseconds);
                            }
                        });
                        if (stop.departures.size() > limit) {
                            stop.departures = new ArrayList<>(stop.departures.subList(0, limit));
                        }
                    }
                    departuresAdapter.set(position, stop);
                }
            }
        }
    }

}
