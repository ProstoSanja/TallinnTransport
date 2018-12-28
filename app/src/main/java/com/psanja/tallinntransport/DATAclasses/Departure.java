package com.psanja.tallinntransport.DATAclasses;


import android.annotation.SuppressLint;

public class Departure {
    public String type, number, arriving, destination;
    public Boolean delay = false;
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

    Departure(String line, String time) {
        type = "train";
        number = "ELR";
        //TODO: MAYBE: maybe check if time has passed before, because they dont put done mark sometimes
        //TODO: SURE: also check if time has passed but we are still here, then it is delayed, so we add check mark
        arrivingseconds = convertSeconds(time);
        arriving = time;
        destination = line.split("-")[1].trim();
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