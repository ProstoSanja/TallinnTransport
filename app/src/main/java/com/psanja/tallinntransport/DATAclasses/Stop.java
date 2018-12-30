package com.psanja.tallinntransport.DATAclasses;

import android.content.Context;

import com.psanja.tallinntransport.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Stop {

    public String name, status;
    private Integer limit;
    private Context context;
    public int sources = 1;
    public ArrayList<Departure> departures = new ArrayList<>();

    public Stop(String name, Integer limit, Context context) {
        this.name = name;
        this.limit = limit;
        this.context = context;
        this.status = context.getResources().getString(R.string.loading);
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
            if (sources == 1) {
                status = null;
            } else {
                sources--;
            }
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
            if (sources == 1) {
                status = context.getResources().getString(R.string.no_departures);
            } else {
                sources--;
            }
        }
    }
}