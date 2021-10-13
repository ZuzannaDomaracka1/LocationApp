package com.example.locationapp;

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
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView textCellId;
    Button btn_start_cellId;
    EditText editTextN;
    EditText editTextM;
    Button btn_confirm;
    Button btn_start_rssi;
    TextView text_station;
    TextView text_location;

    TelephonyManager telephonyManager;
    int cellId;
    public List<Integer> measurementsList = new ArrayList<>();

    Timer timer;
    String N_samples;
    String M_samples;
    int final_N_samples;
    int final_M_samples;

    int max_size;

    boolean measurementEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textCellId = findViewById(R.id.textCellId);
        btn_start_cellId = findViewById(R.id.btn_start_cellId);
        editTextN = findViewById(R.id.editTextN);
        editTextM = findViewById(R.id.editTextM);
        btn_confirm = findViewById(R.id.btn_confirm);
        btn_start_rssi = findViewById(R.id.btn_start_rssi);
        text_station = findViewById(R.id.text_stations_count);
        text_location = findViewById(R.id.text_location);

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            return;
        }

        text_location.setVisibility(View.GONE);

        btn_start_cellId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cellInfo();
            }
        });

        btn_confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeKeyboard();

                 N_samples = editTextN.getText().toString();
                 M_samples = editTextM.getText().toString();
                 final_N_samples = Integer.parseInt(N_samples);
                 final_M_samples = Integer.parseInt(M_samples);

                 max_size = final_N_samples + final_M_samples;

            }
        });


        btn_start_rssi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    if(measurementEnabled){
                        measurementEnabled = false;
                        stopMeasurement();}

                else
                    if(final_N_samples!=0){
                        measurementEnabled = true;
                    startMeasurement();
            }}
        });


    }



    private void startMeasurement(){
        btn_start_rssi.setText("STOP");
        if(cellId!=0) {
            collectSamples();
        }
        else
            stopMeasurement();

    }
    private void stopMeasurement(){
        btn_start_rssi.setText("START");
        text_station.setText(String.valueOf(measurementsList));
        estimationLocation();
        //text_station.setText("");
        measurementsList.clear();
        if(timer!=null){
            timer.cancel();
            timer = null;
        }
        text_location.setVisibility(View.INVISIBLE);


    }

    private  int estimationLocation(){

        return 0;
    }


    private void collectSamples(){
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {

                    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    @SuppressLint("MissingPermission")
                    List<CellInfo> cellInfoList = telephonyManager.getAllCellInfo();
                    for(int i=0;i<cellInfoList.size();++i)
                    {
                        CellInfo info = cellInfoList.get(i);
                        if(info instanceof  CellInfoGsm){
                            CellInfoGsm cellInfoGsm = (CellInfoGsm) info;
                            if(measurementsList.size()>=final_N_samples)
                            {
                                if(timer!=null)
                                {
                                    runOnUiThread(() -> {
                                        stopMeasurement();

                                });
                                    timer.cancel();
                                    timer = null;
                                }
                            }
                            else{
                                if(cellId == cellInfoGsm.getCellIdentity().getCid()){
                                    measurementsList.add(cellInfoGsm.getCellSignalStrength().getDbm());
                                }
                                runOnUiThread(() -> {
                                    String measurementText = "";


                                    measurementText=measurementText + cellId + ": " +measurementsList.size() + "/" + final_N_samples;

                                    text_station.setText(measurementText);
                                    text_location.setVisibility(View.GONE);

                                });
                            }

                        }
                    }

                }
                catch (Exception e)
                {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this,"Error", Toast.LENGTH_LONG).show());
                }

            }
        }, 400,1000);
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

    public void closeKeyboard(){
        View view = this.getCurrentFocus();
        if (view!=null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

}



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