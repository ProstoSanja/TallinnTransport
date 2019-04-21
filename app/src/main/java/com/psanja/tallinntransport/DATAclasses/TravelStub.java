package com.psanja.tallinntransport.DATAclasses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TravelStub {

    public static class TravelResponse {

        @SerializedName("journeys")
        public List<TravelJourney> journeys;

        public TravelResponse() {

        }

    }

    public static class TravelJourney {

        @SerializedName("journey_name")
        public String journey_name;
        @SerializedName("trips")
        public List<TravelTrip> trips;

        public TravelJourney() {

        }

    }

    public static class TravelPayload {

        @SerializedName("date")
        public String formatteddate;
        @SerializedName("origin_stop_area_id")
        public String origin_stop_id;
        @SerializedName("destination_stop_area_id")
        public String destination_stop_id;
        @SerializedName("channel")
        public String channel = "web";

        public TravelPayload(String formatteddate, String origin_stop_id, String destination_stop_id) {
            this.formatteddate = formatteddate;
            this.origin_stop_id = origin_stop_id;
            this.destination_stop_id = destination_stop_id;
        }

    }

    public static class TravelTripPayload {

        @SerializedName("date")
        public String formatteddate;
        @SerializedName("trip_id")
        public int trip_id;
        @SerializedName("origin_zone")
        public int origin_zone;
        @SerializedName("destination_zone")
        public int destination_zone;
        @SerializedName("channel")
        public String channel = "web";

        public TravelTripPayload(String formatteddate, int trip_id, int origin_zone, int destination_zone) {
            this.formatteddate = formatteddate;
            this.trip_id = trip_id;
            this.origin_zone = origin_zone;
            this.destination_zone = destination_zone;
        }
    }

}
