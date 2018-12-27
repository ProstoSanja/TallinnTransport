package com.psanja.tallinntransport;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.psanja.tallinntransport.DATAclasses.Departure;
import com.psanja.tallinntransport.DATAclasses.Stop;

import java.util.ArrayList;

public class DeparturesAdapter extends RecyclerView.Adapter<DeparturesAdapter.ViewHolder> {

    private StatusManager statusManager;
    private ArrayList<Stop> dataset = new ArrayList<>();

    private Context context;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        LinearLayout holderItem, stopItem, departureItem;
        TextView stopName, departureBlock, departureDestination, departureTime;
        ViewHolder(LinearLayout ll) {
            super(ll);
            holderItem = ll;
            stopItem = ll.findViewById(R.id.stop_holder);
            stopName = ll.findViewById(R.id.stop_name);
            departureItem = ll.findViewById(R.id.departure_holder);
            departureBlock = ll.findViewById(R.id.departure_block);
            departureDestination = ll.findViewById(R.id.departure_destination);
            departureTime = ll.findViewById(R.id.departure_time);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    DeparturesAdapter(Context context, StatusManager statusManager) {
        this.context = context;
        this.statusManager = statusManager;
    }

    Integer add(Stop stop) {
        dataset.add(stop);
        notifyDataSetChanged();
        return dataset.size()-1;
    }
    void set(Integer index, Stop stop) {
        dataset.set(index, stop);
        notifyDataSetChanged();
    }
    void clear() {
        dataset.clear();
        notifyDataSetChanged();
    }

    ArrayList<Stop> backup() {
        return (ArrayList<Stop>) dataset.clone();
    }
    void tryRestore(ArrayList<Stop> data) {
        if (data == null) {
            dataset.clear();
        } else {
            dataset = data;
            notifyDataSetChanged();
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public DeparturesAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Integer iteration = 0;
        while (true) {
            Stop thisstop = dataset.get(iteration);
            if (position == 0) {
                holder.departureItem.setVisibility(View.GONE);
                holder.stopName.setText(thisstop.name);
                holder.stopItem.setVisibility(View.VISIBLE);
                break;
            } else {
                position--;
                if (position < thisstop.departures.size()) {
                    Departure newdata = thisstop.departures.get(position);
                    holder.stopItem.setVisibility(View.GONE);
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
                        case "none":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.black));
                            break;
                        case "error":
                            holder.departureBlock.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                            holder.departureBlock.setBackgroundColor(context.getColor(R.color.darkred));
                            break;
                    }
                    holder.departureBlock.setText(newdata.number);
                    holder.departureDestination.setText(newdata.destination);
                    holder.departureTime.setText(newdata.arriving);
                    if (!statusManager.getStatus()) {
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
}