package com.sublate.easycompanion.friendslocation;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class FollowLocationActivity extends Activity implements
        OnMapReadyCallback {

    // =========================================================================
    // Properties
    // =========================================================================

    public static final String ACTION_GEOLOC_START = "com.sublate.com.ACTION_GEOLOC_START";
    public static final String ACTION_GEOLOC_STOP = "com.sublate.com.ACTION_GEOLOC_STOP";
    public static final String ACTION_GEOLOC_ONCE = "com.sublate.com.ACTION_GEOLOC_ONCE";
    public static final String ACTION_GEOLOC_GET_ROUTE = "com.sublate.com.ACTION_GEOLOC_GET_ROUTE";

    public static final String ACTION_GEOLOC_LOCATION = "com.sublate.com.ACTION_GEOLOC_LOCATION";
    private static final String TAG = "Tracker - Maps Follow";

    private boolean isFirstMessage = true;
    private boolean mRequestingLocationUpdates = false;


    private Button mCancelButton;
    private Button mShareButton;
    private RelativeLayout mSnackbar;

    // Google Maps
    private GoogleMap mGoogleMap;
//    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;
    private Marker mMarker;
    private LatLng mLatLng;

    private String fromJid = null;
    private String toJid = null;

    // =========================================================================
    // Activity Life Cycle
    // =========================================================================

    @Override
    protected void onDestroy() {
        Log.i(TAG, "===onDestroy");
        // Unregister since the activity is about to be closed.
        unregisterReceiver(mMessageReceiver);
        stopFollowingLocation();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.follow_location_activity);

        // Get Channel Name
        Intent intent = getIntent();
        fromJid = intent.getStringExtra("fromJid");
        toJid = intent.getStringExtra("toJid");


        // Set up View: Map & Action Bar
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GEOLOC_LOCATION);
        registerReceiver(mMessageReceiver,intentFilter);

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        mShareButton = (Button) findViewById(R.id.share_button);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRequestingLocationUpdates = !mRequestingLocationUpdates;
                if (mRequestingLocationUpdates) {
                    startFollowingLocation();
                }
                if (!mRequestingLocationUpdates) {
                    stopFollowingLocation();
                }
                setTextButtons();
            }
        });
        mSnackbar = (RelativeLayout) findViewById(R.id.snackbar);
        TextView snackbarAction = (TextView) findViewById(R.id.snackbar_action);
        snackbarAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });


        this.mSnackbar.setVisibility(View.GONE);
        this.mShareButton.setTextColor(0xde000000);
        setTextButtons();
    }

    void setTextButtons() {
        mShareButton.setEnabled(true);
        mShareButton.setTextColor(0x8a000000);

        if (mRequestingLocationUpdates) {
            mShareButton.setText("Stop Contact's Location");

        }
        if (!mRequestingLocationUpdates) {
            mShareButton.setText("Start Contact's Location");
        }
    }


    // =========================================================================
    // Map CallBacks
    // =========================================================================

    @Override
    public void onMapReady(GoogleMap map) {
        mGoogleMap = map;
        mGoogleMap.setMyLocationEnabled(true);
        Log.d(TAG, "Map Ready");
        setUpMap(map);
        initializePolyline();

    }

    // =========================================================================
    // Button CallBacks
    // =========================================================================

    private void startFollowingLocation() {
      //  initializePolyline();
        Intent intent = new Intent(ACTION_GEOLOC_START);
        intent.putExtra("fromJid",fromJid);
        intent.putExtra("toJid",toJid);

        isFirstMessage = true;

        sendBroadcast(intent);
        Log.d(TAG, "Sending :" + intent.getAction());
    }

    private void stopFollowingLocation() {
        Intent intent = new Intent(ACTION_GEOLOC_STOP);
        intent.putExtra("fromJid",fromJid);
        intent.putExtra("toJid",toJid);

        sendBroadcast(intent);
        Log.d(TAG, "Sending :" + intent.getAction());

        isFirstMessage = true;
    }

    private void getContactRoute() {

        Intent intent = new Intent(ACTION_GEOLOC_GET_ROUTE);
        intent.putExtra("fromJid",fromJid);
        intent.putExtra("toJid",toJid);

        isFirstMessage = true;

        sendBroadcast(intent);
        Log.d(TAG, "Sending :" + intent.getAction());
    }

    // =========================================================================
    // Map Editing Methods
    // =========================================================================

    private void initializePolyline() {
        mGoogleMap.clear();
        mPolylineOptions = new PolylineOptions().geodesic(true);
        mPolylineOptions.color(Color.BLUE).width(5).geodesic(true);
        mGoogleMap.addPolyline(mPolylineOptions);

    }

    private void updatePolyline() {
        mPolylineOptions.add(mLatLng);
        mGoogleMap.clear();
        mGoogleMap.addPolyline(mPolylineOptions);
    }

    private void updateCamera() {

        // Zoom to my location
        /*
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLatLng)      // Sets the center of the map to Mountain View
                .zoom(18)                   // Sets the zoom
                .tilt(20)
                .build();
        */
        final CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(mLatLng)
                .zoom(18)
                .bearing(90)
                .tilt(40)
                .build();
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        // Creates a CameraPosition from the builder
        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        mGoogleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                //Make sure animates until end.
                mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                mGoogleMap.setOnCameraIdleListener(null);
            }
        });
    }

    private void updateMarker() {

        if (mMarker!=null)
			mMarker.remove();

        mMarker =  mGoogleMap.addMarker(new MarkerOptions()
                .position(mLatLng)
                .title(toJid)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location)));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Location location = intent.getParcelableExtra("location");

            double mLat = location.getLatitude();
            double mLng = location.getLongitude();
            mLatLng = new LatLng(mLat, mLng);

            mRequestingLocationUpdates = true;
            setTextButtons();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isFirstMessage) {
                        isFirstMessage = false;
                        updateCamera();
                    }
                    updatePolyline();
                    updateMarker();
                    updateUI();
                }
            });
        }
    };


    public void setUpMap(GoogleMap map){

        // Enable My Location
        map.setMyLocationEnabled(true);

        // Enable My Location Button
        map.getUiSettings().setMyLocationButtonEnabled(true);

        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        map.setMyLocationEnabled(true);
        //map.setTrafficEnabled(true);
        map.setIndoorEnabled(true);
        map.setBuildingsEnabled(true);
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setCompassEnabled(true);

    }

    private void updateUI() {
        Log.d("updateUI", "updateUI");


        // Get visible region
        LatLngBounds bounds = mGoogleMap.getProjection().getVisibleRegion().latLngBounds;

        // Move camera to the new position
        if(!bounds.contains(mLatLng)) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(mLatLng));
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_get_route:
                //Toast.makeText(this, "You have selected Get Menu Action", Toast.LENGTH_SHORT).show();
                getContactRoute();
                return true;
            case R.id.action_schedule_route:
                Toast.makeText(this, "You have selected Get Schedule Action", Toast.LENGTH_SHORT).show();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {

        //getMenuInflater().inflate(R.menu.menu_file, menu);
        return true;
    }

    }
