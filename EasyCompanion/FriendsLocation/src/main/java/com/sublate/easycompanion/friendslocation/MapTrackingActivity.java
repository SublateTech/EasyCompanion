package com.sublate.easycompanion.friendslocation;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import static com.sublate.easycompanion.friendslocation.FollowLocationActivity.ACTION_GEOLOC_LOCATION;


public class MapTrackingActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks{

    public static final String ACTION_GEOLOC_START = "com.sublate.com.ACTION_GEOLOC_START";
    public static final String ACTION_GEOLOC_STOP = "com.sublate.com.ACTION_GEOLOC_STOP";

    private static final String TAG = MapTrackingActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private double latitudeValue = 0.0;
    private double longitudeValue = 0.0;
    private GoogleMap mMap;
    private LatLng mLatLng;
    private String fromJid = null;
    private String toJid = null;


    //    private DatabaseQuery mQuery;
    private RouteBroadCastReceiver routeReceiver;
    protected List<Location> startToPresentLocations;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_tracking);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();
        }
  //      mQuery = new DatabaseQuery(this);
  //      startToPresentLocations = mQuery.getAllLocationObjects();
 //       mLocationRequest = createLocationRequest();
        routeReceiver = new RouteBroadCastReceiver();


        // Get Channel Name
        Intent intent = getIntent();
        fromJid = intent.getStringExtra("fromJid");
        toJid = intent.getStringExtra("toJid");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }
    private void markStartingLocationOnMap(GoogleMap mapObject, LatLng location){
        mapObject.addMarker(new MarkerOptions().position(location).title("Current location"));
        mapObject.moveCamera(CameraUpdateFactory.newLatLng(location));
    }
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Connection method has been called");
        /*
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastLocation != null) {
                                latitudeValue = mLastLocation.getLatitude();
                                longitudeValue = mLastLocation.getLongitude();
                                Log.d(TAG, "Latitude 4: " + latitudeValue + " Longitude 4: " + longitudeValue);
                                refreshMap(mMap);
                                markStartingLocationOnMap(mMap, new LatLng(latitudeValue, longitudeValue));
                                startPolyline(mMap, new LatLng(latitudeValue, longitudeValue));
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
        */
    }
    @Override
    public void onConnectionSuspended(int i) {
    }
    private class RouteBroadCastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra("location");
            //location.setLatitude(location.getLatitude() + .00001);

            double mLat = location.getLatitude();
            double mLng = location.getLongitude();
            mLatLng = new LatLng(mLat, mLng);

            if (mLastLocation == null) {
                mLastLocation = location;
                latitudeValue = mLastLocation.getLatitude() ;
                longitudeValue = mLastLocation.getLongitude();
                Log.d(TAG, "Latitude 4: " + latitudeValue + " Longitude 4: " + longitudeValue);
                refreshMap(mMap);
                markStartingLocationOnMap(mMap, new LatLng(latitudeValue, longitudeValue));
                startPolyline(mMap, new LatLng(latitudeValue, longitudeValue));
            }

            if (startToPresentLocations == null)
                {
                    startToPresentLocations =  new ArrayList<Location>();
                }


            //get all data from database
            //   startToPresentLocations = mQuery.getAllLocationObjects();
            if(startToPresentLocations.size() > 0){
                //prepare map drawing.
                List<LatLng> locationPoints = getPoints(startToPresentLocations);
               // refreshMap(mMap);
                markStartingLocationOnMap(mMap, locationPoints.get(0));
                drawRouteOnMap(mMap, locationPoints);
            }

            startToPresentLocations.add(location);

            mLastLocation = location;


        }
    }
    private List<LatLng> getPoints(List<Location> mLocations){
        List<LatLng> points = new ArrayList<LatLng>();
        for(Location mLocation : mLocations){
            points.add(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        }
        return points;
    }
    private void startPolyline(GoogleMap map, LatLng location){
        if(map == null){
            Log.d(TAG, "Map object is not null");
            return;
        }
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        options.add(location);
        Polyline polyline = map.addPolyline(options);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(location)
                .zoom(16)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
    private void drawRouteOnMap(GoogleMap map, List<LatLng> positions){
        PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        options.addAll(positions);
        Polyline polyline = map.addPolyline(options);


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(positions.get(0).latitude, positions.get(0).longitude))
                .zoom(17)
                .bearing(90)
                .tilt(40)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
    private void refreshMap(GoogleMap mapInstance){
        mapInstance.clear();
    }
    protected LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(routeReceiver == null){
            routeReceiver = new RouteBroadCastReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GEOLOC_LOCATION);

        //LocalBroadcastManager.getInstance(this).registerReceiver(routeReceiver, intentFilter);
        registerReceiver(routeReceiver,intentFilter);

        startFollowingLocation(null);
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopFollowingLocation();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "===onDestroy");
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(routeReceiver);
        stopFollowingLocation();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // =========================================================================
    // Button CallBacks
    // =========================================================================



     public void startFollowingLocation(View view) {
        if (fromJid == null || toJid == null)
            return;

        //  initializePolyline();
        Intent intent = new Intent(ACTION_GEOLOC_START);
        intent.putExtra("fromJid",fromJid);
        intent.putExtra("toJid",toJid);



        sendBroadcast(intent);
        Log.d(TAG, "Sending :" + intent.getAction());
    }

    private void stopFollowingLocation() {
        Intent intent = new Intent(ACTION_GEOLOC_STOP);
        intent.putExtra("fromJid",fromJid);
        intent.putExtra("toJid",toJid);

        sendBroadcast(intent);
        Log.d(TAG, "Sending :" + intent.getAction());

    }
}