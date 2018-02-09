package com.psanja.tallinntransport.DATAclasses;

import java.util.ArrayList;

public class Stop {

    public String name;
    public ArrayList<Departure> departures = new ArrayList<>();

    public Stop(String name) {
        this.name = name;
    }
}