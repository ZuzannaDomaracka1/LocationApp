package com.example.locationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textCellId;
    Button btn_start;
    List<CellInfo> cellInfoList;
    TelephonyManager telephonyManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textCellId = findViewById(R.id.textCellId);
        btn_start = findViewById(R.id.btn_start);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the missing permissions
            requestPermission();
            return;
        }


        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"przycisk",Toast.LENGTH_SHORT).show();

                cellInfo();

            }
        });


        //FirebaseDatabase database = FirebaseDatabase.getInstance();
        //DatabaseReference myRef = database.getReference().child("Measurements");
        //myRef.orderByChild("cellId");


    }
    @SuppressLint("MissingPermission")
    public void cellInfo() {
        Toast.makeText(MainActivity.this,"przycis9k",Toast.LENGTH_SHORT).show();

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        List<CellInfo> infos = tel.getAllCellInfo();
        for (int i = 0; i<infos.size(); ++i)
        {
            try {
                CellInfo info = infos.get(i);
                if (info instanceof CellInfoGsm) //if GSM connection
                {
                    if(info.isRegistered())
                    {
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        int cellId = identityGsm.getCid();
                        textCellId.setText(String.valueOf(cellId));


                   // CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
                    //list += "dBm: " + gsm.getDbm() + "\r\n\r\n";


                }}
                else if (info instanceof CellInfoLte)  //if LTE connection
                {

                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    int cellId = identityLte.getCi();
                    textCellId.setText(String.valueOf(cellId));

                }


            } catch (Exception ex) {

            }
        }



    }

    protected void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
    }

}