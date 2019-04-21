package com.psanja.tallinntransport.Fragments;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.psanja.tallinntransport.DATAclasses.PurchaseMethod;
import com.psanja.tallinntransport.R;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;


public class PaymentFragment extends BottomSheetDialogFragment {

    private boolean startedcorrectly;
    private OnPaymentMethodSelectedListener listener;
    private PurchaseMethod[] methods;

    public PaymentFragment() {
        startedcorrectly = false;
    }

    @SuppressLint("ValidFragment")
    public PaymentFragment(PurchaseMethod[] methods, OnPaymentMethodSelectedListener listener) {
        startedcorrectly = true;
        this.methods = methods;
        this.listener = listener;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (!startedcorrectly)
            return null;

        LinearLayout holder = new LinearLayout(getContext());
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setPadding(20, 0 ,20, 20);
        //holder.setBackgroundColor(getContext().getColor(R.color.));
        for (final PurchaseMethod method : methods) {
            Button mb =  new Button(new ContextThemeWrapper(getActivity(), R.style.AppTheme_Button), null, 0);
            mb.setText(method.name);
            //mb.setPadding(20,20,20,20);
            mb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onPaymentMethodSelected(method);
                    PaymentFragment.this.dismiss();
                }
            });
            holder.addView(mb);
        }
        return holder;
    }

    public interface OnPaymentMethodSelectedListener {
        void onPaymentMethodSelected(PurchaseMethod purchaseMethod);
    }
}
