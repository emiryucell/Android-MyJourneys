package com.emirtechs.myjourneys;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Insert;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.emirtechs.myjourneys.model.Place;
import com.emirtechs.myjourneys.roomdb.PlaceDao;
import com.emirtechs.myjourneys.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.emirtechs.myjourneys.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    LocationManager locationManager;
    LocationListener locationListener;
    ActivityResultLauncher<String> resultLauncher;
    SharedPreferences sharedPreferences;
    boolean info;
    PlaceDatabase db;
    PlaceDao placeDao;
    Double selectedLat;
    Double selectedLon;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    Place selectedPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        registerLauncher();
        sharedPreferences = MapsActivity.this.getSharedPreferences("com.emirtechs.myjourneys", MODE_PRIVATE);
        info = false;

        db = Room.databaseBuilder(MapsActivity.this, PlaceDatabase.class, "Places")
                //.allowMainThreadQueries()
                .build();
        placeDao = db.placeDao();

        selectedLat = 0.0;
        selectedLon = 0.0;

        binding.saveButton.setEnabled(false);

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");
        if (intentInfo.equals("new")) {
            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);

            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {


                    info = sharedPreferences.getBoolean("info", false);

                    if (!info) {
                        LatLng user = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(user, 15));
                        sharedPreferences.edit().putBoolean("info", true).apply();

                    }
                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Snackbar.make(binding.getRoot(), "PERMISSION NEEDED", Snackbar.LENGTH_INDEFINITE).setAction("GIVE PERMISSION", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                } else {
                    resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null) {
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                }
                mMap.setMyLocationEnabled(true);

            }


        } else {

            mMap.clear();

            selectedPlace = (Place) intent.getSerializableExtra("place");
            LatLng latLng = new LatLng(selectedPlace.latitude, selectedPlace.longitude);
            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            binding.placeNameText.setText(selectedPlace.name);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }


    }

    private void registerLauncher() {
        resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {

                    if (ContextCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (lastLocation != null) {
                            LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                        }
                    }
                } else {

                    Toast.makeText(MapsActivity.this, "Permission Needed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));

        selectedLat = latLng.latitude;
        selectedLon = latLng.longitude;

        binding.saveButton.setEnabled(true);

    }

    public void save(View view) {
        Place place = new Place(binding.placeNameText.getText().toString(), selectedLat, selectedLon);
        compositeDisposable.add(placeDao.insert(place).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handleResponse));
    }

    private void handleResponse() {
        Intent intent = new Intent(MapsActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    public void delete(View view) {
        if (selectedPlace != null) {

            compositeDisposable.add(placeDao.delete(selectedPlace)
                    .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::handleResponse));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}