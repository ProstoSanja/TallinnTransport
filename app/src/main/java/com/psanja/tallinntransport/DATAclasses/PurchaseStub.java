package com.psanja.tallinntransport.DATAclasses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PurchaseStub {

    public static class PurchaseCart {


        @SerializedName("id")
        public int uniq_id;
        @SerializedName("user_id")
        public int user_id;
        @SerializedName("custoner_id")
        public int custoner_id;
        @SerializedName("total")
        public double total;
        @SerializedName("total_sum")
        public double total_sum;
        @SerializedName("external_code")
        public String external_code;
        @SerializedName("jwt_token")
        public String jwt_token;
        @SerializedName("tickets")
        public List<PurchaseCartTicket> tickets;

        PurchaseCart() {

        }

        public boolean isVerified(double checkprice) {
            if (tickets.size() != 1)
                return false;
            PurchaseCartTicket ourticket = tickets.get(0);
            if (total != total_sum)
                return false;
            if (!ourticket.card_id.equals(ourticket.visual_card_id))
                return false;
            if (ourticket.price != total)
                return false;
            if (checkprice != total)
                return false;

            return true;
        }

    }

    public static class PurchaseCartTicket {

        @SerializedName("product_id")
        public int product_id;
        @SerializedName("card_id")
        public String card_id;
        @SerializedName("visual_card_id")
        public String visual_card_id;
        @SerializedName("price")
        public double price;
        @SerializedName("start_zone_name")
        public String start_zone_name;
        @SerializedName("end_zone_name")
        public String end_zone_name;
        @SerializedName("issuer_name")
        public String issuer_name;
        @SerializedName("issuer_address")
        public String issuer_address;
        @SerializedName("issuer_regnr")
        public String issuer_regnr;
        @SerializedName("issuer_vatnr")
        public String issuer_vatnr;

        PurchaseCartTicket() {

        }

    }


    public static class PurchaseAddPayload {

        public String card_type = "Q";
        public String channel = "web";
        public int region_id = 64;
        public String email;
        public String destination_stop_name;
        public int destination_stop_time_min;
        public int end_zone_id ;
        public String origin_stop_name;
        public int origin_stop_time_min;
        public int start_zone_id;
        public int product_id;
        public String starting;
        public int trip_id;

        public PurchaseAddPayload(String email) {
            this.email = email;
        }

    }

    public static class PurchaseAdd {

        @SerializedName("shopping_cart_id")
        public int cart_id;

        PurchaseAdd() {

        }

    }

}
