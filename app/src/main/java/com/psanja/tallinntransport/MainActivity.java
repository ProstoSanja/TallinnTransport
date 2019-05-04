package com.psanja.tallinntransport;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.os.Bundle;

import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.SupportMapFragment;
import com.psanja.tallinntransport.Fragments.DeparturesFragment;
import com.psanja.tallinntransport.Fragments.TicketFragment;
import com.psanja.tallinntransport.Managers.MapManager;
import com.psanja.tallinntransport.Managers.StatusManager;
import com.psanja.tallinntransport.Managers.StopsManager;
import com.psanja.tallinntransport.Utils.Bullshit;
import com.psanja.tallinntransport.Utils.Utils;


public class MainActivity extends AppCompatActivity implements StatusManager.OnStatusListener {


    private RequestQueue queue;

    private StatusManager statusManager;
    private StopsManager stopsManager;
    private MapManager mapManager;

    private Bullshit pageAdapter;
    ViewPager pager;
    private int lastid = -1;

    private BottomNavigationView navigation;
    private TextView errorlocation, errorbig, errorbus, errortrain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupAll();
        } else {
            Intent intent = new Intent(this, SetupActivity.class);
            startActivityForResult(intent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            finish();
        }
    }

    private void setupAll() {
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        queue = Volley.newRequestQueue(this);


        errorlocation = findViewById(R.id.error_location);
        errorbig = findViewById(R.id.error_big);
        errorbus = findViewById(R.id.error_bus);
        errortrain = findViewById(R.id.error_train);


        statusManager = new StatusManager(this);
        stopsManager = new StopsManager(this, queue, statusManager);


        Fragment[] fragments = new Fragment[4];

        DeparturesFragment depfragment = new DeparturesFragment();
        depfragment.SetupMe(queue, statusManager, stopsManager, false);
        ///depfragment.setRetainInstance(true);
        fragments[0] = depfragment;
        DeparturesFragment depfragment2 = new DeparturesFragment();
        depfragment2.SetupMe(queue, statusManager, stopsManager, true);
        //depfragment2.setRetainInstance(true);
        fragments[1] = depfragment2;

        SupportMapFragment mapfragment = SupportMapFragment.newInstance();
        //mapfragment.setRetainInstance(true);
        mapManager = new MapManager(MainActivity.this, queue, statusManager, mapfragment);
        fragments[2] = mapfragment;

        TicketFragment ticketFragment = new TicketFragment();
        //ticketFragment.setRetainInstance(true);
        fragments[3] = ticketFragment;

        pageAdapter = new Bullshit(getSupportFragmentManager(), fragments);
        pager = findViewById(R.id.main_fragment_holder);
        pager.setOffscreenPageLimit(3);
        pager.setAdapter(pageAdapter);

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                selectTab(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });



        stopsManager.TryLoadStops();

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int newstate = item.getItemId();
                switch (newstate) {
                    case R.id.navigation_timetable:
                        selectTab(0);
                        break;
                    case R.id.navigation_search:
                        selectTab(1);
                        break;
                    case R.id.navigation_map:
                        selectTab(2);
                        break;
                    case R.id.navigation_ticket:
                        selectTab(3);
                        break;
                }
                return true;
            }
        });

    }

    @Override
    public void onStatus(StatusManager.Status status) {
        switch (status) {
            case SETUP_OK:
                ((DeparturesFragment)pageAdapter.getItem(0)).tryFirstStart();
                ((DeparturesFragment)pageAdapter.getItem(1)).tryFirstStart();
                break;
            case SETUP_ERROR:
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.error_name));
                alertDialog.setCancelable(false);
                alertDialog.setMessage(getResources().getString(R.string.error_setup));
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.error_retry),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                stopsManager.TryLoadStops();
                            }
                        });
                alertDialog.show();
                break;
            case DEPARTURES_OK:
                errorbig.setVisibility(View.GONE);
                break;
            case DEPARTURES_ERROR:
                errorbig.setVisibility(View.VISIBLE);
                break;
            case MAP_BUS_OK:
                ((DeparturesFragment)pageAdapter.getItem(0)).setLive(true);
                ((DeparturesFragment)pageAdapter.getItem(1)).setLive(true);
                errorbus.setVisibility(View.GONE);
                break;
            case MAP_BUS_ERROR:
                ((DeparturesFragment)pageAdapter.getItem(0)).setLive(false);
                ((DeparturesFragment)pageAdapter.getItem(1)).setLive(false);
                errorbus.setVisibility(View.VISIBLE);
                break;
            case MAP_TRAIN_OK:
                errortrain.setVisibility(View.GONE);
                break;
            case MAP_TRAIN_ERROR:
                errortrain.setVisibility(View.VISIBLE);
                break;
            case LOCATION_OK:
                errorlocation.setVisibility(View.GONE);
                break;
            case LOCATION_ERROR:
                errorlocation.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onMapPosition(LatLng location) {
        if (mapManager != null) {
            mapManager.SetCamera(location, 15.5f);
        }
    }

    @Override
    public boolean onMapPosition(Integer markerId) {
        if (mapManager.SetCamera(markerId)) {
            selectTab(2);
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mapManager != null) {
            mapManager.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mapManager != null) {
            mapManager.stop();
        }
    }

    @Override
    protected void onDestroy() {
        Utils.log("ondestroy");
        super.onDestroy();
    }

    public void selectTab(int id) {
        if (id != lastid) {
            lastid = id;
            pager.setCurrentItem(id, false);
            switch (id){
                case 0:
                    navigation.setSelectedItemId(R.id.navigation_timetable);
                    break;
                case 1:
                    navigation.setSelectedItemId(R.id.navigation_search);
                    break;
                case 2:
                    navigation.setSelectedItemId(R.id.navigation_map);
                    break;
                case 3:
                    navigation.setSelectedItemId(R.id.navigation_ticket);
                    break;
            }
        }
    }
}
