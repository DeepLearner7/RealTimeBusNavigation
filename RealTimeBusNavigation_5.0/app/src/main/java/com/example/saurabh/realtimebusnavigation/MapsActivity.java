package com.example.saurabh.realtimebusnavigation;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static java.lang.Float.parseFloat;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "test";
    private GoogleMap mMap;
    private Button button;
    private TextView textView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    ArrayList<LatLng> stops = new ArrayList<LatLng>();
    ArrayList<String> stops_name = new ArrayList<String>();
    private String bus_id  = login.BusId;
    //private String userInputBusId;
    //HashMap map;
    int stop_counter = 0;

    private List<Polyline> polylinePaths = new ArrayList<>();

    static InputStream is = null;
    static JSONObject json = null;
    static String output = "";

    //OnCreate options
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    //For menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId()==R.id.logout);
        {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            try {
                DatabaseReference firebase_last_visited = database.getReference("lastVisited").child(bus_id);
                firebase_last_visited.removeValue();
            }
            catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Probably Bus not initiated yet.", Toast.LENGTH_LONG).show();
            }
            finally {

                finish();
                Intent i = new Intent(MapsActivity.this, login.class);
                startActivity(i);
            }

        }



        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Update it
        //userInputBusId = R.layout.activity_login.

        //bus_id = userInputBusId;
        //
        /*
        switch (user.getUid()) {
            case "2ndXH6Xkc4gS23cdU77VHDUQ81D2":
                bus_id = "29A-U";
                break;
            case "3fcP6eizh9Xa8kcwHyX10g0qus93":
                bus_id = "29A-U";
                break;
            case "7ogrGVgoeDgol4OCyxjEWIbDDJ53":
                bus_id = "29A-U";
                break;
        }*/
        Log.d("bus_id",bus_id);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    //OnMapReady
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("BusRoutes").child(bus_id);

        //Trigered when data changed in BusRoutes child of root.
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {

            //Retrival of bus_stop name and coordinate and plot on map( Promises problem occured ) and initiate get_current_coordinate of bus request.
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                /*map =(HashMap) dataSnapshot.getValue();
                Iterator iter = (Iterator) map.keySet().iterator();
                stops_name = new ArrayList<String>();
                //stops = new ArrayList<LatLng>();*/
                ArrayList<String> retrival = (ArrayList<String>)dataSnapshot.getValue();
                Log.d("JSON Parser",retrival.get(0).toString() );
                //for(Object key : map.keySet())
                for(int key=0; key< retrival.size(); key++) {
                    String coordinade = (String) retrival.get(key);
                    double lat = parseFloat(coordinade.split(",")[0].split(" ")[0]);
                    double lng = parseFloat(coordinade.split(",")[0].split(" ")[1]);
                    String bus_stop = coordinade.split(",")[1];

                    stops_name.add(bus_stop);
                    stops.add(new LatLng(lat, lng));
                }
                    mMap.addMarker(new MarkerOptions().position(stops.get(stops.size()-1)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    for (int i = stops.size()-2; i>=0 ; i--) {
                        mMap.addMarker(new MarkerOptions().position(stops.get(i)));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(stops.get(i), 16));
                    }
                    //Plot start
                    for (int i = 0; i < stops.size() - 1; i++) {

                        int SDK_INT = android.os.Build.VERSION.SDK_INT;
                        if (SDK_INT > 8) {
                            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                                    .permitAll().build();
                            StrictMode.setThreadPolicy(policy);

                        }

                        String origin = stops.get(0).latitude + "," + stops.get(0).longitude;
                        String destination = stops.get(stops.size()-1).latitude + "," + stops.get(stops.size()-1).longitude;
                        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + origin + "&destination=" + destination + "&key=AIzaSyBC5mZ8i142fKpuGMGTHvs95MkpZ3kBPic\n";

                        URL Url = null;
                        HttpURLConnection urlConnection = null;


                        try {
                            Url = new URL(url);
                        } catch (MalformedURLException e) {
                            e.printStackTrace();
                        }
                        try {
                            urlConnection = (HttpURLConnection) Url.openConnection();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }


                        try {
                            is = new BufferedInputStream(urlConnection.getInputStream());
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            StringBuilder total = new StringBuilder(is.available());
                            String line;
                            while ((line = reader.readLine()) != null) {
                                total.append(line).append('\n');
                            }
                            output = total.toString();
                        } catch (IOException e) {
                            Log.e("JSON Parser", "IO error " + e.toString());

                        } finally {
                            urlConnection.disconnect();
                        }

                        try {
                            json = new JSONObject(output);
                        } catch (JSONException e) {
                            Log.e("JSON Parser", "Error parsing data " + e.toString());
                        }


                        List<Route> routes = new ArrayList<>();
                        JSONObject jsonData = json;
                        JSONArray jsonRoutes = null;


                        try {
                            jsonRoutes = jsonData.getJSONArray("routes");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        for (int j = 0; j < jsonRoutes.length(); j++) {

                            JSONObject jsonRoute = null;
                            try {
                                jsonRoute = jsonRoutes.getJSONObject(j);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            Route route = new Route();


                            JSONObject overview_polylineJson = null;
                            try {
                                overview_polylineJson = jsonRoute.getJSONObject("overview_polyline");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            try {
                                route.points = decodePolyLine(overview_polylineJson.getString("points"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            routes.add(route);
                        }

                        plot(routes);
                        getLocation();


                    }//Till plot
                //}

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });

    }

    //Formula for decoding encrypted PolyLine string and return list of LatLng for endpoint of each sub_path.
    private List<LatLng> decodePolyLine(final String poly) {
        int len = poly.length();
        int index = 0;
        List<LatLng> decoded = new ArrayList<LatLng>();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = poly.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            decoded.add(new LatLng((((double) lat / 1e5)),
                    (((double) lng / 1e5))));
        }

        return decoded;
    }



    //Plot on map
    public void plot(List<Route> routes) {


        for (Route route : routes) {

            PolylineOptions polylineOptions = new PolylineOptions().
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++) {
                polylineOptions.add(route.points.get(i));
            }

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    //get current location of bus
    protected void getLocation() {


        button = (Button) findViewById(R.id.b);
        textView = (TextView) findViewById(R.id.t);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());
                textView.setText(" " + latitude + " " + longitude);

                // Initializing after every 5 sec, make global after solving value of bus_id
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference firebase_current_location = database.getReference("CurrentLocation").child(bus_id);
                DatabaseReference firebase_last_visited = database.getReference("lastVisited").child(bus_id);

                firebase_current_location.setValue(latitude + " " + longitude);
                //Log.d("out",stops_name.get(stop_counter));

                if(stop_counter<stops_name.size()-1) {

                    if (getDistance(stops.get(stop_counter + 1).latitude, stops.get(stop_counter + 1).longitude, parseFloat(latitude), parseFloat(longitude)) < 20000) {
                        firebase_last_visited.setValue(stops_name.get(stop_counter));
                        //Log.d("update",stops_name.get(stop_counter));
                        stop_counter++;
                    }

                }
                //Log.d(TAG,stops_name.toString());

            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        ACCESS_FINE_LOCATION,
                        permission.ACCESS_COARSE_LOCATION,
                        permission.INTERNET
                }, 10);
                return;
            }
        } else {
            configureButton();
        }
    }

    // Calculate radian of any angle
    double  rad (double x) {
        return x * Math.PI / 180;
    };

    //Get distance between two point in meter
    double  getDistance(double p1, double p2, double p3, double p4){
        int R = 6378137; // Earth’s mean radius in meter
        double dLat = rad(p3 - p1);
        double dLong = rad(p4 - p2);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(rad(p1)) * Math.cos(rad(p3)) *
                        Math.sin(dLong / 2) * Math.sin(dLong / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;
        return d; // returns the distance in meter
    };

    //Part of get current location of bus
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    configureButton();
                return;
        }
    }

    //Part of get current location of bus, ****
    private void configureButton() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates("network", 3000, 0, locationListener);
            }
        });
    }




}


