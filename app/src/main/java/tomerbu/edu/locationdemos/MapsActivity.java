package tomerbu.edu.locationdemos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.style.StyleSpan;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.concurrent.TimeUnit;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "TomerBu";
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequset;
    private AutocompleteFilter mAutocompleteFilter;
    private LatLngBounds.Builder mBoundsBuilder = new LatLngBounds.Builder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        MapsActivityPermissionsDispatcher.getPlacesAroundLocationWithCheck(this);
        getPlacesAroundLocation();
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void getPlacesAroundLocation() {
        AutocompleteFilter.Builder mAutoCompleteFilterBuilder = new AutocompleteFilter.Builder();
        mAutoCompleteFilterBuilder.setTypeFilter(Place.TYPE_RESTAURANT);
        mAutocompleteFilter = mAutoCompleteFilterBuilder.build();
        mLocationRequset = new LocationRequest();
        mLocationRequset.setFastestInterval(1000).setInterval(10000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setNumUpdates(10000);
        mGoogleApiClient = new GoogleApiClient.Builder(this, this, this).
                addApi(LocationServices.API).
                addApi(Places.GEO_DATA_API).
                addApi(Places.PLACE_DETECTION_API).
                build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null && !mGoogleApiClient.isConnected()) mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null) mGoogleApiClient.disconnect();
    }


    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showRationaleForCamera(PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_gps_rationale)
                .setPositiveButton(R.string.button_allow, (dialog, button) -> request.proceed())
                .setNegativeButton(R.string.button_deny, (dialog, button) -> request.cancel())
                .show();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void showDeniedForGPS() {
        Toast.makeText(this, R.string.permission_gps_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void showNeverAskForGPS() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_gps_rationale)
                .setPositiveButton(R.string.button_allow, (dialog, button) -> {
                    Uri packageURI = Uri.parse("package:" + getPackageName());
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.button_deny, (dialog, button) -> {
                    System.exit(0);
                })
                .show();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MapsActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(31.254414, 34.801855);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Beer Sheva"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Toast.makeText(MapsActivity.this, "Connected", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequset, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        LatLngBounds bounds = LatLngBounds.builder().include(latLng).build();

        showPlaceAutoCompleteFragment();
        //more elaborate control:
        manuallyGetPlacesFromGooglePlacesApi(bounds);
        autoCompleteTextViewWithCustomAdapter(bounds);
    }

    /**
     * AutoComplete Text View using a custom adapter made by google:
     */
    @NonNull
    private LatLngBounds autoCompleteTextViewWithCustomAdapter(LatLngBounds bounds) {
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.auto);
        autoCompleteTextView.setAdapter(new PlaceAutocompleteAdapter(this, mGoogleApiClient, bounds, mAutocompleteFilter));
        /**AutoComplete Text View:*/return bounds;
    }

    private void manuallyGetPlacesFromGooglePlacesApi(final LatLngBounds bounds) {
        new AsyncTask<Void, Void, AutocompletePredictionBuffer>() {
            @Override
            protected AutocompletePredictionBuffer doInBackground(Void... params) {
                PendingResult<AutocompletePredictionBuffer> results =
                        Places.GeoDataApi
                                .getAutocompletePredictions(mGoogleApiClient, "black",
                                        bounds, mAutocompleteFilter);

                AutocompletePredictionBuffer autocompletePredictions = results
                        .await(60, TimeUnit.SECONDS);
                com.google.android.gms.common.api.Status status = autocompletePredictions.getStatus();
                if (!status.isSuccess()) {
                    Log.e(TAG, status.toString());
                }

                return autocompletePredictions;
            }

            @Override
            protected void onPostExecute(AutocompletePredictionBuffer autocompletePredictions) {
                StringBuilder builder = new StringBuilder();

                for (AutocompletePrediction autocompletePrediction : autocompletePredictions) {
                    Log.e(TAG, autocompletePrediction.toString());
                    builder.append(autocompletePrediction.getFullText(new StyleSpan(Typeface.BOLD))).append("\n________\n");
                }
                Toast.makeText(MapsActivity.this, builder.toString(), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    private void showPlaceAutoCompleteFragment() {
        SupportPlaceAutocompleteFragment autocompleteFragment = new SupportPlaceAutocompleteFragment();
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.

                Log.i(TAG, "Place: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        getSupportFragmentManager().beginTransaction().replace(R.id.placeHolder, autocompleteFragment).commit();
    }
}
