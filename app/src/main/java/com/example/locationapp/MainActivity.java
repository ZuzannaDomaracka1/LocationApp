package com.example.locationapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static java.lang.Math.cos;
import static java.lang.Math.incrementExact;
import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity {

    TextView text_cellId;
    Button btn_start_cellId;
    EditText edit_text_n;
    EditText edit_text_m;
    Button btn_confirm;
    Button btn_mode1;
    Button btn_mode2;
    Button btn_clear;
    TextView text_station;
    TextView text_location;
    TextView text_current_location;
    TextView text_error;
    TextView text_timer;

    TelephonyManager telephonyManager;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;

    int current_cellId;
    int other_CellId;
    Location current_location;
    public List<Integer> current_measurementsList = new ArrayList<>();
    public List<Integer> copy_list = new ArrayList<>();
    public List<Integer> final_list = new ArrayList<>();

    Timer timer;
    String N_samples;
    String M_samples;
    int final_N_samples;
    int final_M_samples;

    boolean measurementEnabled = false;

    double current_lat;
    double current_lon;
    double gps_lat;
    double gps_lon;
    double error_location_km;
    double error_location_m;
    int iterator = 0;
    List<String[]> data = new ArrayList<String[]>();

    CountDownTimer countDownTimer;
    int down_timer;





    //Tryb 1 - na żądanie, brak pływającego okna
    //Tryb 2 - tryb ciągły. Użytkownik dostaje aktualizacje pozycji na bieżąco na podstawie analizy zawartości pływającego okna. Wywołanie metody estimationLocation
    // odbywa się w wątku w tle.
    // Zapis danych do csv --  Należy nacisnąć przycisk odświeżania w prawym górnym rogu


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text_cellId = findViewById(R.id.text_cellId);
        text_station = findViewById(R.id.measurements);
        text_location = findViewById(R.id.text_location);
        text_current_location = findViewById(R.id.text_current_location);
        text_error = findViewById(R.id.text_error);
        text_timer = findViewById(R.id.text_timer);

        edit_text_n= findViewById(R.id.edit_text_n);
        edit_text_m = findViewById(R.id.edit_text_m);

        btn_start_cellId = findViewById(R.id.btn_start_cellId);
        btn_confirm = findViewById(R.id.btn_confirm);
        btn_mode1 = findViewById(R.id.btn_mode1);
        btn_mode2 = findViewById(R.id.btn_mode2);
        btn_clear = findViewById(R.id.btn_clear);




        //noinspection deprecation
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                current_location = locationResult.getLastLocation();

            }
        };

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)   {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

        }
        else
        {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
        text_station.setText("");
        text_current_location.setText("");
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

        btn_mode1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                data.add(new String[]{"Index", "GPS-latitude", "GPS-longitude","RSSI-latitude", "RSSI-longitude", "Error", "N", "M"});

                if(measurementEnabled){
                    measurementEnabled = false;
                    stopMeasurement1();}

                else
                if(final_N_samples!=0){
                    measurementEnabled = true;
                    startMeasurement1();
                }}
        });

        btn_mode2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                current_measurementsList.clear();
                if(measurementEnabled){
                    measurementEnabled=false;
                    stop2();}

                else {
                    measurementEnabled = true;
                    startMeasurement2();}
                }
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
                //zapis danych do csv

                save(data);
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
                        Log.i("Cell ID",String.valueOf(current_cellId));

                    }}
                else if (info instanceof CellInfoLte)  //if LTE connection
                {

                    CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
                    CellIdentityLte identityLte = ((CellInfoLte) info).getCellIdentity();
                    int cellId = identityLte.getCi();
                    text_cellId.setText(String.valueOf(cellId));
                }
                if(current_measurementsList.size()!=0)
                {
                    stopTimer();
                }

            } catch (Exception ex) {

            }
        }

    }

    private void startMeasurement1(){

        btn_mode1.setText("STOP");
        text_location.setText(" ");
        text_current_location.setText("");
        text_error.setText("");

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
            stopMeasurement1();
    }

    private void startMeasurement2(){
        btn_mode2.setText("STOP");
        text_location.setText(" ");
        data.add(new String[]{"Index", "GPS-latitude", "GPS-longitude","RSSI-latitude", "RSSI-longitude", "Error", "N", "M"});

        if(current_cellId!=0 && final_N_samples!=0 && final_M_samples!=0 && current_measurementsList.size()==0) {
            Toast.makeText(MainActivity.this, "Pomiar ciągły",Toast.LENGTH_SHORT).show();
           // startTimer();
            collectSamples2();

            }
        if(current_measurementsList.size()!=0) {
            stopTimer();}



    }
    private void stopMeasurement1(){
        btn_mode1.setText("START");
        text_station.setText(String.valueOf(current_measurementsList));
        estimationLocation();

        if(timer!=null){
            timer.cancel();
            timer = null;
        }

    }

    private void stopMeasurement2() {
        if(timer!=null){
            timer.cancel();
            timer = null;
        }
        text_location.setText("Your location: \n" + current_lat +"\n" + current_lon );
        if(current_measurementsList.size()!=0){
            for(int a=0;a<final_M_samples;a++) {
                text_station.setText(String.valueOf(current_measurementsList));
                current_measurementsList.remove(current_measurementsList.size() - 1);
                Log.i("Usuwanie: ", String.valueOf(current_measurementsList));
                text_station.setText(String.valueOf(current_measurementsList));
            }

        }
        stopTimer();
        collectSamples2();

    }
    private void stop2(){
        btn_mode2.setText("START");
        if(timer!=null){
            timer.cancel();
            timer=null;
        }
        Thread.interrupted();
        current_measurementsList.clear();

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
                                        stopMeasurement1();

                                    });
                                    timer.cancel();
                                    timer = null;
                                }
                            }

                            else {

                                if(current_cellId == cellInfoGsm.getCellIdentity().getCid() && final_M_samples==0) {
                                    current_measurementsList.add(0,cellInfoGsm.getCellSignalStrength().getDbm());
                                    Log.i("Wartości RSSI: ", String.valueOf(current_measurementsList));
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
                                        stopMeasurement1();

                                    });
                                    timer.cancel();
                                    timer = null;
                                }
                            }

                            else {

                                if(current_cellId == cellInfoGsm.getCellIdentity().getCid() ) {
                                    current_measurementsList.add(0,cellInfoGsm.getCellSignalStrength().getDbm());
                                    Log.i("Dodawanie: ",String.valueOf(current_measurementsList));
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

    private void collectSamples2(){
        startTimer();
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
                                        estimationLocation2();

                                    });
                                    timer.cancel();
                                    timer = null;
                                }
                            }

                            else {
                                runOnUiThread(() -> {
                                    if(current_cellId == cellInfoGsm.getCellIdentity().getCid() ) {

                                        if(down_timer == 0 && current_measurementsList.size() <final_N_samples){
                                            Log.i("Timer","Czas się skończył, rozpoczynamy od nowa");
                                            current_measurementsList.clear();
                                            Log.i("Wartosc timer: ", String.valueOf(down_timer));
                                            startMeasurement2();
                                        }
                                        else {
                                            text_station.setText(String.valueOf(current_measurementsList));
                                            Log.i("Dodawanie: ", String.valueOf(current_measurementsList));
                                            current_measurementsList.add(0, cellInfoGsm.getCellSignalStrength().getDbm());
                                            text_station.setText(String.valueOf(current_measurementsList));
                                        }

                                    }

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
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    int iterator = 0;
                    int size_list;
                    int size_list1=0;

                    for(DataSnapshot snapshot1 : snapshot.getChildren()){

                        iterator=iterator+1;
                        MeasurementCell measurementCell = snapshot1.getValue(MeasurementCell.class);
                        copy_list.addAll(measurementCell.measurementsList); // kopia listy z Firebase
                        Log.i("", String.valueOf(iterator));
                        Log.i("Skopiowana lista", String.valueOf(current_measurementsList)); //aktualna lista RSSI
                        Log.i("Lista z Firebase: ", String.valueOf(measurementCell.measurementsList)); // //

                        copy_list.retainAll(current_measurementsList); // porównanie
                        size_list = copy_list.size(); // liczba takich samych próbek
                        if(iterator == 1){

                            if(size_list == 0){

                                final_list.clear();
                                current_lat = 0.0;
                                current_lon = 0.0;
                            }
                            else {
                                final_list = measurementCell.measurementsList;
                            }
                        }
                        else if(size_list >= size_list1){
                            final_list = measurementCell.measurementsList;
                            current_lat = measurementCell.lat;
                            current_lon = measurementCell.lon;
                            size_list1 = copy_list.size();
                            if(size_list==0){
                                final_list.clear();
                                current_lon = 0.0;
                                current_lat = 0.0;
                            }
                        }
                        Log.i("Liczba takich samych: ",String.valueOf(size_list));

                        copy_list.clear();


                    }
                    Log.i("Ostateczna lista: ", String.valueOf(final_list));
                    Log.i("Koordynaty lat: ", String.valueOf(current_lat));
                    Log.i("Koordynaty lon: ", String.valueOf(current_lon));

                    if(current_lat==0.0 && current_lon==0.0){
                        text_location.setText("Location not found");
                    }
                    else {
                        text_location.setText("Your location: \n" + current_lat + "\n" + current_lon);
                        updateLocationValues(current_location);


                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

  public void estimationLocation2(){
      measurementEnabled=false;
      other_CellId=current_cellId;


        cellInfo();
        if(other_CellId!=current_cellId){
            current_measurementsList.clear();
            Toast.makeText(MainActivity.this, "Zmiana CellID",Toast.LENGTH_SHORT).show();
            text_location.setText("Your location: \n In progress...");
            text_error.setText("Error of location[m] \n In progress...");
            error_location_m = 0.0;
            Log.i("Po zmianie cellID", String.valueOf(current_measurementsList));
            collectSamples2();
        }
        else {


            Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    DatabaseReference myRef = FirebaseDatabase.getInstance().getReference();
                    Query query = myRef.child("MeasurementCells").orderByChild("cellId").equalTo(current_cellId);
                    other_CellId = current_cellId;
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                int iterator = 0;
                                int size_list;
                                int size_list1 = 0;

                                for (DataSnapshot snapshot1 : snapshot.getChildren()) {

                                    iterator = iterator + 1;
                                    MeasurementCell measurementCell = snapshot1.getValue(MeasurementCell.class);
                                    copy_list.addAll(measurementCell.measurementsList); // kopia listy z Firebase
                                    Log.i("", String.valueOf(iterator));
                                    Log.i("Skopiowana lista", String.valueOf(current_measurementsList)); //aktualna lista RSSI
                                    Log.i("Lista z Firebase: ", String.valueOf(measurementCell.measurementsList)); //

                                    copy_list.retainAll(current_measurementsList); // porównanie
                                    size_list = copy_list.size(); // liczba takich samych próbek
                                    if (iterator == 1) {

                                        if (size_list == 0) {
                                            text_location.setText("Location not found");
                                            final_list.clear();
                                            current_lat = 0.0;
                                            current_lon = 0.0;
                                        }
                                        else{
                                            final_list = measurementCell.measurementsList;
                                        }
                                    } else if (size_list >= size_list1) {

                                        final_list = measurementCell.measurementsList;
                                        current_lat = measurementCell.lat;
                                        current_lon = measurementCell.lon;
                                        size_list1 = copy_list.size();
                                        if (size_list == 0) {
                                            final_list.clear();
                                            current_lon = 0.0;
                                            current_lat = 0.0;
                                        }
                                    }
                                    Log.i("Liczba takich samych wartości: ", String.valueOf(size_list));

                                    copy_list.clear();

                                }
                                Log.i("Ostateczna lista: ", String.valueOf(final_list));
                                Log.i("Koordynaty lat: ", String.valueOf(current_lat));
                                Log.i("Koordynaty lon: ", String.valueOf(current_lon));
                                text_station.setText("");


                                updateLocationValues(current_location);

                                stopMeasurement2();

                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

                }
            };

            Thread thread = new Thread(runnable);
            thread.start();
        }

  }

  @SuppressLint("SetTextI18n")
  public void updateLocationValues(Location location){

        iterator = iterator+1; //index
        String s_iterator = String.valueOf(iterator);

        gps_lat = location.getLatitude(); // koordynaty GPS
        gps_lon = location.getLongitude();
        String s_gps_lat = String.valueOf(gps_lat);
        String s_gps_lon = String.valueOf(gps_lon);

        text_current_location.setText("Location GPS: \n" + gps_lat + "\n" + gps_lon);
        String s_current_lat = String.valueOf(current_lat);
        String s_current_lon = String.valueOf(current_lon);


         //error
         error_location_km = sqrt(Math.pow(gps_lat-current_lat,2)+(Math.pow(cos((current_lat*Math.PI)/180)*(gps_lon-current_lon),2)))*(40075.704/360);
         error_location_m = error_location_km * 1000;
         Log.i("error [m]", String.valueOf(error_location_m));
         text_error.setText("Error of location [m]: \n" + error_location_m);

         String s_error = String.valueOf(error_location_m);

         String s_n_final = String.valueOf(final_N_samples);
         String s_m_final = String.valueOf(final_M_samples);

         if(current_lon==0.0 && current_lat==0.0){
             text_location.setText("Location not found");
             text_error.setText("Error of location [m]: \n" + "Location not found");
             error_location_m = 0.0;
         }
         else {
             text_location.setText("Your location: \n" + current_lat + "\n" + current_lon);
         }

         data.add(new String[]{s_iterator, s_gps_lat,s_gps_lon, s_current_lat,s_current_lon, s_error,s_n_final,s_m_final});

  }

  public void save(List<String[]> data){

      @SuppressWarnings("deprecation")
      String csv = (Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyMeasuremenrs.csv");
      CSVWriter writer;

      try {

          writer = new CSVWriter(new FileWriter(csv));

          writer.writeAll(data);
          writer.close();
      } catch (IOException e) {
          e.printStackTrace();
      }


  }

/*
    protected void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
    }


 */
@SuppressLint("MissingPermission")
@Override
public void onRequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch (requestCode) {
        case 100:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);

            } else {
                Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_LONG).show();
            }
    }
}

    public void closeKeyboard(){
        View view = this.getCurrentFocus();
        if (view!=null){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(),0);
        }
    }

    public void startTimer() {

        countDownTimer =  new CountDownTimer(final_N_samples*1000+10000, 1000) {


            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                int total = (int) (millisUntilFinished / 1000);
                down_timer = Integer.parseInt(String.valueOf(total));
                text_timer.setText(String.valueOf(total));

            }

            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {


            }
        }.start();
    }

    @SuppressLint("SetTextI18n")
    public void stopTimer(){

        countDownTimer.cancel();

    }


}


