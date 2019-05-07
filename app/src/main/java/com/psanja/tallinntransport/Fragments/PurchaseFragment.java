package com.psanja.tallinntransport.Fragments;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.psanja.tallinntransport.DATAclasses.PurchaseMethod;
import com.psanja.tallinntransport.DATAclasses.PurchaseStub;
import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.R;
import com.psanja.tallinntransport.Utils.GsonRequest;
import com.psanja.tallinntransport.Utils.Utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


public class PurchaseFragment extends Fragment implements PaymentFragment.OnPaymentMethodSelectedListener {

    private static final String PARAM1 = "ticket";

    private FirebaseAnalytics mFirebaseAnalytics;

    private FragmentActivity context;
    private RequestQueue queue;
    private Ticket ticket;
    private static Gson gson;

    //Views
    private ProgressBar view_loading;
    private EditText view_email;
    private TextView view_errorbox;
    private Button view_paybutton;

    public PurchaseFragment() {
        gson = new Gson();
    }

    public static PurchaseFragment newInstance(Ticket ticket) {
        PurchaseFragment fragment = new PurchaseFragment();
        Bundle args = new Bundle();
        args.putString(PARAM1, gson.toJson(ticket));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireActivity();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        queue = Volley.newRequestQueue(context);
        if (getArguments() != null) {
            ticket = gson.fromJson(getArguments().getString(PARAM1), Ticket.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (ticket == null)
            return null;

        View holder = inflater.inflate(R.layout.fragment_purchase, container, false);
        ((AppCompatActivity)context).getSupportActionBar().setElevation(4);

        Utils.populateTicket(holder, ticket);

        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_TO_CART, Utils.packTicket(ticket.origin_stop_name, ticket.destination_stop_name, ticket.formatteddate));

        view_email = holder.findViewById(R.id.purchase_email);
        view_email.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                startPurchasePrep();
                return false;
            }
        });

        view_loading = holder.findViewById(R.id.purchase_loading);
        view_errorbox = holder.findViewById(R.id.errorbox);
        view_paybutton = holder.findViewById(R.id.purchase_buy);
        view_paybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPurchasePrep();
            }
        });

        holder.findViewById(R.id.purchase_tc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://elron.pilet.ee/en/tingimused";
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(getResources().getColor(R.color.colorPrimary));
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(context, Uri.parse(url));
            }
        });

        return holder;
    }

    private void startPurchasePrep() {
        String email = view_email.getText().toString();
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view_email.setError(getResources().getString(R.string.error_email));
            return;
        }
        view_paybutton.setEnabled(false);
        view_email.setError(null);
        view_email.setEnabled(false);
        view_errorbox.setVisibility(View.GONE);
        view_loading.setVisibility(View.VISIBLE);
        requestTicket(email);
    }

    private void failedPurchasePrep(int id) {
        view_loading.setVisibility(View.GONE);
        view_errorbox.setText(getResources().getString(id));
        view_errorbox.setVisibility(View.VISIBLE);
        view_email.setEnabled(true);
        view_email.requestFocus();
    }

    private void requestTicket(String email) {
        PurchaseStub.PurchaseAddPayload payload = new PurchaseStub.PurchaseAddPayload(email);
        payload.card_type = "Q";
        payload.channel = "web";
        payload.region_id = 64;
        payload.email = email;
        payload.destination_stop_name = ticket.destination_stop_name;
        payload.destination_stop_time_min = ticket.destination_time;
        payload.end_zone_id = ticket.destination_zone_id;
        payload.origin_stop_name = ticket.origin_stop_name;
        payload.origin_stop_time_min = ticket.origin_time;
        payload.start_zone_id = ticket.origin_zone_id;
        payload.product_id = ticket.product_id;
        payload.starting = ticket.formatteddate;
        payload.trip_id = ticket.dep_id;
        GsonRequest tripsrequest = new GsonRequest<>(Request.Method.POST, "https://api.ridango.com/v2/64/cart/add", PurchaseStub.PurchaseAdd.class, gson.toJson(payload),
                new Response.Listener<PurchaseStub.PurchaseAdd>() {
                    @Override
                    public void onResponse(PurchaseStub.PurchaseAdd response) {
                        ticket.cart_id = response.cart_id;
                        fetchCart(response.cart_id);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failedPurchasePrep(R.string.error_internet);
            }
        });
        queue.add(tripsrequest);
    }

    private void fetchCart(final int cart_id) {
        GsonRequest tripsrequest = new GsonRequest<>(Request.Method.GET, "https://api.ridango.com/v2/64/cart/"+String.valueOf(cart_id), PurchaseStub.PurchaseCart.class, null,
                new Response.Listener<PurchaseStub.PurchaseCart>() {
                    @Override
                    public void onResponse(PurchaseStub.PurchaseCart response) {
                        if (response.isVerified(ticket.price)) {
                            ticket.QR = response.tickets.get(0).visual_card_id;
                            try {
                                Utils.storeTicket(context, ticket);
                                fetchPayments(cart_id);
                            } catch (Exception e) {
                                failedPurchasePrep(R.string.error_save);
                            }
                        } else {
                            failedPurchasePrep(R.string.error_payment_cart);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failedPurchasePrep(R.string.error_internet);
            }
        });
        queue.add(tripsrequest);
    }


    private void fetchPayments(int cart_id) {
        GsonRequest tripsrequest = new GsonRequest<>(Request.Method.GET, "https://api.ridango.com/v2/64/payment/channel/web/cart/"+String.valueOf(cart_id)+"/methods", PurchaseMethod[].class, null,
                new Response.Listener<PurchaseMethod[]>() {
                    @Override
                    public void onResponse(final PurchaseMethod[] response) {
                        view_loading.setVisibility(View.GONE);
                        final PaymentFragment pf = new PaymentFragment(response, PurchaseFragment.this);
                        view_paybutton.setEnabled(true);
                        view_paybutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                pf.show(context.getSupportFragmentManager(), "payment");
                            }
                        });
                        pf.show(context.getSupportFragmentManager(), "payment");
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                failedPurchasePrep(R.string.error_internet);
            }
        });
        queue.add(tripsrequest);
    }

    @Override
    public void onPaymentMethodSelected(PurchaseMethod purchaseMethod) {
        if (purchaseMethod == null)
            return;

        view_paybutton.setEnabled(false);

        mFirebaseAnalytics.logEvent("cart_go_to_payment", Utils.packTicket(ticket.origin_stop_name, ticket.destination_stop_name, ticket.formatteddate));

        String url = "https://thatguyalex.com/elron_beta.html?banklink_account_id="+purchaseMethod.banklink_account_id+"&payment_id="+purchaseMethod.uniq_id+"&shopping_cart_id="+ ticket.cart_id+"&lang=en";
        Utils.log(url);
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(getResources().getColor(R.color.colorPrimary));
        builder.setShowTitle(true);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        customTabsIntent.launchUrl(context, Uri.parse(url));
    }
}
