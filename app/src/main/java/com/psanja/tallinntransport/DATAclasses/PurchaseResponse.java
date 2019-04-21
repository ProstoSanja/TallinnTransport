package com.psanja.tallinntransport.DATAclasses;

import com.google.gson.annotations.SerializedName;

public class PurchaseResponse {

        @SerializedName("shopping_cart_id")
        public int cart_id;
        @SerializedName("status")
        public String status;

        PurchaseResponse() {

        }

}
