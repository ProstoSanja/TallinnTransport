package com.psanja.tallinntransport.DATAclasses;

import com.google.gson.annotations.SerializedName;
import com.psanja.tallinntransport.Utils.Utils;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Ticket implements Serializable {

    private static final long serialVersionUID = -1210957213351575837L;

    @SerializedName("id")
    public int price_id;

    @SerializedName("name")
    private String name_en;
    private String name_et;
    private String name_ru;
    @SerializedName("product_type")
    public String product_type;
    @SerializedName("product_id")
    public int product_id;
    @SerializedName("price")
    public double price;
    @SerializedName("price_vat")
    private double price_vat;
    @SerializedName("location_price")
    private double location_price;
    @SerializedName("properties")
    public List<TicketPriceProperty> properties;
    @SerializedName("translations")
    List<TicketPriceTranslation> translations;
    //set on initialization
    public String formatteddate;

    //data for purchase
    public int dep_id;
    public int origin_time;
    public int destination_time;
    public String origin_stop_name;
    public String destination_stop_name;
    public String origin_zone_name;
    public String destination_zone_name;
    public int origin_zone_id;
    public int destination_zone_id;
    public String trip_name;
    //cart id after adding
    public int cart_id;
    //qr ticket id
    public String QR;
    //status of ticket purchase
    public boolean isPurchased = false;

    public Ticket() {

    }

    public void setNames() {
        for (TicketPriceTranslation priceTranslation : translations) {
            if (priceTranslation.field.equals("name")) {
                switch (priceTranslation.lang) {
                    case "en":
                        name_en = priceTranslation.translation;
                        break;
                    case "ru":
                        name_ru = priceTranslation.translation;
                        break;
                    case "et":
                        name_et = priceTranslation.translation;
                        break;
                }
            }
        }
        translations = null;
    }

    public String getPrice() {
        return String.format("%.2f", price) + "€";
    }
    public String getLocationPrice() {
        return String.format("%.2f", location_price) + "€";
    }

    public boolean isExpired() {
        try {
            if (new SimpleDateFormat("yyyy-MM-dd").parse(formatteddate).before(new Date())) {
                return true;
            }
        } catch (Exception ignored) {
            return true;
        }
        return false;
    }

    public String getDestinationTime() {
        return minutesToTime(destination_time);
    }

    public String getOriginTime() {
        return minutesToTime(origin_time);
    }

    public String getTripTime() {
        return minutesToTime(destination_time-origin_time);
    }

    private String minutesToTime(int minutes) {
        return String.format("%02d:%02d", minutes/60, minutes%60);
    }

    public String getName() {
        return Utils.getLangMessage(name_ru, name_et, name_en);
    }

    public class TicketPriceProperty implements Serializable {

        public String code, name, description;

        TicketPriceProperty() {

        }
    }

    class TicketPriceTranslation {

        String lang, field, translation;

        TicketPriceTranslation() {

        }
    }
}
