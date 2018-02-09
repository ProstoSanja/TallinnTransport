package com.psanja.tallinntransport;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.psanja.tallinntransport.DATAclasses.Departure;
import com.psanja.tallinntransport.DATAclasses.Stop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StopsManager {

    private RequestQueue queue;
    private DeparturesAdapter recyclerAdapter;

    private Map<String, String[]> stoplist = new HashMap<>();

    private String currentName;
    private List<String> currentSiriIDs;

    StopsManager(RequestQueue queue, DeparturesAdapter recyclerAdapter) {
        this.queue = queue;
        this.recyclerAdapter = recyclerAdapter;
    }

    void get(String name) {
        String[] siriids = stoplist.get(name.toLowerCase());
        if (siriids != null) {
            final ResponseCombiner responseCombiner = new ResponseCombiner(siriids.length, name);
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
                        responseCombiner.process("ERROR: Timed out\nERROR: Timed out\nERROR: Timed out");
                    }
                });
                queue.add(stringRequest);
            }
        }
    }

    private class ResponseCombiner {

        Integer required;
        Integer position;
        Stop stop;
        Boolean error;

        ResponseCombiner(Integer responses, String name) {
            required = responses;
            stop = new Stop(name);
            position = recyclerAdapter.add(stop);
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
                    stop.departures.add(new Departure(true));
                } else if (stop.departures.size() <= 0) {
                    stop.departures.add(new Departure(false));
                } else {
                    Collections.sort(stop.departures, new Comparator<Departure>() {
                        @Override
                        public int compare(Departure o1, Departure o2) {
                            return o1.arrivingseconds.compareTo(o2.arrivingseconds);
                        }
                    });
                    if (stop.departures.size() > 20) {
                        stop.departures = new ArrayList<>(stop.departures.subList(0, 15));
                    }
                }
                recyclerAdapter.set(position, stop);
            }
        }
    }

    //refactor this shit

    void add(String[] data) {
        if (data.length > 5) {
            if (currentName != null) {
                stoplist.put(currentName, currentSiriIDs.toArray(new String[0]));
            }
            currentName = data[5].toLowerCase();
            String[] tryget = stoplist.get(currentName);
            currentSiriIDs = new ArrayList<>();
            if (tryget != null) {
                currentSiriIDs.addAll(Arrays.asList(tryget));
            }
        }
        currentSiriIDs.add(data[1]);
    }
}
