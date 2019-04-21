package com.psanja.tallinntransport.DATAclasses;

import com.google.gson.annotations.SerializedName;

public class PurchaseMethod {

    @SerializedName("payment_id")
    public int uniq_id;
    @SerializedName("name")
    public String name;
    @SerializedName("code")
    public String code;
    @SerializedName("banklink_account_id")
    public int banklink_account_id;
    @SerializedName("payment_type")
    public String payment_type;
    @SerializedName("fee")
    public double fee;
    @SerializedName("fee_vat")
    public double fee_vat;

    PurchaseMethod() {

    }
}
