package com.example.thanos.finalweather;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.thanos.finalweather.Utilities.NetworkUtilities;
import com.example.thanos.finalweather.Utilities.WeatherUtilities;
import com.example.thanos.finalweather.data.WeatherDbHelper;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String> {

    private FusedLocationProviderClient mFusedLocationClient;
    private ProgressBar loadingIndicator;
    private Double lat;
    private Double lng;
    private Double temperature;
    private String description;
    private String placeName;
    private String humidity;
    private String pressure;
    private String windSpeed;
    private Double toCelsius;
    private Integer weatherId;
    private Long sunrise;
    private Long sunset;
    private TextView tvDate, tvLocation, tvWeatherDescription, tvTemperature, tvHumidity, tvPressure;
    private TextView tvWindSpeed, tvSunrise, tvSunset;
    private TextView tvHumidityLabel, tvPressureLabel, tvWindSpeedLabel, tvSunriseLabel, tvSunsetLabel;
    private ImageView weatherIcon;

    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 200;
    private static final int LOADER_ID = 23;
    private static final int REQUEST_CHECK_SETTINGS = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDate = findViewById(R.id.date);
        tvLocation = findViewById(R.id.location);
        tvWeatherDescription = findViewById(R.id.weather_description);
        tvTemperature = findViewById(R.id.temperature);
        tvHumidity = findViewById(R.id.humidity);
        tvPressure = findViewById(R.id.pressure);
        tvWindSpeed = findViewById(R.id.wind_speed);
        tvSunrise = findViewById(R.id.sunrise);
        tvSunset = findViewById(R.id.sunset);
        tvHumidityLabel = findViewById(R.id.humidity_label);
        tvPressureLabel = findViewById(R.id.pressure_label);
        tvWindSpeedLabel = findViewById(R.id.wind_speed_label);
        tvSunriseLabel = findViewById(R.id.sunrise_label);
        tvSunsetLabel = findViewById(R.id.sunset_label);
        weatherIcon = findViewById(R.id.weather_icon);


        loadingIndicator = findViewById(R.id.loading_indicator);

        WeatherDbHelper dbHelper = new WeatherDbHelper(this);


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION);
            return;
        }


        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
            }
        });

        //createLocationRequest();
        getSupportLoaderManager().initLoader(LOADER_ID, null, this).forceLoad();


    }



    private void createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000*60*3);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException)e).getStatusCode();
                switch (statusCode){
                    case CommonStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        }catch (IntentSender.SendIntentException sendEx){

                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public Loader<String> onCreateLoader(int id, final Bundle args) {
        return new AsyncTaskLoader<String>(this) {

            String weatherData = null;

            @Override
            protected void onStartLoading() {


                if (weatherData != null){
                    deliverResult(weatherData);
                }else {
                    loadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public String loadInBackground() {

                URL weatherRequest = NetworkUtilities.buildUrl(lat, lng);

                try {
                    String jsonWeatherResponse = NetworkUtilities.getResponseFromHttpUrl(weatherRequest);
                    return jsonWeatherResponse;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }

            }

            public void deliverResult (String data){
                super.deliverResult(data);
                weatherData = data;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<String> loader, String data) {
        loadingIndicator.setVisibility(View.INVISIBLE);

        try {
            JSONObject jsonObject = new JSONObject(data);
            JSONObject mainData = new JSONObject(jsonObject.getString("main"));
            JSONObject windData = new JSONObject(jsonObject.getString("wind"));
            JSONObject sysData = new JSONObject(jsonObject.getString("sys"));

            temperature = Double.parseDouble(mainData.getString("temp"));

            JSONArray jsonArray = jsonObject.getJSONArray("weather");
            JSONObject jsonWeather = jsonArray.getJSONObject(0);
            description = jsonWeather.getString("description");
            weatherId = Integer.parseInt(jsonWeather.getString("id"));

            toCelsius = temperature - 273.15;
            placeName = jsonObject.getString("name");
            humidity = mainData.getString("humidity");
            pressure = mainData.getString("pressure");
            windSpeed = windData.getString("speed");
            sunrise = Long.parseLong(sysData.getString("sunrise"));
            sunset = Long.parseLong(sysData.getString("sunset"));

        } catch (JSONException e) {
            e.printStackTrace();
        }


        updateUserInterface();
    }

    @Override
    public void onLoaderReset(Loader<String> loader) {

    }

    private void updateUserInterface(){
        setHelperViewsVisibility();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE, dd/MM");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        long currentTime = System.currentTimeMillis();
        String humidityToDisplay = humidity + " %";
        String pressureToDisplay = pressure + " hPa";
        String windSpeedToDisplay = windSpeed + " meter/sec";
        String temperatureToDisplay = (String.valueOf(new DecimalFormat("##.#").format(toCelsius)))+"°C";
        String sunriseTimeToDisplay = String.valueOf(timeFormat.format(sunrise * 1000L));
        String sunsetTimeToDisplay = String.valueOf(timeFormat.format(sunset * 1000L));


        tvDate.setText(String.valueOf(simpleDateFormat.format(currentTime)));
        weatherIcon.setImageResource(WeatherUtilities.getIconResourceForWeatherCondition(weatherId));
        tvTemperature.setText(String.valueOf(temperatureToDisplay));
        tvLocation.setText(placeName);
        tvWeatherDescription.setText(description);
        tvHumidity.setText(humidityToDisplay);
        tvPressure.setText(pressureToDisplay);
        tvWindSpeed.setText(windSpeedToDisplay);
        tvSunrise.setText(sunriseTimeToDisplay);
        tvSunset.setText(sunsetTimeToDisplay);
    }

    private void setHelperViewsVisibility(){
        tvHumidityLabel.setVisibility(View.VISIBLE);
        tvPressureLabel.setVisibility(View.VISIBLE);
        tvWindSpeedLabel.setVisibility(View.VISIBLE);
        tvSunriseLabel.setVisibility(View.VISIBLE);
        tvSunsetLabel.setVisibility(View.VISIBLE);
    }
}
