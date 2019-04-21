package com.psanja.tallinntransport.Adapters;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.psanja.tallinntransport.DATAclasses.Departure;
import com.psanja.tallinntransport.DATAclasses.Stop;
import com.psanja.tallinntransport.MainActivity;
import com.psanja.tallinntransport.Managers.StatusManager;
import com.psanja.tallinntransport.R;

import java.util.ArrayList;

public class DeparturesAdapter extends RecyclerView.Adapter<DeparturesAdapter.ViewHolder> implements View.OnClickListener {

    private ArrayList<Stop> dataset = new ArrayList<>();

    private FragmentActivity context;
    private StatusManager statusManager;

    public Boolean isLive = true;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        LinearLayout holderItem, stopItem, departureItem;
        TextView stopName, stopStatus, departureBlock, departureDestination, departureTime;
        Button infoButton;
        ViewHolder(LinearLayout ll) {
            super(ll);
            holderItem = ll;
            stopItem = ll.findViewById(R.id.stop_holder);
            stopName = ll.findViewById(R.id.stop_name);
            stopStatus = ll.findViewById(R.id.stop_status);
            departureItem = ll.findViewById(R.id.departure_holder);
            departureBlock = ll.findViewById(R.id.departure_block);
            departureDestination = ll.findViewById(R.id.departure_destination);
            departureTime = ll.findViewById(R.id.departure_time);
            infoButton = ll.findViewById(R.id.info_button);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DeparturesAdapter(FragmentActivity context, StatusManager statusManager) {
        this.context = context;
        this.statusManager = statusManager;
    }

    public Integer add(Stop stop) {
        dataset.add(stop);
        notifyDataSetChanged();
        return dataset.size()-1;
    }
    public void set(Integer index, Stop stop) {
        dataset.set(index, stop);
        notifyDataSetChanged();
    }
    public void clear() {
        dataset.clear();
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public DeparturesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.item_stopdeparture, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        int iteration = 0;
        while (true) {
            Stop thisstop = dataset.get(iteration);
            if (position == 0) {
                holder.departureItem.setVisibility(View.GONE);
                holder.stopName.setText(thisstop.name);
                holder.stopStatus.setText(thisstop.status);
                holder.stopItem.setVisibility(View.VISIBLE);
                break;
            } else {
                position--;
                if (position < thisstop.departures.size()) {
                    final Departure newdata = thisstop.departures.get(position);
                    holder.stopItem.setVisibility(View.GONE);
                    holder.departureBlock.setText(newdata.number);
                    switch (newdata.type) {
                        case "bus":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_bus), null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.buslight));
                            break;
                        case "tram":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_tram), null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.tramlight));
                            break;
                        case "trol":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_bus), null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.trolleylight));
                            break;
                        case "train":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(context.getDrawable(R.drawable.ic_train), null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.trainlight));
                            holder.departureBlock.setText("ELR"); //TODO: deal with it?
                            break;
                    }
                    holder.departureDestination.setText(newdata.destination);
                    holder.departureTime.setText(newdata.arriving);
                    if (newdata.addinfo) {
                        holder.infoButton.setOnClickListener(this);
                        holder.infoButton.setTag(newdata);
                        holder.infoButton.setVisibility(View.VISIBLE);
                    } else {
                        holder.infoButton.setOnClickListener(null);
                        holder.infoButton.setTag(null);
                        holder.infoButton.setVisibility(View.GONE);
                    }
                    if (!isLive) {
                        holder.departureTime.setTextColor(context.getColor(R.color.grey));
                    } else if (newdata.delay) {
                        holder.departureTime.setTextColor(context.getColor(R.color.darkred));
                    } else {
                        holder.departureTime.setTextColor(context.getColor(R.color.darkgreen));
                    }
                    holder.departureItem.setVisibility(View.VISIBLE);
                    break;
                } else {
                    position-=thisstop.departures.size();
                    iteration++;
                }
            }
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        Integer count = 0;
        for (Stop stop: dataset) {
            count++;
            count+= stop.departures.size();
        }
        return count;
    }

    @Override
    public void onClick(View v) {
        final Departure item = (Departure) v.getTag();

        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        final LayoutInflater inflater = ((MainActivity) context).getLayoutInflater();
        final View layout = inflater.inflate(R.layout.dialog_routeinfo, null);
        layout.findViewById(R.id.info_closebutton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        alertDialog.setView(layout);
        alertDialog.show();
        item.getInfo(new Departure.OnInfoLoadedListener() {
            @Override
            public void onInfoLoaded(Object result) {
                if (!alertDialog.isShowing())
                    return;
                final LinearLayout rootItem = alertDialog.findViewById(R.id.info_holder);
                rootItem.findViewById(R.id.info_loading).setVisibility(View.GONE);
                if (result.getClass() == ArrayList.class) {
                    ArrayList<Departure> parsedresult = (ArrayList<Departure>) result;
                    ((TextView)rootItem.findViewById(R.id.info_title)).setText(String.format("%s - %s", parsedresult.get(0).destination, parsedresult.get(parsedresult.size()-1).destination));
                    final LinearLayout dep_holder = rootItem.findViewById(R.id.info_item_holder);
                    Boolean isInRoute = false;
                    for (Departure dep : parsedresult) {
                        View dep_lay = inflater.inflate(R.layout.dialog_routeinfo_item, null);//layout, true);
                        ((TextView)dep_lay.findViewById(R.id.info_date)).setText(dep.arriving);
                        if (dep.delay) {
                            ((TextView)dep_lay.findViewById(R.id.info_date)).setTextColor(context.getResources().getColor(R.color.darkgreen));
                            isInRoute = true;
                        }
                        ((TextView)dep_lay.findViewById(R.id.info_dest)).setText(dep.destination);
                        dep_holder.addView(dep_lay);
                    }
                    if (isInRoute) {
                        rootItem.findViewById(R.id.info_mapbutton).setVisibility(View.VISIBLE);
                        rootItem.findViewById(R.id.info_mapbutton).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (statusManager.setmap(10000 + Integer.valueOf(item.number))) {
                                    alertDialog.dismiss();
                                } else {
                                    rootItem.findViewById(R.id.info_mapbutton).setEnabled(false);
                                }
                            }
                        });
                    }
                } else {
                    ((TextView)rootItem.findViewById(R.id.info_title)).setText(String.valueOf(result));
                }
            }
        });
    }
}