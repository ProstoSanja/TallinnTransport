package com.psanja.tallinntransport.DATAclasses;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.psanja.tallinntransport.Utils.GsonRequest;
import com.psanja.tallinntransport.Utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TravelTrip {

    @SerializedName("id")
    public int dep_id;
    @SerializedName("departure_time_min")
    public int origin_time;
    @SerializedName("arrival_time_min")
    public int destination_time;
    @SerializedName("origin_stop_name")
    public String origin_stop_name;
    @SerializedName("destination_stop_name")
    public String destination_stop_name;
    @SerializedName("origin_zone_name")
    public String origin_zone_name;
    @SerializedName("destination_zone_name")
    public String destination_zone_name;
    @SerializedName("route_id")
    public int route_id;
    @SerializedName("service_id")
    public int service_id;
    @SerializedName("origin_zone")
    public int origin_zone_id;
    @SerializedName("destination_zone")
    public int destination_zone_id;
    @SerializedName("trip_short_name")
    public String trip_name;
    @SerializedName("ext_trip_id")
    public String day_id;
    @SerializedName("product")
    public Ticket quick_product;
    @SerializedName("trip_messages")
    public List<TravelTripMessage> warning;
    @SerializedName("route_class")
    public String route_class;
    @SerializedName("bikes_allowed")
    public String bikes_allowed;

    public String formatteddate;
    public RequestQueue queue;

    public ArrayList<Ticket> priceList = new ArrayList<>();

    public boolean isLoading = false;

    TravelTrip() {

    }

    public String getTimestamps() {
        return getOriginTime() + " - " + getDestinationTime();
    }

    public String getDestinationTime() {
        return minutesToTime(destination_time);
    }

    public String getOriginTime() {
        return minutesToTime(origin_time);
    }

    private String minutesToTime(int minutes) {
        return String.format("%02d:%02d", minutes/60, minutes%60);
    }



    public interface OnTicketsLoadedListener {
        void onTicketsLoaded();
        void onError(Exception e);
    }

    public void getTickets(final OnTicketsLoadedListener listener) {
        if (priceList.size()>0) {
            listener.onTicketsLoaded();
            return;
        }
        isLoading = true;
        TravelStub.TravelTripPayload payload = new TravelStub.TravelTripPayload(formatteddate, dep_id, origin_zone_id, destination_zone_id);
        GsonRequest ticketsrequest = new GsonRequest<>(Request.Method.PUT, "https://api.ridango.com/v2/64/intercity/trip_products", Ticket[].class, (new Gson()).toJson(payload),
                new Response.Listener<Ticket[]>() {
                    @Override
                    public void onResponse(Ticket[] response) {
                        for (Ticket price : response) {
                            if (price.product_type.equals("D")) {
                                price.setNames();
                                price.formatteddate = formatteddate;
                                priceList.add(price);
                            }
                        }
                        listener.onTicketsLoaded();
                        isLoading = false;
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onError(error);
                isLoading = false;
            }
        });
        queue.add(ticketsrequest);
    }



    public class TravelTripMessage {

        public String message_en, message_ru, message_et;

        TravelTripMessage() {

        }
        public String getMessage() {
            return Utils.getLangMessage(message_ru, message_et, message_en);
        }
    }

}
