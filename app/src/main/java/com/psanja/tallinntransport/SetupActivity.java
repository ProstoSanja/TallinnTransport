package com.psanja.tallinntransport;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.toolbox.Volley;
import com.psanja.tallinntransport.DATAclasses.StopIDs;

import java.util.Map;

public class SetupActivity extends AppCompatActivity {

    private Integer status = 0;
    private Button confirm, permbutton;
    private TextView permtext, downtext;
    private ProgressBar downprogress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        confirm = findViewById(R.id.confirmsetup);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(status);
                finish();
            }
        });
        downtext = findViewById(R.id.down_text);
        permtext = findViewById(R.id.perm_text);
        permbutton = findViewById(R.id.perm_button);
        permbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(SetupActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        });
        downprogress = findViewById(R.id.down_progress);
        DownloadStops();

    }

    private void DownloadStops() {
        new StopSetup(new StopSetup.OnResultListener() {
            @Override
            public void onSuccess(Map<String, StopIDs> list) {
                DownloadComplete();
            }

            @Override
            public void onError(String error) {
                AlertDialog alertDialog = new AlertDialog.Builder(SetupActivity.this).create();
                alertDialog.setTitle(getResources().getString(R.string.error_name));
                alertDialog.setCancelable(false);
                alertDialog.setMessage(error);
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, getResources().getString(R.string.error_retry),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                DownloadStops();
                            }
                        });
                alertDialog.show();
            }
        }, getApplicationContext(), Volley.newRequestQueue(getApplicationContext()));
    }

    private void DownloadComplete() {
        status++;
        downtext.setCompoundDrawablesWithIntrinsicBounds(null, null, getApplication().getDrawable(R.drawable.ic_done), null);
        downprogress.setVisibility(View.GONE);
        check();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String permissions[],@NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            status++;
            permtext.setCompoundDrawablesWithIntrinsicBounds(null, null, getApplication().getDrawable(R.drawable.ic_done), null);
            permbutton.setVisibility(View.GONE);
        }
        check();
    }

    private void check() {
        if (status >= 2) {
            confirm.setEnabled(true);
        } else {
            confirm.setEnabled(false);
        }
    }
}
