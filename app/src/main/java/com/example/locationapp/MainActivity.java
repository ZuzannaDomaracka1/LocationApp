package com.example.locationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    TextView textCellId;
    Button btn_start;
    List<CellInfo> cellInfoList;
    TelephonyManager telephonyManager;
    int cellId;
    List<Integer> list = new ArrayList<Integer>();


    TextView text1, text2, text3;
    DatabaseReference databaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textCellId = findViewById(R.id.textCellId);
        btn_start = findViewById(R.id.btn_start);
        text1 = findViewById(R.id.text1);
        text2 = findViewById(R.id.text2);
        text3 = findViewById(R.id.text3);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("Measurements");
        databaseReference.orderByChild("lat").equalTo(52.3921003).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.i("OUR INFO", String.valueOf(snapshot.getValue()));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



        //text1.setText(String.valueOf(list.size()));

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

                cellInfo();


              /*  Query query = reference.child("Measurements").child("measurementAllCells").orderByChild("cellId").equalTo(cellId);
                query.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists())
                        {
                            for (DataSnapshot snapshot1:snapshot.getChildren())
                            {
                                list.add(snapshot1.getValue().toString());
                                Toast.makeText(MainActivity.this,"Error",Toast.LENGTH_SHORT).show();

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,"Error",Toast.LENGTH_SHORT).show();

                    }
                });

               */

            }
        });






        //myRef.orderByChild("cellId");


    }
    @SuppressLint("MissingPermission")
    public void cellInfo() {

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        List<CellInfo> allInfo = tel.getAllCellInfo();
        for (int i = 0; i<allInfo.size(); ++i)
        {
            try {
                CellInfo info = allInfo.get(i);
                if (info instanceof CellInfoGsm) //if GSM connection
                {
                    if(info.isRegistered())
                    {
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        cellId = identityGsm.getCid();
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