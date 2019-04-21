package com.psanja.tallinntransport;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.psanja.tallinntransport.Fragments.PurchaseFragment;
import com.psanja.tallinntransport.Fragments.TravelFragment;

public class TravelActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));


        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        TravelFragment fragment = new TravelFragment();
        fragmentTransaction.add(R.id.travel_fragmment_holder, fragment);
        fragmentTransaction.commit();

    }
}
