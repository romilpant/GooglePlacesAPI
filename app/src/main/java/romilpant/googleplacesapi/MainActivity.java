package romilpant.googleplacesapi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.PhotoMetadata;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.PlaceLikelihood;
import com.google.android.libraries.places.api.net.FetchPhotoRequest;
import com.google.android.libraries.places.api.net.FetchPhotoResponse;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest;
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    // ADD YOUR OWN GOOGLE PLACES API KEY HERE
    private final String API_KEY = "";

    private String placeID;

    Button findCurrentPlacesButton, getPhotoButton;

    EditText placeAddress, placeLikelihood;

    TextView detailsText;

    ImageView photo;


    List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.LAT_LNG, Place.Field.NAME, Place.Field.ADDRESS);


    PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findCurrentPlacesButton = findViewById(R.id.btn_current_places);
        getPhotoButton = findViewById(R.id.btn_getPhoto);

        detailsText = findViewById(R.id.txt_detail);

        photo = findViewById(R.id.image_view);

        placeAddress = findViewById(R.id.edt_address);
        placeLikelihood = findViewById(R.id.edt_likelihood);


        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        placesClient = Places.createClient(this);

        final AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteSupportFragment.setPlaceFields(placeFields);

        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                final LatLng latLng = place.getLatLng();
                final String name = place.getName();

                Toast.makeText(MainActivity.this, "Name of the place is: " + name + " " + latLng.toString(), Toast.LENGTH_LONG).show();

            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });


        // Find current places
        findCurrentPlacesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentPlaces();
            }
        });

        getPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(placeID)) {
                    Toast.makeText(MainActivity.this, "Place Id cannot be null", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    getPhotoAndDetail(placeID);
                }
            }
        });

    }

    private void getPhotoAndDetail(String placeID) {

        FetchPlaceRequest request = FetchPlaceRequest.builder(placeID, Arrays.asList(Place.Field.PHOTO_METADATAS)).build();
        placesClient.fetchPlace(request)
                .addOnSuccessListener(new OnSuccessListener<FetchPlaceResponse>() {
                    @Override
                    public void onSuccess(FetchPlaceResponse fetchPlaceResponse) {
                        try {
                            Place place = fetchPlaceResponse.getPlace();
                            PhotoMetadata photoMetadata = place.getPhotoMetadatas().get(0);
                            final FetchPhotoRequest fetchPhotoRequest = FetchPhotoRequest.builder(photoMetadata).build();
                            placesClient.fetchPhoto(fetchPhotoRequest).addOnSuccessListener(new OnSuccessListener<FetchPhotoResponse>() {
                                @Override
                                public void onSuccess(FetchPhotoResponse fetchPhotoResponse) {
                                    Bitmap bitmap = fetchPhotoResponse.getBitmap();
                                    photo.setImageBitmap(bitmap);
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        } catch (NullPointerException e) {
                            Toast.makeText(MainActivity.this, "No photos available", Toast.LENGTH_LONG).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });


    }


    public void getPermission() {
        Dexter.withActivity(this).withPermissions(Arrays.asList(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {

            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                Toast.makeText(MainActivity.this, "You must allow location permission to use this application", Toast.LENGTH_LONG).show();
            }
        }).check();
    }

    public void getCurrentPlaces() {
        FindCurrentPlaceRequest request = FindCurrentPlaceRequest.builder(placeFields).build();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            getPermission();
            return;
        }
        Task<FindCurrentPlaceResponse> placeResponseTask = placesClient.findCurrentPlace(request);
        placeResponseTask.addOnCompleteListener(new OnCompleteListener<FindCurrentPlaceResponse>() {
            @Override
            public void onComplete(@NonNull Task<FindCurrentPlaceResponse> task) {
                if (task.isSuccessful()) {
                    FindCurrentPlaceResponse response = task.getResult();
                    Collections.sort(response.getPlaceLikelihoods(), new Comparator<PlaceLikelihood>() {
                        @Override
                        public int compare(PlaceLikelihood o1, PlaceLikelihood o2) {
                            return new Double(o1.getLikelihood()).compareTo(o2.getLikelihood());
                        }
                    });

                    Collections.reverse(response.getPlaceLikelihoods());

                    placeID = response.getPlaceLikelihoods().get(0).getPlace().getId();

                    placeAddress.setText(new StringBuilder(response.getPlaceLikelihoods().get(0).getPlace().getAddress()));

                    StringBuilder stringBuilder = new StringBuilder();

                    for (PlaceLikelihood place : response.getPlaceLikelihoods()) {
                        stringBuilder.append(place.getPlace().getName()).append("- Likelihood value: ").append(place.getLikelihood())
                                .append("\n");
                    }

                    placeLikelihood.setText(stringBuilder.toString());
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
