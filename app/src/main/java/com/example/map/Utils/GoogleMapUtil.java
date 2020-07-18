package com.example.map.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.map.R;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Eslam El-hoseiny on 6/10/2016.
 */
public final class GoogleMapUtil {

    private GoogleMapUtil() {
    }

    public static LatLng convertToLatLong(String latlong) {
        try {
            String[] arrLatLong = latlong.split(",");
            return new LatLng(Double.parseDouble(arrLatLong[0]), Double.parseDouble(arrLatLong[1]));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void getAndDrawPath(Context context, GoogleMap googleMap, LatLng from, LatLng to, TextView textView, boolean byCar) {

        String url = getDirectionsUrl(from, to, byCar);
        DownloadTask downloadTask = new DownloadTask(context, googleMap, textView);
        downloadTask.execute(url);

    }

    private static String getDirectionsUrl(LatLng from, LatLng to, boolean byCar) {

        String str_origin = "origin=" + from.latitude + "," + from.longitude;

        String str_dest = "destination=" + to.latitude + "," + to.longitude;

        String sensor = null;

        if (byCar) {
            sensor = "sensor=false";
        } else {
            sensor = "mode=walking&sensor=true";
        }

        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/json?" + parameters;


        return url;
    }

    public static void clearPaths() {
        if (ParserTask.polyline != null) {
            ParserTask.polyline.remove();
        }
    }

    private static class DownloadTask extends AsyncTask<String, Void, String> {

        Context context;
        GoogleMap googleMap;
        TextView textView;

        public DownloadTask(Context context, GoogleMap googleMap, TextView textView) {
            this.context = context;
            this.googleMap = googleMap;
            this.textView = textView;
        }

        @Override
        protected String doInBackground(String... url) {

            try {
                return requestGet(url[0]);

            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            JSONObject distOb = null;
            JSONObject timeOb = null;

            try {
                final JSONObject json = new JSONObject(result);
                JSONArray routeArray = json.getJSONArray("routes");
                JSONObject routes = routeArray.getJSONObject(0);

                JSONArray newTempARr = routes.getJSONArray("legs");
                JSONObject newDisTimeOb = newTempARr.getJSONObject(0);

                distOb = newDisTimeOb.getJSONObject("distance");
                timeOb = newDisTimeOb.getJSONObject("duration");


                if (textView != null && context != null) {
//                    String distanceAndTime = "\n"+context.getString(R.string.driver_on_his_way) + "\n\n\t( " + timeOb.getString("text") + " & " + distOb.getString("text") + " )\n";
//                    textView.setText(distanceAndTime);
                }
//                Toast.makeText(context, distanceAndTime, Toast.LENGTH_LONG).show();

                ParserTask parserTask = new ParserTask(context, googleMap);
                parserTask.execute(result);


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ParserTask extends AsyncTask<String, PolylineOptions, Void> {

        Context context;
        GoogleMap googleMap;

        public ParserTask(Context context, GoogleMap googleMap) {
            this.context = context;
            this.googleMap = googleMap;
        }

        @Override
        protected Void doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {

                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                routes = parser.parse(jObject);

                if (routes.size() > 0) {
                    ArrayList<LatLng> points = new ArrayList<LatLng>();
                    PolylineOptions lineOptions = new PolylineOptions();

                    List<HashMap<String, String>> path = routes.get(0);

                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    }

                    lineOptions.addAll(points);
                    lineOptions.width(10);
                    lineOptions.color(ContextCompat.getColor(context, R.color.colorPrimary));
                    publishProgress(lineOptions);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        public static Polyline polyline;

        @Override
        protected void onProgressUpdate(PolylineOptions... values) {
            try {
                if (polyline != null) {
                    polyline.remove();
                }
                polyline = googleMap.addPolyline(values[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    private static class DirectionsJSONParser {

        /**
         * Receives a JSONObject and returns a list of lists containing latitude and longitude
         */
        public List<List<HashMap<String, String>>> parse(JSONObject jObject) {

            List<List<HashMap<String, String>>> routes = new ArrayList<List<HashMap<String, String>>>();
            JSONArray jRoutes = null;
            JSONArray jLegs = null;
            JSONArray jSteps = null;

            try {

                jRoutes = jObject.getJSONArray("routes");

                /** Traversing all routes */
                for (int i = 0; i < jRoutes.length(); i++) {
                    jLegs = ((JSONObject) jRoutes.get(i)).getJSONArray("legs");
                    List path = new ArrayList<HashMap<String, String>>();

                    /** Traversing all legs */
                    for (int j = 0; j < jLegs.length(); j++) {
                        jSteps = ((JSONObject) jLegs.get(j)).getJSONArray("steps");

                        /** Traversing all steps */
                        for (int k = 0; k < jSteps.length(); k++) {
                            String polyline = "";
                            polyline = (String) ((JSONObject) ((JSONObject) jSteps.get(k)).get("polyline")).get("points");
                            List<LatLng> list = decodePoly(polyline);

                            /** Traversing all points */
                            for (int l = 0; l < list.size(); l++) {
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("lat", Double.toString(((LatLng) list.get(l)).latitude));
                                hm.put("lng", Double.toString(((LatLng) list.get(l)).longitude));
                                path.add(hm);
                            }
                        }
                        routes.add(path);
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            } catch (Exception e) {
            }


            return routes;
        }


        /**
         * Method to decode polyline points
         * Courtesy : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
         */
        private List<LatLng> decodePoly(String encoded) {

            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((((double) lat / 1E5)),
                        (((double) lng / 1E5)));
                poly.add(p);
            }

            return poly;
        }

    }

    private static String requestGet(String strUrl) throws IOException {
        String data = "";
        try {
            URL url = new URL(strUrl);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.connect();
            InputStream iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb = new StringBuffer();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            data = sb.toString();
            br.close();
            iStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void openGoogleNavigationActivity(Context context, LatLng mCurrentLatLong, LatLng tripDestinationLatLong) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?saddr="
                + mCurrentLatLong.latitude + "," + mCurrentLatLong.longitude + "&daddr=" + tripDestinationLatLong.latitude + "," + tripDestinationLatLong.longitude));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        if (intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void openGoogleSearchPlaces(Activity activity, int requestCode) {

        try {

            AutocompleteFilter typeFilter = new AutocompleteFilter
                    .Builder()
                    .setCountry("EG")
                    .build();
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
                    .setFilter(typeFilter)
                    .build(activity);
            activity.startActivityForResult(intent, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static void zoomAndFitLocations(GoogleMap googleMap, int padding, LatLng... latLogs) {
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (LatLng latLng : latLogs) {
                builder.include(latLng);
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
