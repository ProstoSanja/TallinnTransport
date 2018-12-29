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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
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
            new stopSetup(null);
        } catch (Exception e) {
            new stopSetup(null);
        }
    }

    public class stopSetup {

        Map<String, String[]> newstoplist = new HashMap<>();
        Void callback;

        stopSetup(Void callback) {
            this.callback = callback;
            DownloadStopsTLL();
        }

        private void DownloadStopsTLL() {
            StringRequest stringRequest = new StringRequest(Request.Method.GET, "https://transport.tallinn.ee/data/stops.txt",
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            String preprocess = null;
                            try {
                                preprocess = new String(response.getBytes("ISO-8859-1"), "UTF-8");
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                            String[] rawstops = preprocess.split("\n");
                            for (String rawstop : rawstops) {
                                addBus(rawstop.split(";"));
                            }
                            DownloadStopsELR();
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

        private void DownloadStopsELR() {
            JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/stops", null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            statusManager.reportTrain(true);
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
                            } catch (Exception e) {
                                statusManager.reportTrain(false);
                            }
                            writeStops();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            statusManager.reportTrain(false);
                        }
                    });
            queue.add(elronRequest);
        }

        private void writeStops() {
            try {
                FileOutputStream f = context.openFileOutput("stops", Context.MODE_PRIVATE);
                ObjectOutputStream s = new ObjectOutputStream(f);
                s.writeObject(newstoplist);
                s.close();
                stoplist = newstoplist;
                //TODO: call callback if not null
            } catch (Exception e) {
                statusManager.reportBus(false);
            }
        }

        //this script is actually pretty reasonable, considering how much shit they have in thier source file
        private String currentName;
        private List<String> currentSiriIDs;

        void addBus(String[] data) {
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

    void get(final String name, Integer limit, final DeparturesAdapter departuresAdapter) {
        String[] siriids = stoplist.get(name.toLowerCase());
        if (siriids != null) {
            final Stop stop = new Stop(name, limit);
            departuresAdapter.add(stop);
            if (Arrays.asList(siriids).contains("-1")) {
                //TODO: pass data about double loading to stop, so no false "no departures"
                //TODO: potentially clean -1
                //TODO: if minus one is the only one, then pass info about one load ..   simple enough... var will be named sources
                JsonObjectRequest elronRequest = new JsonObjectRequest(Request.Method.GET, "https://elron.ee/api/v1/stop?stop="+name, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                statusManager.reportTrain(true);
                                try {
                                    stop.addData(response.getJSONArray("data"));
                                    departuresAdapter.notifyDataSetChanged();
                                } catch (Exception e) {
                                    statusManager.reportTrain(false);
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                statusManager.reportTrain(false);
                            }
                        });
                queue.add(elronRequest);
            }
            //final ResponseCombiner responseCombiner = new ResponseCombiner(siriids.length, name, limit, departuresAdapter);
            String url = "https://transport.tallinn.ee/siri-stop-departures.php?stopid=" + TextUtils.join(",", siriids);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            stop.addData(response);
                            departuresAdapter.notifyDataSetChanged();
                            //potentially update stop
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            departuresAdapter.add(new Stop(name, "Connection Error"));
                            statusManager.reportBus(false);
                        }
                    });
            queue.add(stringRequest);
        } else {
            departuresAdapter.add(new Stop(name, "Not TLT or Elron stop"));
        }
    }


    ArrayList<String> searchStop(String search) {
        search = search.toLowerCase().trim();
        ArrayList<String> response = new ArrayList<>();
        for (String result: stoplist.keySet()) {
            if (result.startsWith(search)) {
                response.add(toUpperCase(result));
            }
        }
        Collections.sort(response, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }
        });
        return response;
    }

    String toUpperCase(String value) {
        if (value != null) {
            if (value.length() > 1) {
                return value.substring(0, 1).toUpperCase() + value.substring(1);
            } else if (value.length() == 1) {
                return value.toUpperCase();
            }
        }
        return null;
    }
/*
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
*/
}
