package com.psanja.tallinntransport;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

class StatusManager {

    private boolean busstatus = true, mapstatus = true, trainstatus = true;
    private TextView statusfield;
    private Context context;

    StatusManager(Context context, TextView statusfield) {
        this.context = context;
        this.statusfield = statusfield;
    }

    private void analyze() {
        Log.w("DEBUG", String.valueOf(busstatus));
        Log.w("DEBUG", String.valueOf(mapstatus));
        Log.w("DEBUG", String.valueOf(trainstatus));
        if (busstatus && mapstatus && trainstatus) {
            statusfield.setVisibility(View.GONE);
        } else if (busstatus && trainstatus) {
            statusfield.setText(context.getResources().getString(R.string.small_error));
            statusfield.setBackground(context.getResources().getDrawable(R.color.darkyellow));
            statusfield.setVisibility(View.VISIBLE);
        } else {
            statusfield.setText(context.getResources().getString(R.string.big_error));
            statusfield.setBackground(context.getResources().getDrawable(R.color.darkred));
            statusfield.setVisibility(View.VISIBLE);
        }
    }

    boolean getStatus() {
        return mapstatus;
    }

    void reportBus(boolean status) {
        if (busstatus != status) {
            busstatus = status;
            analyze();
        }
    }

    void reportMap(boolean status) {
        if (mapstatus != status) {
            mapstatus = status;
            analyze();
        }
    }

    void reportTrain(boolean status) {
        if (trainstatus != status) {
            trainstatus = status;
            analyze();
        }
    }

}
