package com.psanja.tallinntransport.Fragments;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.psanja.tallinntransport.Adapters.TravelAdapter;
import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.DATAclasses.TravelStub;
import com.psanja.tallinntransport.DATAclasses.TravelTrip;
import com.psanja.tallinntransport.Managers.StatusManager;
import com.psanja.tallinntransport.Managers.StopsManager;
import com.psanja.tallinntransport.R;
import com.psanja.tallinntransport.Utils.GsonRequest;
import com.psanja.tallinntransport.Utils.Keyboard;
import com.psanja.tallinntransport.Utils.Utils;

import java.util.Calendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class TravelFragment extends Fragment {

    private FragmentActivity context;
    private RequestQueue queue;
    private ViewGroup container;
    private TravelAdapter travelAdapter;
    private DatePickerDialog date;
    private Gson gson;

    private StopsManager stopsManager;
    private AutoCompleteTextView view_origin, view_destination;
    private Button view_date;
    private String reqdate;

    public TravelFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = requireActivity();
        queue = Volley.newRequestQueue(context);
        gson = new Gson();

        travelAdapter = new TravelAdapter(context, new TravelAdapter.OnTicketSelectedListener() {
            @Override
            public void onTicketSelected(Ticket ticket) {
                try {
                    FragmentTransaction fragmentTransaction = context.getSupportFragmentManager().beginTransaction();

                    PurchaseFragment fragment = PurchaseFragment.newInstance(ticket);
                    fragmentTransaction.replace(container.getId(), fragment);
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();

                } catch (Exception e) {
                    Utils.showError(context, e);
                }
            }
        });

        Calendar c = Calendar.getInstance();
        date = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                reqdate = String.format(Locale.US, "%04d-%02d-%02d", year, month+1, dayOfMonth);
                view_date.setText(reqdate);
            }
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        date.getDatePicker().setMinDate(c.getTimeInMillis());
        date.getDatePicker().setMaxDate(c.getTimeInMillis()+604800000);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.container = container;
        LinearLayout holder = (LinearLayout) inflater.inflate(R.layout.fragment_travel, container, false);
        ((AppCompatActivity)context).getSupportActionBar().setElevation(0);

        ((RecyclerView)holder.findViewById(R.id.travel_result)).setLayoutManager(new LinearLayoutManager(context));
        ((RecyclerView)holder.findViewById(R.id.travel_result)).setAdapter(travelAdapter);

        view_origin = holder.findViewById(R.id.travel_origin);
        view_destination = holder.findViewById(R.id.travel_destination);
        view_date = holder.findViewById(R.id.travel_date);

        stopsManager = new StopsManager(context, queue, new StatusManager(new StatusManager.OnStatusListener() {
            @Override
            public void onStatus(StatusManager.Status status) {
                if (status == StatusManager.Status.SETUP_OK) {
                    view_origin.setAdapter(new ArrayAdapter<>(context, android.R.layout.select_dialog_item, stopsManager.GetStops(true)));
                    view_destination.setAdapter(new ArrayAdapter<>(context, android.R.layout.select_dialog_item, stopsManager.GetStops(true)));
                }
            }

            @Override
            public void onMapPosition(LatLng location) { }

            @Override
            public boolean onMapPosition(Integer markerId) { return false; }
        }));
        stopsManager.TryLoadStops();

        holder.findViewById(R.id.travel_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Keyboard.hide(context, view_destination);
                fetchroutes();
            }
        });

        view_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                date.show();
            }
        });

        return holder;
    }

    private void fetchroutes() {
        travelAdapter.clear();
        String origin = stopsManager.getElronIDs(view_origin.getText().toString().toLowerCase());
        String destination = stopsManager.getElronIDs(view_destination.getText().toString().toLowerCase());
        if (origin == null || destination == null)
            return;

        TravelStub.TravelPayload payload = new TravelStub.TravelPayload(reqdate, origin, destination);
        Utils.log(origin);
        Utils.log(destination);

        GsonRequest tripsrequest = new GsonRequest<>(Request.Method.PUT, "https://api.ridango.com/v2/64/intercity/stopareas/trips/direct", TravelStub.TravelResponse.class, gson.toJson(payload),
                new Response.Listener<TravelStub.TravelResponse>() {
                    @Override
                    public void onResponse(TravelStub.TravelResponse response) {
                        for (TravelStub.TravelJourney journey : response.journeys) {
                            TravelTrip travelTrip = journey.trips.get(0);
                            travelTrip.queue = queue;
                            travelTrip.formatteddate = reqdate;
                            travelAdapter.add(travelTrip);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Utils.showError(context, error);
            }
        });
        queue.add(tripsrequest);
    }
}
