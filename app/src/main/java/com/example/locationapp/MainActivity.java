package com.example.locationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView text_cellId;
    Button btn_start_cellId;
    EditText edit_text_n;
    EditText edit_text_m;
    Button btn_confirm;
    Button btn_start_rssi;
    Button btn_clear;
    TextView text_station;
    TextView text_location;

    TelephonyManager telephonyManager;
    int current_cellId;
    public List<Integer> current_measurementsList = new ArrayList<>();
    public List<Integer> copy_list = new ArrayList<>();
    public List<Integer> final_list = new ArrayList<>();

    Timer timer;
    String N_samples;
    String M_samples;
    int final_N_samples;
    int final_M_samples;

    boolean measurementEnabled = false;

    String current_lat;
    String current_lon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_cellId = findViewById(R.id.text_cellId);
        text_station = findViewById(R.id.text_stations_count);
        text_location = findViewById(R.id.text_location);

        edit_text_n= findViewById(R.id.edit_text_n);
        edit_text_m = findViewById(R.id.edit_text_m);

        btn_start_cellId = findViewById(R.id.btn_start_cellId);
        btn_confirm = findViewById(R.id.btn_confirm);
        btn_start_rssi = findViewById(R.id.btn_start_rssi);
        btn_clear = findViewById(R.id.btn_clear);


        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
            return;
        }

        text_location.setText("");

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                current_measurementsList.clear();
                text_station.setText(String.valueOf(current_measurementsList));

            }
        });

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

                N_samples = edit_text_n.getText().toString();
                M_samples = edit_text_m.getText().toString();
                final_N_samples = Integer.parseInt(N_samples);
                final_M_samples = Integer.parseInt(M_samples);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.refresh_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch ((item.getItemId())){
            case R.id.refresh:
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                return true;
            default: return super.onOptionsItemSelected(item);
        }

    }

    @SuppressLint("MissingPermission")
    public void cellInfo() {

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        List<CellInfo> allInfo = tel.getAllCellInfo();
        for (int i = 0; i<allInfo.size(); ++i)
        {
            try {
                CellInfo info = allInfo.get(i);
                if (info instanceof CellInfoGsm)
                {
                    if(info.isRegistered())
                    {
                        CellIdentityGsm identityGsm = ((CellInfoGsm) info).getCellIdentity();
                        current_cellId = identityGsm.getCid();
                        text_cellId.setText(String.valueOf(current_cellId));

                    }}
                else if (info instanceof CellInfoLte)  //if LTE connection
                {

                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    int cellId = identityLte.getCi();
                    text_cellId.setText(String.valueOf(cellId));

                }

            } catch (Exception ex) {

            }
        }

    }

    private void startMeasurement(){
        btn_start_rssi.setText("STOP");
        text_location.setText(" ");
        if(current_cellId !=0 &&  final_N_samples!=0  && final_M_samples==0) {
            Toast.makeText(MainActivity.this, "pierwszy pomiar ", Toast.LENGTH_SHORT).show();
            collectSamples();
        }
        else if (current_cellId!=0 && final_N_samples!=0 && final_M_samples!=0)
        {
            Toast.makeText(MainActivity.this, "drugi pomiar ", Toast.LENGTH_SHORT).show();
            for(int j=0;j<final_M_samples;j++){
                current_measurementsList.remove(current_measurementsList.size()-1);
                Log.i("Usuwanie: ", String.valueOf(current_measurementsList));
                text_station.setText(String.valueOf(current_measurementsList));
            }
            collectNextSamples();
        }

        else
            stopMeasurement();

    }
    private void stopMeasurement(){
        btn_start_rssi.setText("START");
        text_station.setText(String.valueOf(current_measurementsList));
        estimationLocation();

        if(timer!=null){
            timer.cancel();
            timer = null;
        }

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
                            if(current_measurementsList.size()>=final_N_samples)
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

                            else {

                                if(current_cellId == cellInfoGsm.getCellIdentity().getCid() && final_M_samples==0) {
                                    current_measurementsList.add(0,cellInfoGsm.getCellSignalStrength().getDbm());
                                    Log.i("Pierwsze próbki: ", String.valueOf(current_measurementsList));
                                }

                                runOnUiThread(() -> {
                                    String measurementText = "";
                                    measurementText = measurementText + current_cellId + ": " + current_measurementsList.size() + "/" + final_N_samples;
                                    text_station.setText(measurementText);

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

    private void collectNextSamples() {
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

                            if(current_measurementsList.size()>=final_N_samples)
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

                            else {


                                if(current_cellId == cellInfoGsm.getCellIdentity().getCid() ) {
                                    current_measurementsList.add(0,cellInfoGsm.getCellSignalStrength().getDbm());
                                    Log.i("dodawanie: ",String.valueOf(current_measurementsList));
                                }

                                runOnUiThread(() -> {

                                    text_station.setText(String.valueOf(current_measurementsList));

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




    private void  estimationLocation() {
        measurementEnabled=false;
        Toast.makeText(MainActivity.this,"estimation location",Toast.LENGTH_SHORT).show();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
        Query query = myRef.child("MeasurementCells").orderByChild("cellId").equalTo(current_cellId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    int iterator = 0;
                    copy_list.addAll(current_measurementsList);
                    int size_list;
                    int size_list1=0;

                    for(DataSnapshot snapshot1 : snapshot.getChildren()){
                        iterator=iterator+1;
                        MeasurementCell measurementCell = snapshot1.getValue(MeasurementCell.class);
                        Log.i("", String.valueOf(iterator));
                        Log.i("Skopiowana lista", String.valueOf(copy_list));
                        Log.i("Lista z Firebase: ", String.valueOf(measurementCell.measurementsList));

                        copy_list.retainAll(measurementCell.measurementsList);
                        size_list = copy_list.size();
                        if(iterator == 1){
                            final_list = measurementCell.measurementsList;
                            if(size_list == 0){
                                text_location.setText("Location not found");
                            }
                        }
                        else if(size_list > size_list1){
                            final_list = measurementCell.measurementsList;
                            current_lat = String.valueOf(measurementCell.lat);
                            current_lon = String.valueOf(measurementCell.lon);
                        }
                        Log.i("Liczba takich samych: ",String.valueOf(copy_list.size()));
                        size_list1 = copy_list.size();
                        copy_list.clear();
                        copy_list.addAll(current_measurementsList);

                    }
                    Log.i("Ostateczna lista: ", String.valueOf(final_list));
                    Log.i("Koordynaty lat: ", String.valueOf(current_lat));
                    Log.i("Koordynaty lon: ", String.valueOf(current_lon));
                    text_station.setText("");
                    text_location.setText("Your location: \n" + current_lat +"\n" + current_lon );

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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
