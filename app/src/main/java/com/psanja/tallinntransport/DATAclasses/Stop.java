package com.psanja.tallinntransport.DATAclasses;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Stop {

    public String name, status = "Loading";
    private Integer limit;
    public ArrayList<Departure> departures = new ArrayList<>();

    public Stop(String name, Integer limit) {
        this.name = name;
        this.limit = limit;
    }
    public Stop(String name, String status) {
        this.name = name;
        this.status = status;
    }

    //TLT
    public void addData(String data) {
        String[] responses = data.split("\n");
        responses[0] = null;
        for (String departure : responses) {
            if (departure != null && !departure.contains("ERROR") && !departure.contains("stop,")) {
                departures.add(new Departure(departure));
            }
        }
        CheckSort();
    }

    //ELR
    public void addData(JSONArray data) {
        try {
            for (int i = 0; i < data.length(); i++) {
                JSONObject dep = data.getJSONObject(i);
                if (dep.getString("tegelik_aeg").isEmpty()) {
                    Departure newitem = new Departure(dep.getString("liin"), name, dep.getString("plaaniline_aeg"));
                    if (!newitem.deleteMe) {
                        departures.add(newitem);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CheckSort();
    }

    private void CheckSort() {
        if (departures.size() > 0) {
            status = null;
            Collections.sort(departures, new Comparator<Departure>() {
                @Override
                public int compare(Departure o1, Departure o2) {
                    return o1.arrivingseconds.compareTo(o2.arrivingseconds);
                }
            });
            if (departures.size() > limit) {
                departures = new ArrayList<>(departures.subList(0, limit));
            }
        } else {
            status = "No Departures";
        }
    }
}