package com.psanja.tallinntransport;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.psanja.tallinntransport.DATAclasses.PurchaseResponse;
import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.Fragments.TicketFragment;
import com.psanja.tallinntransport.Utils.Utils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

public class ReceiverActivity extends AppCompatActivity {

    Ticket ticket;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onBackPressed() {
        //LUL, not this time
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiver);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        PurchaseResponse response = (new Gson()).fromJson(Utils.decodeJWT(this, getIntent().getData().getQueryParameter("result")), PurchaseResponse.class);
        try {
            ticket = Utils.getTicket(this);
        } catch (Exception e) {
            ticket = new Ticket();
            Utils.showError(this, e);
            //continued in savemismatch
        }
        //todo: remove !
        if ("Y".equals(response.status)) {
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ECOMMERCE_PURCHASE, Utils.packTicket(ticket.origin_stop_name, ticket.destination_stop_name, ticket.formatteddate));
            if (ticket.cart_id == response.cart_id) {
                ticket.isPurchased = true;
                try {
                    Utils.storeTicket(this, ticket);
                } catch (Exception e) {
                    Utils.showError(this, e);
                    showError(R.string.error_payment_mismatched);
                }
                displayTicket();
            } else {
                try {
                    Ticket tckt = new Ticket();
                    tckt.cart_id = response.cart_id;
                    tckt.QR = ticket.QR.substring(0,2) + String.valueOf(response.cart_id);
                    tckt.isPurchased = true;
                    Utils.storeTicket(this, tckt);
                    displayTicket();
                } catch (Exception e) {
                    Utils.showError(this, e);
                }
                showError(R.string.error_payment_mismatched);
            }
        } else {
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.REMOVE_FROM_CART, Utils.packTicket(ticket.origin_stop_name, ticket.destination_stop_name, ticket.formatteddate));
            showError(R.string.error_payment_cancelled);
        }

        findViewById(R.id.receiver_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });

    }

    private void displayTicket() {
        findViewById(R.id.receiver_success).setVisibility(View.VISIBLE);

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        TicketFragment fragment = new TicketFragment();
        fragmentTransaction.replace(R.id.receiver_fragment_holder, fragment);
        fragmentTransaction.commit();
    }

    private void showError(int id) {
        TextView errorview = findViewById(R.id.errorbox);
        errorview.setText(getResources().getString(id));
        errorview.setVisibility(View.VISIBLE);
    }
}
