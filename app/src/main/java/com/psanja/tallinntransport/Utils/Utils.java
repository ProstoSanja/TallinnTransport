package com.psanja.tallinntransport.Utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.R;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Utils {

    public static void showError(Context context, Exception e) {
        e.printStackTrace();
        Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
    }

    public static void log(Object log) {
        Log.w("DEBUG", String.valueOf(log));
    }

    public static String decodeJWT(Context context, String strEncoded) {
        Utils.log(strEncoded);
        try {
            storeTokenEmergency(context, strEncoded);
        } catch (Exception ignore) {}
        byte[] decodedBytes = Base64.decode(strEncoded.split("\\.")[1], Base64.URL_SAFE);
        try {
            return new String(decodedBytes, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public static void storeTokenEmergency(Context context, String token) throws Exception {
        FileOutputStream f = context.openFileOutput("tokenEmergency", Context.MODE_PRIVATE);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(token);
        s.close();
    }

    public static void storeTicket(Context context, Ticket ticket) throws Exception {
        FileOutputStream f = context.openFileOutput("ticket", Context.MODE_PRIVATE);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(ticket);
        s.close();
    }
    public static void deleteTicket(Context context) {
        context.deleteFile("ticket");
    }
    public static Ticket getTicket(Context context) throws Exception {
        FileInputStream fs = context.openFileInput("ticket");
        ObjectInputStream ss = new ObjectInputStream(fs);
        Ticket ticket = (Ticket) ss.readObject();
        ss.close();
        return ticket;
    }

    public static void populateTicket(View holder, Ticket ticket) {
        ((TextView) holder.findViewById(R.id.ticket_title)).setText(ticket.name_en);
        ((TextView) holder.findViewById(R.id.ticket_price)).setText(ticket.getPrice());
        ((TextView) holder.findViewById(R.id.ticket_departure_time)).setText(ticket.getOriginTime());
        ((TextView) holder.findViewById(R.id.ticket_destination_time)).setText(ticket.getDestinationTime());
        ((TextView) holder.findViewById(R.id.ticket_departure_stop)).setText(ticket.origin_stop_name);
        ((TextView) holder.findViewById(R.id.ticket_destination_stop)).setText(ticket.destination_stop_name);
        ((TextView) holder.findViewById(R.id.ticket_departure_zone)).setText(ticket.origin_zone_name);
        ((TextView) holder.findViewById(R.id.ticket_destination_zone)).setText(ticket.destination_zone_name);
        ((TextView) holder.findViewById(R.id.ticket_tripdate)).setText(ticket.formatteddate);
        ((TextView) holder.findViewById(R.id.ticket_triptime)).setText("Route time "+ ticket.getTripTime());
    }

}
