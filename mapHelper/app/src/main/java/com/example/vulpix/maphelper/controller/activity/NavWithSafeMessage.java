package com.example.vulpix.maphelper.controller.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.vulpix.maphelper.model.ChatMessage;
import com.example.vulpix.maphelper.R;

// classes needed to initialize map
import com.example.vulpix.maphelper.service.GeofenceTrasitionService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapView;

// classes needed to add location layer
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import android.location.Location;
import com.mapbox.mapboxsdk.geometry.LatLng;

import android.support.annotation.NonNull;

import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;

// classes needed to add a marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions;

// classes to calculate a route
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

// classes needed to launch navigation UI
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;

import org.json.JSONObject;

import static com.example.vulpix.maphelper.controller.activity.MainActivity.ROUTE_CHECK_POINT_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.ROUTE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_IMAGE_CHILD;
import static com.example.vulpix.maphelper.controller.activity.MainActivity.USER_NAME_CHILD;

/**
 * A self navigation process with auto-safe-messaging function which is deployed by Geofence
 * once set the route, analyse the route points by Google Direction api
 * and select the average route points as check points to add geofence on them
 */

public class NavWithSafeMessage extends AppCompatActivity implements LocationEngineListener,
        PermissionsListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "NavWithSafeMessage";

    // variables for adding location layer
    @BindView(R.id.map_view)
    MapView mapView;
    private MapboxMap map;
    private PermissionsManager permissionsManager;
    private LocationLayerPlugin locationPlugin;
    private LocationEngine locationEngine;
    private Location originLocation;

    // variables for adding a marker
    private LatLng originCoord;
    private LatLng destinationCoord;

    // variables for calculating and drawing a route
    private Point originPosition;
    private Point destinationPosition;
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    // local layout variables
    private Button button;
    private ImageButton needHelpBtn;
    private String place;

    private Marker destinationMarker;
    private GoogleApiClient googleApiClient;
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    // Firebase variables
    private DatabaseReference routeRef;
    private DatabaseReference routeCheckPointRef;
    private String uid;
    private String receiver;

    // Firebase user variables
    private DatabaseReference userRef;
    private String key;
    private String myName;
    private String myPhoto;

    // geofence check points variables
    public static List<List<HashMap<String, String>>> routePoints = null;
    private ArrayList<LatLng> tempCheckPoints;
    private ArrayList geofenceList;
    public int CHECK_POINT_INTERVAL = 5;

    // GPS
    public final static String AUTO_START_MESSAGE = "I am going to  ";

    // date variables
    DateFormat df = DateFormat.getDateTimeInstance();
    Date date = new Date();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Timber.tag(TAG).i("onCreate()");
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));
        setContentView(R.layout.activity_self_nav);
        mapView = findViewById(R.id.mapView);
        button = findViewById(R.id.startButton);
        needHelpBtn = findViewById(R.id.need_help);


        // Initialize firebaseDB
        routeRef = FirebaseDatabase.getInstance().getReference().child(ROUTE_CHILD);
        routeCheckPointRef = FirebaseDatabase.getInstance().getReference().child(ROUTE_CHECK_POINT_CHILD);
        userRef = FirebaseDatabase.getInstance().getReference().child(USER_CHILD);
        uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        receiver = getIntent().getStringExtra("receiver");
        key = ShareLocationActivity.hash(receiver, uid);


        // menu bar
        getSupportActionBar().setTitle("Search destination");
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);

        // start map call listener
        mapView.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            enableLocationPlugin();
            assert(originLocation != null);

        });

        mapView.onCreate(savedInstanceState);

        // create GoogleApiClient
        createGoogleApi();

        // ask for help btn click listener
        needHelpBtn.setOnClickListener(v -> {
            Intent intent = new Intent(NavWithSafeMessage.this, AskForHelp.class);
            startActivity(intent);
        });
    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Timber.tag(TAG).d("createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Timber.tag(TAG).i("onConnected()"); }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Timber.tag(TAG).w("onConnectionSuspended()");
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Timber.tag(TAG).w("onConnectionFailed()");
    }

    private void getRoute(Point origin, Point destination) {
        assert Mapbox.getAccessToken() != null;
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
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
                        Timber.tag(TAG).e("Error: %s", throwable.getMessage());
                    }
                });
    }

    private void animateCameraToNewPosition(LatLng latLng) {
        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(new CameraPosition.Builder()
                        .target(latLng)
                        .zoom(14)
                        .build()), 1500);
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create an instance of LOST location engine
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, map, locationEngine);
            locationPlugin.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            originLocation = lastLocation;
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 13));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            finish();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {

        Timber.tag(TAG).i("onLocationChanged()");
        if (location != null) {
            originLocation = location;
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            CarmenFeature feature = PlaceAutocomplete.getPlace(data);

            getSupportActionBar().setTitle(feature.text());

            Toast.makeText(this, feature.placeName(), Toast.LENGTH_LONG).show();

            place = feature.placeName();

            destinationPosition = feature.center();
            destinationCoord = new LatLng(destinationPosition.latitude(), destinationPosition.longitude());
            originCoord = new LatLng(originLocation.getLatitude(), originLocation.getLongitude());
            originPosition = Point.fromLngLat(originCoord.getLongitude(), originCoord.getLatitude());

            if (destinationMarker != null) {
                map.removeMarker(destinationMarker);
            }
            destinationMarker = map.addMarker(new MarkerOptions()
                    .position(destinationCoord)
            );

            getRoute(originPosition, destinationPosition);
            animateCameraToNewPosition(destinationCoord);
            createGeofence(originCoord, destinationCoord);

            button.setOnClickListener(v -> {
                sendStartMessage();
                startGeofence();
                NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                        .directionsRoute(currentRoute)
                        .shouldSimulateRoute(false)
                        .build();

                // Call this method with Context from within an Activity
                NavigationLauncher.startNavigation(NavWithSafeMessage.this, options);
            });

            button.setEnabled(true);
            button.setBackgroundResource(R.color.mapboxBlue);
        }
    }

    private void sendStartMessage(){
        Timber.tag(TAG).i("sendStartMessage()");
        userRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Map<String, Object> userInfo = (Map<String, Object>) dataSnapshot.getValue();

                myName = Objects.requireNonNull(userInfo.get(USER_NAME_CHILD)).toString();
                myPhoto = Objects.requireNonNull(userInfo.get(USER_IMAGE_CHILD)).toString();

                // upload message to firebase
                ChatMessage msg = new ChatMessage(AUTO_START_MESSAGE + place,
                        myName, uid, myPhoto, null, destinationCoord, df.format(date));

                MainActivity.updateMessageToFirebase(msg, uid, receiver, AUTO_START_MESSAGE + place);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });

        Toast.makeText(this, "Safe message send successfully", Toast.LENGTH_LONG).show();

    }

    private void createGeofence(LatLng originCoord, LatLng destinationCoord){
        Timber.tag(TAG).i("createGeofence()");
        makeRoute(originCoord, destinationCoord);
    }

    private void makeRoute(LatLng origin, LatLng dest) {

        //Getting URL to the Google Directions API
        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

    }

    public String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.getLatitude() + "," + origin.getLongitude();

        // Destination of route
        String str_dest = "destination=" + dest.getLatitude() + "," + dest.getLongitude();

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=walking";

        // access token
        String key = "key=" + getString(R.string.google_api_key);
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Timber.tag("Exception").d(e.toString());
        } finally {
            assert iStream != null;
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    @SuppressLint("StaticFieldLeak")
    private class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {

                data = downloadUrl(url[0]);

            } catch (Exception e) {

                Timber.tag("Background task").d(e.toString());

            }

            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();
            parserTask.execute(result);

        }
    }

    @SuppressLint("StaticFieldLeak")
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);

            } catch (Exception e) {


                e.printStackTrace();
            }

            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {

            routePoints = result;
            tempCheckPoints = new ArrayList<>();

            for (int i = 0; i < result.size(); i++) {

                List<HashMap<String, String>> path = result.get(i);

                if(path.size()<5){ CHECK_POINT_INTERVAL = 2; }
                for (int j = 0; j < path.size(); j += CHECK_POINT_INTERVAL) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(Objects.requireNonNull(point.get("lat")));
                    double lng = Double.parseDouble(Objects.requireNonNull(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);

                    tempCheckPoints.add(position);
                }
            }

            // update the whole route on Firebase
            routeRef.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    routeRef.child(uid).setValue(routePoints.get(0));
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });

            // update route check point on Firebase
            routeCheckPointRef.child(uid).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    routeCheckPointRef.child(uid).setValue(tempCheckPoints);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                }
            });
        }
    }

    // Start Geofence creation process
    private void startGeofence() {

        Timber.tag(TAG).i("startGeofence(" + tempCheckPoints.toString() + ")");

        if( tempCheckPoints.size() != 0 ) {
            List<Geofence> geofences = createGeofence( tempCheckPoints, GEOFENCE_RADIUS );
            GeofencingRequest geofenceRequest = createGeofenceRequest( geofences );
            addGeofence( geofenceRequest );
        } else {
            Timber.tag(TAG).e("checkPoints is null");
        }
    }
    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final float GEOFENCE_RADIUS = 50.0f; // in meters

    // Create a Geofence
    private ArrayList<Geofence> createGeofence(ArrayList<LatLng> checkPoints, float radius ) {
        Timber.tag(TAG).d("createGeofence");

        geofenceList = new ArrayList();
        String geofenceID = "geoID01";

        for (LatLng point : checkPoints) {

            double lat = point.getLatitude();
            double lon = point.getLongitude();
            geofenceID = geofenceID + "1";

            geofenceList.add(new Geofence.Builder()
                    .setRequestId(geofenceID)
                    .setCircularRegion(lat, lon, radius)
                    .setExpirationDuration(GEO_DURATION)
                    .setNotificationResponsiveness(1000)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());

        }

        return geofenceList;
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest( List<Geofence> geofences) {
        Timber.tag(TAG).d("createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofences( geofences )
                .build();
    }

    private PendingIntent geoFencePendingIntent;

    private PendingIntent createGeofencePendingIntent() {
        Timber.tag(TAG).d("createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTrasitionService.class);
        intent.putExtra("receiver", receiver);
        intent.putExtra("destination", place);
        intent.putExtra("destination_lat", destinationCoord.getLatitude());
        intent.putExtra("destination_lon", destinationCoord.getLongitude());
        int GEOFENCE_REQ_CODE = 0;
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Timber.tag(TAG).d("addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(status ->{
                if(status.isSuccess()){
                    Toast.makeText(this, "Add Geofence successfully", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(this, "Add Geofence failed", Toast.LENGTH_LONG).show();
                }
            });
    }

    private boolean checkPermission() {
        Timber.tag(TAG).d("checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
        googleApiClient.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
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
                break;

            case R.id.search_place:
                assert Mapbox.getAccessToken() != null;
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken())
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.WHITE)
                                .build())
                        .build(NavWithSafeMessage.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
        }
        return super.onOptionsItemSelected(item);
    }


}
