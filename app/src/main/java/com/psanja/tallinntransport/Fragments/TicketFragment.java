package com.psanja.tallinntransport.Fragments;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.psanja.tallinntransport.DATAclasses.Ticket;
import com.psanja.tallinntransport.MainActivity;
import com.psanja.tallinntransport.R;
import com.psanja.tallinntransport.TravelActivity;
import com.psanja.tallinntransport.Utils.Utils;

import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class TicketFragment extends Fragment {

    private FragmentActivity context;
    private Ticket ticket;
    private View holder;

    public TicketFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = requireActivity();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        holder = inflater.inflate(R.layout.fragment_ticket, container, false);

        try {
            ticket = Utils.getTicket(context);
        } catch (Exception e) {
            return noTicket();
        }
        if (!ticket.isPurchased)
            return noTicket();

        Utils.populateTicket(holder, ticket);
        generateQR();

        if (ticket.isExpired()) {
            holder.findViewById(R.id.ticket_expired_delete).setVisibility(View.VISIBLE);
            holder.findViewById(R.id.ticket_expired_delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.deleteTicket(context);
                    noTicket();
                }
            });
        }

        return holder;
    }

    private void generateQR() {
        try {
            BitMatrix matrix = (new QRCodeWriter()).encode(ticket.QR, BarcodeFormat.QR_CODE, 512, 512, new HashMap<EncodeHintType,Object>() {{put(EncodeHintType.MARGIN, 0);put(EncodeHintType.CHARACTER_SET, "UTF-8");}});
            Bitmap bmp = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            ((ImageView) holder.findViewById(R.id.ticket_qr)).setImageBitmap(bmp);
            holder.findViewById(R.id.ticket_qr).setVisibility(View.VISIBLE);

        } catch (Exception e) {
            Utils.showError(context, e);
        }
    }

    private View noTicket() {
        holder.findViewById(R.id.ticket_expired_delete).setVisibility(View.GONE);
        holder.findViewById(R.id.ticket_holder).setVisibility(View.GONE);
        holder.findViewById(R.id.ticket_no_holder).setVisibility(View.VISIBLE);
        holder.findViewById(R.id.ticket_no_buy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, TravelActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
        });
        return holder;
    }

}
