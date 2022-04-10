package com.example.firebasedangooglemaps;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener{

    private GoogleMap gMap;
    private Marker selectedMarket;
    private LatLng selectedPlace;
    private LatLng selectedStartPlace;
    private Location currentLocation;

    private FirebaseFirestore db;

    private TextView txtOrderId,txtSelectedPlace,txt_titik_awal;
    private EditText editTextName;
    private Button btnEditOrder, btnOrder, btn_getLocation;

    private boolean isNewOrder = true;
    boolean locationPermission = false;

    private final static int REQ_CODE_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtOrderId = findViewById(R.id.txt_orderId);
        txtSelectedPlace = findViewById(R.id.txt_selectedPlace);
        txt_titik_awal = findViewById(R.id.txt_titik_awal);
        editTextName = findViewById(R.id.editText_nama);
        btnEditOrder = findViewById(R.id.btn_editOrder);
        btnOrder = findViewById(R.id.btn_order);
        btn_getLocation = findViewById(R.id.btn_getLocation);

        requestPermission();

        db = FirebaseFirestore.getInstance();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btnOrder.setOnClickListener(view -> {saveOrder();});
        btnEditOrder.setOnClickListener(view -> {updateOrder();});
        btn_getLocation.setOnClickListener(view -> {mylocation();});

    }

    private void requestPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)  != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_CODE_PERMISSION);
        }
        else{
            locationPermission=true;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_CODE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermission = true;
                    getLocation();

                } else {
                    Toast.makeText(this, "Need to access location", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        gMap.setMyLocationEnabled(true);
        gMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {

                currentLocation = location;
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15.0f);
                gMap.animateCamera(cameraUpdate);
            }
        });

    }

    private void mylocation(){

        selectedStartPlace = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

        Geocoder geo = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses_start = geo.getFromLocation(selectedStartPlace.latitude, selectedStartPlace.longitude, 1);
            if (addresses_start != null){
                Address place = addresses_start.get(0);
                StringBuilder street = new StringBuilder();

                for (int i=0; i<= place.getMaxAddressLineIndex(); i++){
                    street.append(place.getAddressLine(i)).append("\n");
                }

                txt_titik_awal.setText(street.toString());
            }
            else {
                Toast.makeText(this, "Not Found", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e){
            Toast.makeText(this, "not found", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;

        if (locationPermission){
            getLocation();
        }

        LatLng salatiga = new LatLng(-7.335, 110.5084);

        selectedPlace = salatiga;
        selectedMarket = gMap.addMarker(new MarkerOptions().position(selectedPlace));

        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(selectedPlace, 15.0f));

        gMap.setOnMapClickListener(this);

    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        selectedPlace = latLng;
        selectedMarket.setPosition(selectedPlace);
        gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> addresses = geocoder.getFromLocation(selectedPlace.latitude, selectedPlace.longitude, 1);
            if (addresses != null){
                Address place = addresses.get(0);
                StringBuilder street = new StringBuilder();

                for (int i=0; i<= place.getMaxAddressLineIndex(); i++){
                    street.append(place.getAddressLine(i)).append("\n");
                }

                txtSelectedPlace.setText(street.toString());
            }
            else {
                Toast.makeText(this, "Not Found", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e){
            Toast.makeText(this, "not found", Toast.LENGTH_SHORT).show();
        }
    }

    private  void saveOrder(){
        Map<String, Object> order = new HashMap<>();
        Map<String, Object> titik_awal = new HashMap<>();
        Map<String, Object> titik_tujuan = new HashMap<>();

        String name = editTextName.getText().toString();

        titik_awal.put("address_start", txt_titik_awal.getText().toString());
        titik_awal.put("lat_start", selectedStartPlace.latitude);
        titik_awal.put("lng_start", selectedStartPlace.longitude);


        titik_tujuan.put("address_end", txtSelectedPlace.getText().toString());
        titik_tujuan.put("lat_end", selectedPlace.latitude);
        titik_tujuan.put("lng_end", selectedPlace.longitude);

        order.put("Titik_tujuan", titik_tujuan);
        order.put("Titik_awal", titik_awal);
        order.put("createDate", new Date());
        order.put("name", name);


        String orderId = txtOrderId.getText().toString();

        if (isNewOrder){
            db.collection("orders").add(order).addOnSuccessListener(documentReference -> {
                editTextName.setText("");
                txtSelectedPlace.setText("pilih titik tujuan");
                txtOrderId.setText(documentReference.getId());
            })
                    .addOnFailureListener(e -> {
                Toast.makeText(this, "gagal tambah data order", Toast.LENGTH_SHORT).show();
            });
        }
        else {
            db.collection("orders").document(orderId).set(order).addOnSuccessListener(unused -> {
                editTextName.setText("");
                txtSelectedPlace.setText("");
                txtOrderId.setText(orderId);
                isNewOrder = true;
            })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Gagal ubah data order", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updateOrder(){
        isNewOrder = false;
        String orderId = txtOrderId.getText().toString();
        DocumentReference order = db.collection("orders").document(orderId);
        order.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()){
                DocumentSnapshot document = task.getResult();
                if (document.exists()){
                    String name = document.get("name").toString();
                    Map<String, Object> titik_tujuan = (HashMap<String, Object>) document.get("Titik_tujuan");

                    editTextName.setText(name);
                    txtSelectedPlace.setText(titik_tujuan.get("address_end").toString());

                    LatLng resultPlace = new LatLng((double) titik_tujuan.get("lat_end"), (double) titik_tujuan.get("lng_end"));
                    selectedPlace = resultPlace;
                    selectedMarket.setPosition(selectedPlace);
                    gMap.animateCamera(CameraUpdateFactory.newLatLng(selectedPlace));

                }
                else {
                    isNewOrder = true;
                    Toast.makeText(this, "Documents not exist", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Toast.makeText(this, "Tidak dapat membaca Database", Toast.LENGTH_SHORT).show();
            }
        });
    }


}