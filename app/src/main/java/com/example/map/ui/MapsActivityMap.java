package com.example.map.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.map.R;
import com.example.map.Utils.GoogleMapUtil;
import com.example.map.adapter.AutoCompleteGooglePlaceAdapter;
import com.example.map.models.GooglePlace;
import com.example.map.viewmodels.PlaceviewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapsActivityMap extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapClickListener {

    //https://learntodroid.com/android-autocompletetextview-tutorial-with-examples/

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private static final int REQUEST_CHECK_SETTINGS = 200;
    private GoogleMap mMap;
    private String titleMarker="";
    private Marker userMarker, selectedMarker;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean zoomToUserLocation = true;
    private AutoCompleteTextView autoCompleteTextView;
    PlaceviewModel placeViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        placeViewModel = ViewModelProviders.of(this).get(PlaceviewModel.class);
        autoCompleteTextView=findViewById(R.id.input_search);

        autoCompleteTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_DONE
                        || actionId==EditorInfo.IME_ACTION_SEARCH
                        || event.getAction()==KeyEvent.ACTION_DOWN
                        || event.getAction()==KeyEvent.KEYCODE_ENTER){
                    changeStringtolocation();
                }
                hideSoftKeyboard(v);
                return false;
            }
        });


        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                   placeViewModel.searchGooglePlaces(s.toString(), true);
               }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        placeViewModel.getGooglePlacesLiveData().observe(this, new Observer<List<GooglePlace>>() {
            @Override
            public void onChanged(List<GooglePlace> placesList) {
                AutoCompleteGooglePlaceAdapter a = new AutoCompleteGooglePlaceAdapter(MapsActivityMap.this, placesList);
                a.getFilter().filter(autoCompleteTextView.getText().toString());
                autoCompleteTextView.setAdapter(a);
            }
        });



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    showUserLocationOnMap(location);
                    break;
                }
            }
        };
    }

    private void changeStringtolocation() {
        String searchString =autoCompleteTextView.getText().toString();
        Geocoder geocoder=new Geocoder(MapsActivityMap.this);
        List<Address> list=new ArrayList<Address>();
        try {
            list = geocoder.getFromLocationName(searchString, 5);

        }catch (IOException e){
            e.printStackTrace();
        }
        if(list.size()>0){
            Address address=list.get(0);
            zoomToUserLocation=false;
            LatLng userLatLng = new LatLng(address.getLatitude(), address.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng,10));
            userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(address.getAddressLine(0)));
        }
    }

    private void hideSoftKeyboard(View v){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        InputMethodManager inputMethodManager=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(v.getApplicationWindowToken(),0);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(this);
        // to determine my location by blue point
        //  mMap.setMyLocationEnabled(true);
        initUserLocation();
    }

    private void initUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            showUserLocationOnMap(location);
                        }
                    }
                });
        createLocationRequest();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());
        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                createLocationUpdates();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    try {
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapsActivityMap.this, REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                createLocationUpdates();
            } else {
                Toast.makeText(this, "Can't Detect Your Location", Toast.LENGTH_LONG).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void createLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback, null /* Looper */);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initUserLocation();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showUserLocationOnMap(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        if (userMarker == null)
            // this (if condition) to avoid Markers duplication which happen due to OnEditorActionListener action (Edittext)
            if(!titleMarker.equals("Your Location")){
                titleMarker="Your Location";
                userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title(titleMarker));
            }
        else
            userMarker.setPosition(userLatLng);

        if (zoomToUserLocation)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 20));
    }

    @Override
    public void onMapClick(LatLng latLng) {

        if (selectedMarker == null)
            selectedMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
        else
            selectedMarker.setPosition(latLng);

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
        drawShortestPath();
    }

    private void drawShortestPath() {
        zoomToUserLocation = false;
        GoogleMapUtil.zoomAndFitLocations(mMap, 150, userMarker.getPosition(), selectedMarker.getPosition());
        GoogleMapUtil.getAndDrawPath(this, mMap, userMarker.getPosition(), selectedMarker.getPosition(), null, true);
    }

}
