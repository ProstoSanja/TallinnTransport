package com.psanja.tallinntransport.DATAclasses;


import android.annotation.SuppressLint;
import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.psanja.tallinntransport.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class Departure {
    public Boolean deleteMe = false;
    public String type, number, arriving, destination;
    public Boolean delay = false, addinfo = false;
    private RequestQueue queue;
    private Context context;
    public Integer arrivingseconds;

    Departure(String raw) {
        String[] formatted = raw.split(",");
        if (formatted.length >= 5) {
            type = formatted[0];
            number = formatted[1];
            arrivingseconds = Integer.parseInt(formatted[2]);
            arriving = convertTime(arrivingseconds);
            Integer ScheduleTimeInSeconds = Integer.parseInt(formatted[3]);
            if (arrivingseconds - ScheduleTimeInSeconds >= 60) {
                delay = true;
            }
            destination = formatted[4];
        }
    }

    Departure(Context context, RequestQueue queue, String destination, String number, String time) {
        this.context = context;
        this.queue = queue;
        type = "train";
        //this.number = "ELR";
        this.number = number;
        addinfo = true;
        arrivingseconds = convertSeconds(time);
        //TODO: THIS WILL LAST ONLY UNTIL SPRING DEAL WITH DST AND TIMEZONES
        long delaydelt = (((System.currentTimeMillis()/1000)+7200)%86400) - arrivingseconds;
        if (delaydelt > 600) { //delay 10 min
            deleteMe = true;
            return;
        } else if (delaydelt > 30){
            delay = true;
        }
        //this.addinfo = "This is a train number " + number + " and we can request its route through Elron V1";
        this.destination = destination;
        arriving = time;
    }

    private Departure(String arriving, String destination, Boolean delay) {
        this.arriving = arriving;
        this.destination = destination;
        this.delay = delay;
    }

    public void getInfo(final OnInfoLoadedListener listener) {
        if (!addinfo)
            return;
        String url = "https://elron.ee/api/v1/trip?id=" + number;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url,null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ArrayList<Departure> result = new ArrayList<>();
                            JSONArray stops = response.getJSONArray("data");
                            for (int i=0; i < stops.length(); i++) {
                                JSONObject stop = stops.getJSONObject(i);
                                if (!stop.getString("tegelik_aeg").isEmpty()) {
                                    result.add(new Departure(stop.getString("tegelik_aeg"), stop.getString("peatus"), true));
                                } else {
                                    result.add(new Departure(stop.getString("plaaniline_aeg"), stop.getString("peatus"), false));
                                }
                            }
                            listener.onInfoLoaded(result);
                        } catch (Exception e) {
                            listener.onInfoLoaded(context.getResources().getString(R.string.error_elron_parse));
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onInfoLoaded(context.getResources().getString(R.string.error_elron_down));
            }
        });
        queue.add(jsObjRequest);
    }

    public interface OnInfoLoadedListener {
        void onInfoLoaded(Object result);
    }

    @SuppressLint("DefaultLocale")
    private String convertTime(Integer temptime) {
        Integer hours = temptime / 3600;
        Integer minutes = (temptime % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private Integer convertSeconds(String time) {
        String[] times = time.split(":");
        Integer result = Integer.valueOf(times[0])*3600;
        result += Integer.valueOf(times[1])*60;
        return result;
    }
}