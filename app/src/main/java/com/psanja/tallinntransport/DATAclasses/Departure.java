package com.psanja.tallinntransport.DATAclasses;


import android.annotation.SuppressLint;

public class Departure {
    public String type, number, arriving, destination;
    public Boolean delay = false;
    public Integer arrivingseconds;


    public static final int ERROR = 1;
    public static final int NODEPARTURE = 2;
    public static final int NOTFOUND = 3;

    public Departure(String raw) {
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

    public Departure (Integer action) {
        delay = true;
        destination = "";
        switch (action) {
            case ERROR:
                type = "error";
                number = "Tallinn government server reported error";
                arriving = "";
                break;
            case NODEPARTURE:
                type = "none";
                number = "No Departures";
                arriving = "--:--";
                break;
            case NOTFOUND:
                type = "none";
                number = "Stop not found";
                arriving = "";
                break;
        }
    }

    @SuppressLint("DefaultLocale")
    private String convertTime(Integer temptime) {
        Integer hours = temptime / 3600;
        Integer minutes = (temptime % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}