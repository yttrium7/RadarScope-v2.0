package com.example.vulpix.maphelper.controller.activity;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import com.example.vulpix.maphelper.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.Map;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.LATITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LONGITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.TRACKING_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * Track activity is linking to the auto-safe-messaging function by clicking the AUTO SAFE MESSAGE
 * which is sent by the elder people(assistance required user)
 * it shows the map with elder people's marker and his/her destination and route
 * which is updated in real time
 */

public class TrackActivity extends AppCompatActivity implements OnMapReadyCallback {

    // variables for adding location layer
    private MapboxMap map;
    private MapView mapView;
    private ImageButton senderLocation;

    // variables for adding a marker
    private Marker marker;

    // variables for calculating and drawing a route
    private Point currentPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private static final String TAG = "TrackActivity";
    private NavigationMapRoute navigationMapRoute;

    // Firebase variables
    private DatabaseReference locationRef;
    private DatabaseReference userRef;
    private String senderId;
    private String senderName;

    // local variables
    LatLng destination;
    LatLng checkDestination = null;
    double latitude,longitude;
    LatLng currentCoord;
    LatLng lastCoord;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track);

        // Firebase variables
        locationRef = FirebaseDatabase.getInstance().getReference().child(TRACKING_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);

        // read extra value from ChatActivity
        senderId = getIntent().getStringExtra("sender");
        try{
            double destination_lat = getIntent().getDoubleExtra("destination_lat", 1);
            double destination_lon = getIntent().getDoubleExtra("destination_lon", 1);
            destination = new LatLng(destination_lat, destination_lon);
        }catch (NullPointerException e){
            Timber.tag(TAG).e("No destination coord passed");
        }

        // Mapbox variable
        Mapbox.getInstance(this, getString(R.string.access_token));

        mapView = findViewById(R.id.track_map);
        senderLocation = findViewById(R.id.map_to_location);
        mapView.onCreate(savedInstanceState);


        // get coordinate value
        userRef.child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                senderName = Objects.requireNonNull(Objects.requireNonNull(userInfo).get(USER_NAME_CHILD)).toString();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // get coordinate value
        locationRef.child(senderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> location = (Map<String, Object>) dataSnapshot.getValue();
                assert location != null;
                latitude = Double.parseDouble(Objects.requireNonNull(location.get(LATITUDE_CHILD)).toString());
                longitude = Double.parseDouble(Objects.requireNonNull(location.get(LONGITUDE_CHILD)).toString());

                if(currentCoord != null){
                    lastCoord = currentCoord;
                }
                currentCoord = new LatLng(latitude, longitude);
                loadMap();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    public void loadMap(){
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {

        TrackActivity.this.map = mapboxMap;

        // set assisted person's current location icon
        IconFactory iconFactory = IconFactory.getInstance(TrackActivity.this);
        Icon icon = iconFactory.fromResource(R.drawable.pink_dot);

        // add marker on map
        if (lastCoord == null) {
            marker = map.addMarker(new MarkerOptions()
                    .position(currentCoord)
                    .icon(icon)
                    .title(senderName)
                    .snippet("Latitude : " + currentCoord.getLatitude() +
                            "," + "Longitude : " + currentCoord.getLongitude()));
        } else {

            // remove previous marker to let marker update with the movement
            if (marker != null) {
                map.removeMarker(marker);
            }
            marker = map.addMarker(new MarkerOptions()
                    .position(lastCoord)
                    .icon(icon)
                    .title(senderName)
                    .snippet("Latitude : " + currentCoord.getLatitude() +
                            ", " + "Longitude : " + currentCoord.getLongitude()));
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(marker, "position",
                    new LatLngEvaluator(), marker.getPosition(), currentCoord);
            markerAnimator.setDuration(2000);
            markerAnimator.start();
        }

        // if the user(elder people) update the destination, helper can track elder people's
        // location with destination and route
        if (destination != null) {

            if(checkDestination == null){
                animateCameraToNewPosition(destination);
                checkDestination = new LatLng(destination.getLatitude(), destination.getLongitude());
            }

            mapboxMap.addMarker(new MarkerOptions()
                    .position(destination)
                    .title("Destination")
                    .snippet("Latitude : " + destination.getLatitude() +
                            ", " + "Longitude : " + destination.getLongitude())
            );

            currentPosition = Point.fromLngLat(currentCoord.getLongitude(), currentCoord.getLatitude());
            destinationPosition = Point.fromLngLat(destination.getLongitude(), destination.getLatitude());
            getRoute(currentPosition, destinationPosition);
        }

        senderLocation.setOnClickListener(view -> {
            if (currentCoord != null) {
                animateCameraToNewPosition(currentCoord);
            }
        });
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Timber.tag(TAG).d("Response code: %s", response.code());
                        if (response.body() == null) {
                            Timber.tag(TAG).e("No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Timber.tag(TAG).e("No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, map, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Timber.e("Error: %s", throwable.getMessage());
                    }
                });
    }

    private void animateCameraToNewPosition(LatLng latLng) {
        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(16)
                        .build()));
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    // set marker move animation
    public static class LatLngEvaluator implements TypeEvaluator<LatLng> {
        // Method is used to interpolate the marker animation.
        private LatLng latLng = new LatLng();

        @Override
        public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }
}

