package com.example.lotadataandroid;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.FusedLocationProviderClient;
import android.location.Location;
import android.location.LocationManager;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import org.greenrobot.eventbus.EventBus;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FusedLocationProviderClient mFusedLocationClient;
    private ActivityRecognitionClient mActivityRecognitionClient;
    int PERMISSION_LOCATION_REQUEST_ID = 44;
    int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 45;
    int counter = 1;
    String currentActivity = "Still";
    Intent locationService;
    Intent activityService;
    private boolean runningQOrLater =
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        locationService = new Intent(this, LocationService.class);
        mActivityRecognitionClient = new ActivityRecognitionClient(this);
//        activityService = new Intent(this, ActivityIntentService.class);
        if (checkPermissions()) {
            if(!isLocationEnabled()) {
                requestPermissions();
                requestActivityPermission();
            }
        }
        else {
            requestPermissions();
            requestActivityPermission();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationService);
//            startForegroundService(activityService);
        }
        else {
            startService(locationService);
//            startService(activityService);
        }
        requestUpdatesHandler();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("Schedule", " Stop Task Scheduled");
                stopService(locationService);
//                stopService(activityService);
            }
        }, 300000);
        EventBus.getDefault().register(this);
        mapFragment.getMapAsync(this);
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        LatLng marker = new LatLng(event.latitude, event.longitude);
        mMap.addMarker(new MarkerOptions().position(marker).title(currentActivity));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(marker));
        Toast.makeText(this, "Marker No." + counter + " added!", Toast.LENGTH_SHORT).show();
        counter++;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ActivityEvent event) {
        this.currentActivity = event.activity;
    }

    public void requestUpdatesHandler() {
//Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                10000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {

            }
        });
    }
    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
//Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    private boolean checkPermissions(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED){
            return true;
        }
        return false;
    }

    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_LOCATION_REQUEST_ID
        );
    }

    private void requestActivityPermission(){
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
    }

    private boolean isLocationEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
}
