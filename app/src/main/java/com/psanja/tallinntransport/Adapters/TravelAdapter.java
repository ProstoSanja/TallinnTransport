package com.psanja.tallinntransport.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;
import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.DATAclasses.TravelTrip;
import com.psanja.tallinntransport.R;
import com.psanja.tallinntransport.Utils.Utils;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

public class TravelAdapter extends RecyclerView.Adapter<TravelAdapter.ViewHolder> implements View.OnClickListener {

    private ArrayList<TravelTrip> dataset = new ArrayList<>();
    private FragmentActivity context;
    private OnTicketSelectedListener ticketSelectedListener;

    static class ViewHolder extends RecyclerView.ViewHolder {
        int position;
        MaterialCardView holderItem;
        LinearLayout expand_area, ticket_holder;
        TextView title, time, express, price, price_orig, warning;
        ProgressBar expand_loading;
        ViewHolder(MaterialCardView ll) {
            super(ll);
            holderItem = ll;
            expand_area = ll.findViewById(R.id.card_travel_expanded_area);
            title = ll.findViewById(R.id.card_travel_title);
            time = ll.findViewById(R.id.card_travel_time);
            express = ll.findViewById(R.id.card_travel_express);
            price = ll.findViewById(R.id.card_travel_price);
            price_orig = ll.findViewById(R.id.card_travel_price_orig);
            warning = ll.findViewById(R.id.card_travel_warning);
            expand_loading = ll.findViewById(R.id.card_travel_expanded_loading);
            ticket_holder = ll.findViewById(R.id.card_travel_expanded_tickets_holder);
        }
    }

    public TravelAdapter(FragmentActivity context, OnTicketSelectedListener ticketSelectedListener) {
        this.context = context;
        this.ticketSelectedListener = ticketSelectedListener;
    }

    public void add(TravelTrip travelTrip) {
        dataset.add(travelTrip);
        notifyDataSetChanged();
    }
    public void clear() {
        dataset.clear();
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public TravelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MaterialCardView v = (MaterialCardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.card_travel, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TravelTrip thisTicket = dataset.get(position);
        holder.position = position;
        holder.title.setText(thisTicket.trip_name);
        holder.time.setText(thisTicket.getTimestamps());
        if (thisTicket.route_class.equals("E")) {
            holder.express.setVisibility(View.VISIBLE);
        } else {
            holder.express.setVisibility(View.GONE);
        }
        holder.price.setText(thisTicket.quick_product.getPrice());
        holder.price_orig.setText(String.format(context.getResources().getString(R.string.price_train), thisTicket.quick_product.getLocationPrice()));
        holder.expand_area.setVisibility(View.GONE);
        if (thisTicket.warning.size() > 0) {
            holder.warning.setText(thisTicket.warning.get(0).getMessage());
            holder.warning.setVisibility(View.VISIBLE);
        } else {
            holder.warning.setText(null);
            holder.warning.setVisibility(View.GONE);
        }
        holder.holderItem.setTag(holder);
        holder.expand_loading.setVisibility(View.GONE);
        holder.ticket_holder.removeAllViews();
        holder.holderItem.setOnClickListener(this);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return dataset.size();
    }

    @Override
    public void onClick(View v) {
        final ViewHolder holder = (ViewHolder) v.getTag();
        final TravelTrip thisTrip = dataset.get(holder.position);
        if (!thisTrip.isLoading) {
            //todo if visible
            holder.expand_loading.setVisibility(View.VISIBLE);
            holder.ticket_holder.removeAllViews();
            thisTrip.getTickets(new TravelTrip.OnTicketsLoadedListener() {
                @Override
                public void onTicketsLoaded() {
                    LayoutInflater inflater = context.getLayoutInflater();

                    holder.expand_loading.setVisibility(View.GONE);

                    for (final Ticket ticket : thisTrip.priceList) {

                        View ticketview = inflater.inflate(R.layout.card_travel_priceitem, holder.expand_area, false);
                        ((TextView) ticketview.findViewById(R.id.card_travel_priceitem_name)).setText(ticket.getName());

                        Button ticketbutton = ticketview.findViewById(R.id.card_travel_priceitem_button);

                        for (Ticket.TicketPriceProperty property : ticket.properties) {
                            if (property.code.equals("FIXED_SEATS")) {
                                 ticketbutton.setEnabled(false);
                            }
                        }
                        ticketbutton.setText(ticket.getPrice());
                        ticketbutton.setTag(ticket);
                        ticketbutton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ticket.dep_id = thisTrip.dep_id;
                                ticket.origin_time = thisTrip.origin_time;
                                ticket.destination_time = thisTrip.destination_time;
                                ticket.origin_stop_name = thisTrip.origin_stop_name;
                                ticket.destination_stop_name = thisTrip.destination_stop_name;
                                ticket.origin_zone_name = thisTrip.origin_zone_name;
                                ticket.destination_zone_name = thisTrip.destination_zone_name;
                                ticket.origin_zone_id = thisTrip.origin_zone_id;
                                ticket.destination_zone_id = thisTrip.destination_zone_id;
                                ticket.trip_name = thisTrip.trip_name;
                                ticketSelectedListener.onTicketSelected(ticket);
                            }
                        });

                        holder.ticket_holder.addView(ticketview);
                        holder.expand_area.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onError(Exception e) {
                    holder.expand_loading.setVisibility(View.GONE);
                    Utils.showError(context, e); //todo???
                }
            });
        }
    }

    public interface OnTicketSelectedListener {
        void onTicketSelected(Ticket ticket);
    }
}