package com.example.vulpix.maphelper.controller.activity;

import java.util.Map;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.example.vulpix.maphelper.R;

// classes needed to initialize map
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapView;

// classes needed to add location layer
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.geometry.LatLng;
import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;

// classes needed to add a marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

// classes to calculate a route
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.util.Log;

// classes needed to launch navigation UI
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

// voice chat
import java.util.Locale;
import java.util.Objects;

import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import timber.log.Timber;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.DESTINATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.DESTINATION_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LATITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.LONGITUDE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.PARTICIPANTS_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.SHARING_LOCATION_BEHAVIOR_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.SHARING_LOCATION_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.TRACKING_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_STATUS_CHILD;

/**
 * Two Users join the same activity and reading the user status from database
 * with live voice chat function by Agora api
 *
 * user 1 can set route for user 2 and the map view will updated in both devices
 */
public class ShareLocationActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final String TAG = "ShareLocationActivity";

    // variables for adding location layer
    private MapboxMap map;
    private MapView mapView;

    // variables for calculating and drawing a route
    private LatLng destinationCoord;
    private Point senderPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private Marker marker;
    private Marker lastMarker;
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    //Firebase variables
    private DatabaseReference shareLocationRef;
    private DatabaseReference locationRef;
    private FirebaseAuth mFirebaseAuth;
    private String uid;
    private boolean setRoute = false;
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    // location variables
    double latitude,longitude;
    LatLng senderCoord;
    LatLng lastCoord;
    LatLng firstCoord;

    // User info
    private String senderId;
    private String senderName;
    private String receiverId;
    private String key;

    // local view variables
    private Button startNavBtn;
    private TextView senderNameView;
    private TextView receiverNameView;
    private TextView senderStatus;
    private TextView receiverStatus;
    private String destination = "empty";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // local layout
        setContentView(R.layout.activity_share_location);
        senderNameView = findViewById(R.id.userName1);
        receiverNameView = findViewById(R.id.userName2);
        senderStatus = findViewById(R.id.userStatus1);
        receiverStatus = findViewById(R.id.userStatus2);
        startNavBtn = findViewById(R.id.start_nav_btn);

        // get user info
        senderId = getIntent().getExtras().getString("sender");
        receiverId = getIntent().getExtras().getString("receiver");
        assert receiverId != null;
        key = hash(senderId, receiverId);

        // menu bar
        getSupportActionBar().setTitle("Search destination");
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);

        // map actions
        Mapbox.getInstance(this, getString(R.string.access_token));
        mapView = findViewById(R.id.mapboxMapView);
        mapView.onCreate(savedInstanceState);

        //other actions
        accessToFirebase();

        //initialize voice chat
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)) {
            initAgoraEngineAndJoinChannel();
        }
    }

    private void accessToFirebase() {

        // initialize Firebase variables
        shareLocationRef = FirebaseDatabase.getInstance().getReference().child(SHARING_LOCATION_CHILD);
        locationRef = FirebaseDatabase.getInstance().getReference().child(TRACKING_CHILD);
        mFirebaseAuth = FirebaseAuth.getInstance();
        uid = Objects.requireNonNull(mFirebaseAuth.getCurrentUser()).getUid();


        // get sender's coordinate value
        locationRef.child(senderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> senderLocation = (Map<String, Object>) dataSnapshot.getValue();
                double senderLat, senderLon;
                assert senderLocation != null;
                senderLat = Double.parseDouble(Objects.requireNonNull(senderLocation.get(LATITUDE_CHILD)).toString());
                senderLon = Double.parseDouble(Objects.requireNonNull(senderLocation.get(LONGITUDE_CHILD)).toString());
                senderCoord = new LatLng(senderLat, senderLon);
                loadMap();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // get behaviour values
        shareLocationRef.child(key).child(SHARING_LOCATION_BEHAVIOR_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> status = (Map<String, Object>) dataSnapshot.getValue();
                assert status != null;
                setRoute = (boolean) status.get("setRoute");
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // get destination values
        shareLocationRef.child(key).child(DESTINATION_CHILD).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> des = (Map<String, Object>) dataSnapshot.getValue();

                assert des != null;
                destination = des.get(DESTINATION_NAME_CHILD).toString();
                latitude = Double.parseDouble(des.get(LATITUDE_CHILD).toString());
                longitude = Double.parseDouble(des.get(LONGITUDE_CHILD).toString());
                destinationCoord = new LatLng(latitude, longitude);
                if(!destination.equals("empty")){
                    loadMap();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        // set user status
        shareLocationRef.child(key).child("Updates").addValueEventListener(new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(uid).child(USER_STATUS_CHILD).setValue("Online");
                setUsersStatus();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }

    private void setUsersStatus(){

        shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(senderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                assert userInfo != null;
                senderName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                String userStatus = Objects.requireNonNull(userInfo.get(USER_STATUS_CHILD)).toString();
                senderNameView.setText(senderName);
                senderStatus.setText(userStatus);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(receiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();
                assert userInfo != null;
                String userName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                String userStatus = Objects.requireNonNull(userInfo.get(USER_STATUS_CHILD)).toString();
                receiverNameView.setText(userName);
                receiverStatus.setText(userStatus);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

    }

    private void loadMap(){
        mapView.getMapAsync(this);
    }

    public void onMapReady(MapboxMap mapboxMap) {

        ShareLocationActivity.this.map = mapboxMap;

        // set assisted person's current location icon
        IconFactory iconFactory = IconFactory.getInstance(ShareLocationActivity.this);
        Icon icon = iconFactory.fromResource(R.drawable.pink_dot);

        // add marker on map
        if(firstCoord == null){
            marker = map.addMarker(new MarkerOptions()
                    .position(senderCoord)
                    .icon(icon)
                    .title(senderName)
                    .snippet("Latitude : " + senderCoord.getLatitude() + " , " +
                            "Longitude : " + senderCoord.getLongitude()));
            setCameraPosition(senderCoord);
            firstCoord = senderCoord;

        }else{

            if (marker != null){
                map.removeMarker(marker);
                lastMarker = marker;
            }

            marker = map.addMarker(new MarkerOptions()
                    .position(senderCoord)
                    .icon(icon)
                    .title(senderName)
                    .snippet("Latitude : " + senderCoord.getLatitude() + " , " +
                            "Longitude : " + senderCoord.getLongitude()));

            // remove previous marker to let marker update with the movement
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(marker, "position",
                    new LatLngEvaluator(), lastMarker.getPosition(), marker.getPosition());
            markerAnimator.setDuration(2000);
            markerAnimator.start();
        }

        if(!destination.equals("empty")){

            // add destination marker on mapboxMapView
            if(destinationMarker != null){
                map.removeMarker(destinationMarker);
            }

            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(destinationCoord)
                    .title(destination)
                    .snippet("Latitude : " + destinationCoord.getLatitude() + " , " +
                            "Longitude : " + destinationCoord.getLongitude()));

            senderPosition = Point.fromLngLat(senderCoord.getLongitude(), senderCoord.getLatitude());
            destinationPosition = Point.fromLngLat(destinationCoord.getLongitude(), destinationCoord.getLatitude());

            getRoute(senderPosition, destinationPosition);

            setRoute = true;
            shareLocationRef.child(key).child(SHARING_LOCATION_BEHAVIOR_CHILD).child("setRoute").setValue(setRoute);

            startNavBtn.setOnClickListener(v -> {
                boolean simulateRoute = false;
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(simulateRoute)
                        .build();
                // Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(ShareLocationActivity.this, options);
            });

            startNavBtn.setEnabled(true);
            startNavBtn.setBackgroundResource(R.color.mapboxBlue);
        }
    }

    private void setCameraPosition(LatLng senderCoord) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                senderCoord, 16));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            CarmenFeature feature = PlaceAutocomplete.getPlace(data);

            getSupportActionBar().setTitle(feature.text());

            destinationPosition = feature.center();
            destination = feature.text();
            latitude = Objects.requireNonNull(feature.center()).latitude();
            longitude = Objects.requireNonNull(feature.center()).longitude();
            destinationCoord = new LatLng(latitude, longitude);

            // update destination on Firebase
            shareLocationRef.child(key).child("Updates").addValueEventListener(new ValueEventListener(){
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    shareLocationRef.child(key).child(DESTINATION_CHILD).child(DESTINATION_NAME_CHILD).setValue(destination);
                    shareLocationRef.child(key).child(DESTINATION_CHILD).child(LATITUDE_CHILD).setValue(latitude);
                    shareLocationRef.child(key).child(DESTINATION_CHILD).child(LONGITUDE_CHILD).setValue(longitude);
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) { }
            });

            Toast.makeText(this, feature.text(), Toast.LENGTH_LONG).show();
        }
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
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
                    public void onFailure(@NonNull Call<DirectionsResponse> call, @NonNull Throwable throwable) {
                        Timber.tag(TAG).e("Error: %s", throwable.getMessage());
                    }
                });
    }

    @Override
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                shareLocationRef.child(key).child(PARTICIPANTS_CHILD).child(uid).child(USER_STATUS_CHILD).setValue("Offline");
                break;

            case R.id.search_place:
                assert Mapbox.getAccessToken() != null;
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.WHITE)
                                .build())
                        .build(ShareLocationActivity.this);

                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);

        }
        return super.onOptionsItemSelected(item);
    }

    public static String hash(String value1, String value2) {
        long result = value1.hashCode() + value2.hashCode();
        return String.valueOf(result);
    }


    /*
     * Start the live voice chat
     */
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;

    // ask for the user id
    private RtcEngine mRtcEngine;
    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        @Override
        public void onUserOffline(final int uid, final int reason) {
            runOnUiThread(() -> onRemoteUserLeft(uid, reason));
        }

        @Override
        public void onUserMuteAudio(final int uid, final boolean muted) {
            runOnUiThread(() -> onRemoteUserVoiceMuted(uid, muted));
        }
    };

    // initialize the channel and join the channel automatically
    private void initAgoraEngineAndJoinChannel() {
        initializeAgoraEngine();
        joinChannel();
    }

    // check permission
    public boolean checkSelfPermission(String permission, int requestCode) {
        Timber.tag(TAG).i("checkSelfPermission " + permission + " " + requestCode);
        if (ContextCompat.checkSelfPermission(this,
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    requestCode);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Timber.tag(TAG).i("onRequestPermissionsResult " + grantResults[0] + " " + requestCode);

        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initAgoraEngineAndJoinChannel();
                } else {
                    showLongToast("No permission for " + Manifest.permission.RECORD_AUDIO);
                    finish();
                }
                break;
            }
        }
    }

    public final void showLongToast(final String msg) {
        this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        leaveChannel();
        RtcEngine.destroy();
        mRtcEngine = null;
    }

    // Mute button click listener
    public void onLocalAudioMuteClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.muteLocalAudioStream(iv.isSelected());
    }

    // Switch speak button click listener
    public void onSwitchSpeakerphoneClicked(View view) {
        ImageView iv = (ImageView) view;
        if (iv.isSelected()) {
            iv.setSelected(false);
            iv.clearColorFilter();
        } else {
            iv.setSelected(true);
            iv.setColorFilter(getResources().getColor(R.color.colorPrimary), PorterDuff.Mode.MULTIPLY);
        }

        mRtcEngine.setEnableSpeakerphone(view.isSelected());
    }

    // Initialize the engine
    private void initializeAgoraEngine() {
        try {
            mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.agora_app_id), mRtcEventHandler);
        } catch (Exception e) {
            Timber.tag(TAG).e(Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannel() {
        mRtcEngine.joinChannel(null, "voiceDemoChannel1", "Extra Optional Data", 0); // if you do not specify the uid, we will generate the uid for you
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    private void onRemoteUserLeft(int uid, int reason) {
        showLongToast(String.format(Locale.US, "user %d left %d", (uid & 0xFFFFFFFFL), reason));
    }

    private void onRemoteUserVoiceMuted(int uid, boolean muted) {
        showLongToast(String.format(Locale.US, "user %d muted or unmuted %b", (uid & 0xFFFFFFFFL), muted));
    }

    // set marker move animation
    private class LatLngEvaluator implements TypeEvaluator<LatLng> {
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
