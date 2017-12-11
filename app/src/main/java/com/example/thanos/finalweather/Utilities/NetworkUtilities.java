package com.example.thanos.finalweather.Utilities;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * Created by Thanos on 12/3/2017.
 */

public class NetworkUtilities {

    private static final String OPEN_WEATHER_API_URL = "http://api.openweathermap.org/data/2.5/weather";

    final static String QUERY_PARAM = "q";
    final static String LAT_PARAM = "lat";
    final static String LON_PARAM = "lon";
    final static String API_KEY_PARAM = "appid";
    final static String API_KEY = "714c575794dfcad408544516f8b1f983";

    public static URL buildUrl (Double lat, Double lon){
        Uri buildUri = Uri.parse(OPEN_WEATHER_API_URL).buildUpon()
                .appendQueryParameter(LAT_PARAM, String.valueOf(lat))
                .appendQueryParameter(LON_PARAM, String.valueOf(lon))
                .appendQueryParameter(API_KEY_PARAM, API_KEY)
                .build();

        URL url = null;

        try{
            url = new URL(buildUri.toString());
        }catch (MalformedURLException e){
            e.printStackTrace();
        }

        return url;
    }

    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                return scanner.next();
            } else {
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}
